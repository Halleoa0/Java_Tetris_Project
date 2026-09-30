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
            frame.setVisible(true);
        });
    }
}
