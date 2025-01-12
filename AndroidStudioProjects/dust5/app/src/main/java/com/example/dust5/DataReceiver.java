package com.example.dust5;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import java.io.IOException;
import android.content.Context;
public class DataReceiver {

    private static final String TAG = "DataReceiver";
    private SocketClient socketClient;
    private TextView dustLevelTextView;
    private Handler handler;

    // DataReceiver 생성자 - TextView와 Context를 받는 경우
    public DataReceiver(TextView textView, Context context) {
        this.dustLevelTextView = textView;
        this.socketClient = new SocketClient();
        this.handler = new Handler(Looper.getMainLooper());
    }

    // DataReceiver 생성자 - Context만 받는 경우
    public DataReceiver(Context context) {
        this(null, context);
    }

    // 데이터 수신 시작
    public void startReceivingData() {
        socketClient.connect();
        new Thread(() -> {
            while (true) {
                if (socketClient.isConnected()) {
                    Log.d(TAG, "서버에 연결되었습니다. 데이터를 수신합니다.");
                    try {
                        byte[] buffer = new byte[1024];
                        int bytes = socketClient.getInputStream().read(buffer);
                        if (bytes > 0) {
                            String data = new String(buffer, 0, bytes);
                            Log.d(TAG, "Received data: " + data);
                            handler.post(() -> {
                                Log.d(TAG, "UI 업데이트: " + data);
                                if (dustLevelTextView != null) {
                                    dustLevelTextView.setText(data);
                                }
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading data", e);
                        handler.post(() -> {
                            if (dustLevelTextView != null) {
                                dustLevelTextView.setText("데이터를 받아오지 못했습니다");
                            }
                        });
                        break;
                    }
                } else {
                    Log.d(TAG, "Not connected to server. Retrying in 5 seconds...");
                    handler.post(() -> {
                        if (dustLevelTextView != null) {
                            dustLevelTextView.setText("서버에 연결되지 않았습니다");
                        }
                    });
                    try {
                        Thread.sleep(5000); // 5초 후에 다시 시도
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread interrupted", e);
                    }
                }
            }
        }).start();
    }
}
