import javax.swing.SwingUtilities;

/** 프로그램 시작점. Swing 화면 생성은 이벤트 처리 스레드에서 실행한다. */
public class Main {
    private static final boolean TUTORIAL_MODE = false;
    // 튜토리얼 실행 시에는 ture로 바꾸기

    public static void main(String[] args) {
        // IntelliJ에서 Main.main()을 실행하면 게임 창이 열린다.
        SwingUtilities.invokeLater(() -> {
            if (!TUTORIAL_MODE) {
                new GamePanel().showWindow();
                return;
            }

            Game game = new Game();
            GamePanel panel = new GamePanel(game);
            TutorialController tutorial = new TutorialController(game, panel);
            tutorial.start();
            panel.showWindow();
        });
    }
}
