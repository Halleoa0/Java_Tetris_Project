package app;

import audio.AudioManager;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;


/** 프로그램 시작점. Swing 화면 생성은 이벤트 처리 스레드에서 실행한다. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Jetris");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new ScreenManager());
            frame.pack();
            frame.setLocationRelativeTo(null);

            // 창 포커스 상태를 오디오 매니저에 전달한다.
            frame.addWindowFocusListener(new java.awt.event.WindowAdapter() {
                @Override public void windowGainedFocus(java.awt.event.WindowEvent e) {
                    AudioManager.get().setWindowActive(true);
                }
                @Override public void windowLostFocus(java.awt.event.WindowEvent e) {
                    AudioManager.get().setWindowActive(false);
                }
            });

            frame.setVisible(true);
        });
    }
}
