package com.acousticrypt.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    EditText editTextName;
    Button encodeButton;
    Button decodeButton;
    TextView textName;
    private final static int MICROPHONE_PERMISSION_CODE = 200;
    MediaPlayer mediaPlayer;
    wavClass wavObj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextName = (EditText) findViewById(R.id.editTextName);
        encodeButton = (Button) findViewById(R.id.buttonClick);
        textName = (TextView) findViewById(R.id.textName);
        decodeButton = (Button) findViewById(R.id.decode);

        encodeButton.setOnClickListener(view -> {
            sendEncodeRequest();
        });

        if (isMicrophonePresent()) {
            getMicrophonePermission();
        }

        decodeButton.setOnClickListener(view -> {
            sendDecodeRequest();
        });

    }

    public void sendEncodeRequest() {
        String enteredText = editTextName.getText().toString();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
        RequestBody formBody = new FormBody.Builder().add("text", enteredText).build();
        Request request = new Request.Builder().url("http://128.61.80.83:5000/encode").post(formBody).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "network not found", Toast.LENGTH_LONG).show();
                        System.out.println("ERROR ENCODING: " + e);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
//                            System.out.println("RESPONSE ENCODING: " + Objects.requireNonNull(response.body()).string());
                            textName.setText(response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void sendDecodeRequest() {
        File wavFile = new File(getApplicationContext().getFilesDir(), "final_record.wav");
        OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", wavFile.getName(),
                        RequestBody.create(wavFile, MediaType.parse("audio/wav"))
                )
                .build();
        Request request = new Request.Builder().url("http://128.61.80.83:5000/decode").post(formBody).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "network not found", Toast.LENGTH_LONG).show();
                        System.out.println("ERROR DECODING: " + e);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                TextView decodedTextView = findViewById(R.id.textView2);
//                System.out.println("DECODED RESPONSE: " + response.body().string());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String text = "Decoded text: " + response.body().string();
                            decodedTextView.setText(text);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void buttonRecordPressed(View v) {
        try {
//            mediaRecorder = new MediaRecorder();
//            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            mediaRecorder.setOutputFile(getRecordingFilePath());
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mediaRecorder.prepare();
//            mediaRecorder.start();
            wavObj = new wavClass(getApplicationContext().getFilesDir().toString());
            wavObj.startRecording();
            Toast.makeText(this, "Recording has started", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buttonStopPressed(View v) {
//        mediaRecorder.stop();
//        mediaRecorder.release();
//        mediaRecorder = null;
        wavObj.stopRecording();
        Toast.makeText(this, "Recording has stopped", Toast.LENGTH_LONG).show();
    }

    public void buttonPlayPressed(View v) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext().getFilesDir().toString() + "/final_record.wav");
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Recording is playing", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isMicrophonePresent() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void getMicrophonePermission() {
        // If recording audio permission is denied, request permission to record audio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.RECORD_AUDIO
            }, MICROPHONE_PERMISSION_CODE);
        }
    }

    private String getRecordingFilePath() {
        File file = new File(getApplicationContext().getFilesDir(), "recordingTest2.mp3");
        return file.getPath();
    }
}