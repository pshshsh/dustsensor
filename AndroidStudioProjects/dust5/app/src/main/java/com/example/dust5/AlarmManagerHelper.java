package com.example.dust5;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AlarmManagerHelper {

    private static final String TAG = "AlarmManagerHelper";
    private static final String PREFS_NAME = "AlarmPrefs";
    private static final String PREF_ALARM_TIMES = "AlarmTimes";

    private final List<String> alarms = new ArrayList<>();
    private final Context context;
    private final TextView alarmsTextView;

    public AlarmManagerHelper(Context context, TextView alarmsTextView) {
        this.context = context;
        this.alarmsTextView = alarmsTextView;
        loadAlarms();
        removePastAlarms(); // 시간이 지난 알람 제거
    }

    public void addAlarm(Button setAlarmButton) {
        setAlarmButton.setOnClickListener(v -> {
            Calendar currentTime = Calendar.getInstance();
            int year = currentTime.get(Calendar.YEAR);
            int month = currentTime.get(Calendar.MONTH);
            int day = currentTime.get(Calendar.DAY_OF_MONTH);
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);

            DatePickerDialog datePickerDialog = new DatePickerDialog(setAlarmButton.getContext(), (view, year1, month1, dayOfMonth) -> {
                TimePickerDialog timePickerDialog = new TimePickerDialog(setAlarmButton.getContext(), (view1, hourOfDay, minute1) -> {
                    String alarmTime = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", year1, month1 + 1, dayOfMonth, hourOfDay, minute1);
                    alarms.add(alarmTime);
                    alarmsTextView.setText(String.join("\n", alarms));

                    // 알람 설정
                    Calendar alarmCalendar = Calendar.getInstance();
                    alarmCalendar.set(year1, month1, dayOfMonth, hourOfDay, minute1, 0);

                    try {
                        setAlarm(alarmCalendar.getTimeInMillis(), alarmTime);
                        saveAlarms();

                        // 알람 시간을 서버에 전송
                        sendAlarmTimeToServer(alarmTime);

                    } catch (Exception e) {
                        Log.e(TAG, "Error setting alarm", e);
                        Toast.makeText(context, "알람 설정 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }

                }, hour, minute, true);
                timePickerDialog.show();
            }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void setAlarm(long timeInMillis, String message) throws Exception {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    throw new SecurityException("정확한 알람을 설정할 수 없습니다. 권한을 확인해주세요.");
                }
            }
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("ALARM_MESSAGE", message);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) timeInMillis, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                Log.d(TAG, "setAlarm: Alarm set for " + timeInMillis);
            } catch (Exception e) {
                Log.e(TAG, "setAlarm: Exception while setting alarm", e);
                throw e;
            }
        } else {
            throw new Exception("AlarmManager is null");
        }
    }

    private void sendAlarmTimeToServer(String alarmTime) {
        new Thread(() -> {
            SocketClient socketClient = new SocketClient();
            socketClient.connect();
            while (!socketClient.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                String message = "ALARM_TIME " + alarmTime;
                socketClient.getOutputStream().write(message.getBytes());
                socketClient.getOutputStream().flush();
                Log.d(TAG, "sendAlarmTimeToServer: Alarm time sent to server: " + message);
            } catch (IOException e) {
                Log.e(TAG, "sendAlarmTimeToServer: Error sending alarm time", e);
            }
        }).start();
    }

    private void saveAlarms() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_ALARM_TIMES, String.join(",", alarms));
            editor.apply();
            Log.d(TAG, "saveAlarms: Alarms saved");
        } catch (Exception e) {
            Log.e(TAG, "saveAlarms: Error saving alarms", e);
        }
    }

    private void loadAlarms() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String alarmTimes = prefs.getString(PREF_ALARM_TIMES, "");
            if (!alarmTimes.isEmpty()) {
                String[] alarmArray = alarmTimes.split(",");
                for (String alarm : alarmArray) {
                    alarms.add(alarm);
                    Log.d(TAG, "loadAlarms: Alarm loaded - " + alarm);
                }
                alarmsTextView.setText(String.join("\n", alarms));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadAlarms: Error loading alarms", e);
        }
    }

    private void removePastAlarms() {
        Calendar now = Calendar.getInstance();
        Iterator<String> iterator = alarms.iterator();
        while (iterator.hasNext()) {
            String alarm = iterator.next();
            String[] parts = alarm.split("[- :]");
            Calendar alarmTime = Calendar.getInstance();
            alarmTime.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            alarmTime.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            alarmTime.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            alarmTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[3]));
            alarmTime.set(Calendar.MINUTE, Integer.parseInt(parts[4]));
            alarmTime.set(Calendar.SECOND, 0);
            if (alarmTime.before(now)) {
                iterator.remove();
                Log.d(TAG, "removePastAlarms: Removed past alarm - " + alarm);
            }
        }
        saveAlarms();
        alarmsTextView.setText(String.join("\n", alarms));
    }
}
