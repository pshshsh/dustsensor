package com.example.dust5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClient {

    private String serverIP = "192.168.113.9"; // 서버 IP 주소 연결
    private int serverPort = 12345; // 서버 포트 번호 연결
    private Socket socket; // 클라이언트 소켓 객체이다. 서버와의 연결 관리를 함
    private InputStream inputStream; // 서버로 부터 데이터 읽기 위한 입력 스트림
    private OutputStream outputStream; // 서버로 데이터 보내기 위한 출력 스트림
    private boolean isConnected = false;

    // , connect  매서드를 호출하여 서버와 연결 새로운 스레드를 사용하여 서버와 연결 시도, 연결 성공하면 소켓 객체 초기화, 연결상태 true로 설정
    // 연결 실패시 예외 처리하여 오류 문구 드게함
    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIP, serverPort);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                isConnected = true;
                System.out.println("서버와 연결되었습니다.");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                System.err.println("서버 호스트를 찾을 수 없습니다.");
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("서버와의 연결 중 오류가 발생했습니다.");
            }
        }).start();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
