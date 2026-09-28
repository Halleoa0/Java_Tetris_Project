import javax.swing.SwingUtilities;

/** 프로그램 시작점. Swing 화면 생성은 이벤트 처리 스레드에서 실행한다. */
public class Main {
    public static void main(String[] args) {
        // IntelliJ에서 Main.main()을 실행하면 게임 창이 열린다.
        SwingUtilities.invokeLater(() -> new GamePanel().showWindow());
    }
}
