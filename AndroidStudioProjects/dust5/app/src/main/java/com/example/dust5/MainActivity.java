package com.example.dust5;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView dustLevelTextView;
    private TextView alarmsTextView;
    private Button setAlarmButton;
    private DataReceiver dataReceiver;
    private AlarmManagerHelper alarmManagerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 뷰 초기화
        dustLevelTextView = findViewById(R.id.dustLevelTextView);
        alarmsTextView = findViewById(R.id.alarmsTextView);
        setAlarmButton = findViewById(R.id.setAlarmButton);

        // 초기 화면 설정
        dustLevelTextView.setText("서버에 연결되지 않았습니다");

        // 객체 초기화
        dataReceiver = new DataReceiver(dustLevelTextView, this); // Context 추가
        alarmManagerHelper = new AlarmManagerHelper(this, alarmsTextView);

        // 알람 추가 기능 초기화
        alarmManagerHelper.addAlarm(setAlarmButton);

        // 데이터 수신 시작
        dataReceiver.startReceivingData();
    }
}
