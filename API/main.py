import numpy as np
from flask import Flask
from flask import request
from text_wav_converter import TextWavConverter

app = Flask(__name__)
freq_lo = 17000
freq_hi = 18000
alpha_freq = np.linspace(freq_lo, freq_hi, num=27, endpoint=True).astype(int)
converter = TextWavConverter()


@app.route('/')
def hello_world():
    return 'Hello, World!'


@app.route('/encode', methods=['POST'])
def text_encode():
    text_to_convert = request.form['text']
    converter.generate_audio(text_to_convert, 2, alpha_freq, 10000, "audio_samples/encoded_audio.wav")

    return "Success"


@app.route('/decode', methods=['POST'])
def text_decode():
    wav = request.files['file']
    wav.save("audio_samples/audio_from_phone.wav")
    message_recovered = converter.decode_audio(alpha_freq, 2, "audio_samples/audio_from_phone.wav")
    return message_recovered


if __name__ == '__main__':
    #app.run(host='143.215.51.70', port=5000)
    #app.run(host='4.71.27.132', port=5000)
    app.run(host='143.215.51.224', port=5000)
