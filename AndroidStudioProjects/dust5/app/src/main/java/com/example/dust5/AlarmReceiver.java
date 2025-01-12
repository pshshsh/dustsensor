package com.example.dust5;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.io.InputStream;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 로그로 알람 동작 확인
        Log.d(TAG, "onReceive: Alarm received!");

        // 진동으로 알람 표시
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(1000); // 1초 동안 진동
        }

        // 토스트 메시지 표시
        Toast.makeText(context, "알람이 울렸습니다", Toast.LENGTH_LONG).show();

        // 알림 메시지 가져오기
        String message = intent.getStringExtra("ALARM_MESSAGE");

        // 데이터 수신 및 알림 표시
        receiveDataAndShowNotification(context, message);
    }

    private void receiveDataAndShowNotification(Context context, String message) {
        new Thread(() -> {
            Log.d(TAG, "Attempting to connect to server...");
            SocketClient socketClient = new SocketClient();
            socketClient.connect();

            try {
                // 최대 5번 재시도
                int retries = 5;
                while (retries > 0 && !socketClient.isConnected()) {
                    Log.d(TAG, "Server not connected. Retrying...");
                    Thread.sleep(1000); // 1초 간격으로 재시도
                    socketClient.connect();
                    retries--;
                }

                if (socketClient.isConnected()) {
                    Log.d(TAG, "Connected to server.");
                    InputStream inputStream = socketClient.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String data = new String(buffer, 0, bytes);
                        Log.d(TAG, "Received data: " + data);

                        // 미세먼지 농도를 해석하여 알림 메시지 생성
                        String alertMessage = getAlertMessage(data);
                        showNotification(context, message + "\n" + alertMessage);
                    } else {
                        Log.d(TAG, "No data received from server.");
                        showNotification(context, message + "\n서버로부터 데이터를 받아오지 못했습니다");
                    }
                } else {
                    Log.d(TAG, "Failed to connect to server after retries.");
                    showNotification(context, message + "\n서버에 연결되지 않았습니다");
                }
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error receiving data", e);
                showNotification(context, message + "\n데이터를 받아오지 못했습니다");
            }
        }).start();
    }

    private String getAlertMessage(String data) {
        String[] parts = data.split(": ");
        if (parts.length == 2) {
            try {
                double dustLevel = Double.parseDouble(parts[1]);
                if (dustLevel >= 0 && dustLevel <= 40) {
                    return "Dust Level: " + dustLevel + " (좋음)";
                } else if (dustLevel > 40 && dustLevel <= 80) {
                    return "Dust Level: " + dustLevel + " (보통)";
                } else {
                    return "Dust Level: " + dustLevel + " (나쁨)";
                }
            } catch (NumberFormatException e) {
                return "Invalid dust level data";
            }
        }
        return "Invalid data format";
    }

    private void showNotification(Context context, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alarm Channel", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // 알림 사운드 설정
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("미세먼지 알람")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(alarmSound)
                .setAutoCancel(true);

        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        notificationManager.notify(1, builder.build());
    }
}
