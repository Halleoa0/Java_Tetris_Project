package app;

import audio.AudioManager;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;


/// 프로그램의 시작점. 실행 시 Swing 화면을 준비하여 게임 창을 띄움
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Jetris"); // 윈도우 창 이름 Jetris
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 사용자가 닫기 버튼 누르면 꺼지도록
            frame.setResizable(false); // 창 크기 조절 할 수 없게
            frame.setContentPane(new ScreenManager()); // 화면 관리는 ScreenManager가
            frame.pack();
            frame.setLocationRelativeTo(null); // 화면 중앙에 배치. = null이 중앙

            // 창 포커스 상태를 오디오 매니저에 전달한다.
            frame.addWindowFocusListener(new java.awt.event.WindowAdapter() {
                // 이 게임이 포커스 중이라면 AudioManager가 소리를 재생.
                @Override public void windowGainedFocus(java.awt.event.WindowEvent e) {
                    AudioManager.get().setWindowActive(true);
                }
                // 포커스를 잃었따면 false로 소리 끄기.
                @Override public void windowLostFocus(java.awt.event.WindowEvent e) {
                    AudioManager.get().setWindowActive(false);
                }
            });

            frame.setVisible(true);
        });
    }
}

/// Main.java 연관 클래스
// ScreenManager - 화면 관리를 하도록 넘겨줌
// AudioManager - 윈도우 포커스에 따라 소리 on / off