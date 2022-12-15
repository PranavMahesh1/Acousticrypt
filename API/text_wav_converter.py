import numpy as np
import wave
import struct
import matplotlib.pyplot as plt
from scipy.fft import fft, fftfreq
from scipy.signal import find_peaks
from scipy.fft import fft, fftfreq
from scipy.signal import find_peaks


class TextWavConverter:
    # freq_lo = 15000
    # freq_hi = 18000
    # alpha_freq = np.linspace(freq_lo, freq_hi, num=27, endpoint=True).astype(int)

    def generate_audio(self, message, mult_param, alpha_freq, amplitude, outfile):
        print("Generating audio...")
        # construct audio wave
        samplerate = 44100
        spl = int(samplerate / mult_param)  # samples per letter
        t = np.linspace(0., 1.0 / mult_param, spl)
        data = []
        for l in message:
            lf = alpha_freq[-1] if l == ' ' else alpha_freq[ord(l) - ord('a')]
            print(lf)
            data_message_element = np.int16(amplitude * np.sin(2 * np.pi * lf * t))
            data.append(data_message_element)
        gen_bin_data = np.hstack(tuple(data))

        # write file
        nchannels = 1
        sampwidth = 2  # bytes
        nframes = int(len(message) * spl)
        wav_file = wave.open(outfile, 'w')
        wav_file.setparams((nchannels, sampwidth, int(samplerate), nframes, "NONE", "not compressed"))
        for frame in gen_bin_data:
            wav_file.writeframes(struct.pack('h', frame))
        wav_file.close()
        print("Done")

    def search_start(self, audio_file, freq_lo):
        
        n_test_frames = 1500
        num_discard_frames = 0
        with wave.open(audio_file, 'r') as wav_file:
            nchannels, sampwidth, framerate, nframes, _, _ = wav_file.getparams()
            
            for i in range(0, int(nframes / 2), 10):
                discard_frames = wav_file.readframes(i)
                
                test_frames = wav_file.readframes(n_test_frames)
                byte_storage_format = "h" * (len(test_frames) // sampwidth)
                test_frames = struct.unpack_from(byte_storage_format, test_frames)
                test_frames = np.array(test_frames)
                
                dt = 1.0 / 44100
                freq = fftfreq(n_test_frames, d=dt)
                
                d_fft = fft(test_frames, norm="forward")
                peaks_index, properties = find_peaks(np.abs(d_fft), height=15)
                
                peak_height_threshold = 25
                peaks_index, properties = find_peaks(np.abs(d_fft), height=peak_height_threshold)
                while (len(peaks_index)) == 0 and peak_height_threshold > 0:
                    peak_height_threshold -= 5
                    peaks_index, properties = find_peaks(np.abs(d_fft), height=peak_height_threshold)

                if len(peaks_index) == 0:
                    wav_file.rewind()
                    continue
                    
                #identify peak with maximum height
                peaks_list = list()
                for i in range(len(peaks_index)):
                    if peaks_index[i] >= len(freq):
                        continue
                    peak_freq = abs(freq[peaks_index[i]])
                    peak_height = properties['peak_heights'][i]
                    peaks_list.append((peak_height, peak_freq))
                peaks_list.sort(reverse=True)

                while len(peaks_list) > 0 and peaks_list[0][1] < freq_lo - 1000:
                    peaks_list.pop(0)

                if (len(peaks_list)) == 0:
                    continue

                num_discard_frames = i
                break
                
        return num_discard_frames

    def decode_audio(self, alpha_freq, mult_param, audio_file):
        freq_lo = alpha_freq[0]
        freq_hi = alpha_freq[-1]
        message = ""

        num_discard_frames = self.search_start(audio_file, freq_lo)

        with wave.open(audio_file, 'r') as wav_file:
            nchannels, sampwidth, framerate, nframes, _, _ = wav_file.getparams()
            spl = int(44100 / mult_param)

            discard = wav_file.readframes(num_discard_frames)

            while True:
                #print("New letter being parsed")
                samples = wav_file.readframes(spl)
                if not samples:
                    break
                byte_storage_format = "h" * (len(samples) // sampwidth)
                frames = struct.unpack_from(byte_storage_format, samples)
                
                frames = np.array(frames)
                
                n = spl   
                dt = 1.0 / 44100
                freq = fftfreq(n, d=dt)
                d_fft = fft(frames, norm="forward")
                
                peak_height_threshold = 25
                peaks_index, properties = find_peaks(np.abs(d_fft), height=peak_height_threshold)
                while (len(peaks_index)) == 0 and peak_height_threshold > 0:
                    peak_height_threshold -= 5
                    peaks_index, properties = find_peaks(np.abs(d_fft), height=peak_height_threshold)
                
                if (len(peaks_index)) == 0:
                    continue
                    
                #identify peak with maximum height
                peaks_list = list()
                for i in range(len(peaks_index)):
                    if peaks_index[i] >= len(freq):
                        continue
                    peak_freq = abs(freq[peaks_index[i]])
                    peak_height = properties['peak_heights'][i]
                    peaks_list.append((peak_height, peak_freq))
                peaks_list.sort(reverse=True)
                
                while len(peaks_list) > 0 and peaks_list[0][1] < freq_lo - 2000:
                    peaks_list.pop(0)
                
                if (len(peaks_list)) == 0:
                    continue
                
                #determine closest letter
                letter_freq = peaks_list[0][1]
                closest_letter = None
                closest_diff = float('inf')
                closest_freq = None
                for i in range(len(alpha_freq)):
                    diff = abs(alpha_freq[i] - letter_freq)
                    if diff < closest_diff:
                        closest_freq = alpha_freq[i]
                        closest_diff = diff
                        closest_letter = chr(ord('a') + i) if i < 26 else ' '

                message += closest_letter
        
        return message
