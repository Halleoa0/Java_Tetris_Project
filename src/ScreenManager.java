import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/** 메뉴와 게임 화면 사이의 전환을 관리한다. */
final class ScreenManager extends JPanel {
    private static final String MENU = "menu";
    private static final String SETTINGS = "settings";
    private static final String SHOP = "shop";

    private final CardLayout cards = new CardLayout();
    private final HomePanel homePanel;
    private GamePanel activeGamePanel;

    ScreenManager() {
        setLayout(cards);
        setPreferredSize(new Dimension(1280, 720));
        homePanel = new HomePanel(this);
        add(homePanel, MENU);
        // 설정 화면은 최신 설정을 반영하기 위해 동적으로 생성한다.
        add(new ShopPanel(this), SHOP);
        showMenu();
    }

    void showMenu() {
        removeActiveGame();
        cards.show(this, MENU);
        // 메뉴 BGM을 재생한다.
        AudioManager.get().playMenuBgm();
    }

    void startGame() {
        removeActiveGame();
        GamePanel panel = new GamePanel();
        panel.setMenuCallback(this::showMenu);
        activeGamePanel = panel;
        add(panel, "game");
        cards.show(this, "game");
        // 인게임 BGM을 무작위로 재생한다.
        AudioManager.get().startGameBgm();
        revalidate();
        repaint();
        panel.requestFocusInWindow();
    }

    void startTutorial() {
        removeActiveGame();
        Game game = new Game();
        GamePanel panel = new GamePanel(game);
        panel.setMenuCallback(this::showMenu);
        TutorialController tutorial = new TutorialController(game, panel);
        tutorial.setCompletionCallback(this::showMenu);
        tutorial.start();
        activeGamePanel = panel;
        add(panel, "tutorial");
        cards.show(this, "tutorial");
        // 튜토리얼 BGM을 재생한다.
        AudioManager.get().playTutorialBgm();
        revalidate();
        repaint();
        panel.requestFocusInWindow();
    }

    private SettingsPanel settingsPanel;

    void showSettings() {
        removeActiveGame();
        if (settingsPanel != null) remove(settingsPanel);
        settingsPanel = new SettingsPanel(this);
        add(settingsPanel, SETTINGS);
        cards.show(this, SETTINGS);
        // 메뉴 BGM을 유지한다.
        AudioManager.get().playMenuBgm();
        revalidate();
        repaint();
    }



    void showShop() {
        removeActiveGame();
        cards.show(this, SHOP);
        // 메뉴 BGM을 유지한다.
        AudioManager.get().playMenuBgm();
    }

    private void removeActiveGame() {
        if (activeGamePanel != null) {
            activeGamePanel.stop();
            remove(activeGamePanel);
            activeGamePanel = null;
            revalidate();
            repaint();
        }
    }
}
