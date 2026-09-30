import javax.swing.SwingUtilities;
import javax.swing.JFrame;

/** 프로그램 시작점. Swing 화면 생성은 이벤트 처리 스레드에서 실행한다. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new ScreenManager());
            frame.pack();
            frame.setLocationRelativeTo(null);

            // 설정의 '창 비활성화 시 소리 끄기' 옵션 반영을 위해 창 포커스 상태 전달
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