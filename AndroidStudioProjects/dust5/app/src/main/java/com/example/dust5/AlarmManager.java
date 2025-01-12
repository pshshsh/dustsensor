package com.example.dust5;

import android.app.TimePickerDialog;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class AlarmManager {

    private List<String> alarms;
    private TextView alarmsTextView;

    // AlarmManager 생성자
    public AlarmManager(TextView alarmsTextView) {
        this.alarms = new ArrayList<>();
        this.alarmsTextView = alarmsTextView;
    }

    // 알람 추가
    public void addAlarm(Button setAlarmButton) {
        setAlarmButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(setAlarmButton.getContext(), (view, hourOfDay, minute) -> {
                String alarmTime = String.format("%02d:%02d", hourOfDay, minute);
                alarms.add(alarmTime);
                alarmsTextView.setText(String.join("\n", alarms));
                // 알람 시간 저장을 확인하기 위해 로그 출력
                System.out.println("알람 시간이 설정되었습니다: " + alarmTime);
            }, 0, 0, true);
            timePickerDialog.show();
        });
    }
}
