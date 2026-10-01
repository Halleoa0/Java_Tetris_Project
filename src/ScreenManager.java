import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/* cardLayout을 이용해 메뉴, 게임, 설정, 상점 등의 화면을 전환하고 현재 실행 중인 GamePanel의 생명주기를 관리함 */
final class ScreenManager extends JPanel {
    private static final String HOME = "home";
    private static final String SETTINGS = "settings";
    private static final String SHOP = "shop";

    private final CardLayout cards = new CardLayout();
    private final HomePanel homePanel;
    private GamePanel gamePanel;
    private SettingsPanel settingsPanel;
    private ShopPanel shopPanel;

    ScreenManager() {
        setLayout(cards);
        setPreferredSize(new Dimension(1280, 720));
        homePanel = new HomePanel(this);
        add(homePanel, HOME);
        // 설정 화면은 최신 설정을 반영하기 위해 동적으로 생성한다.
        shopPanel = new ShopPanel(this);
        add(shopPanel, SHOP);
        showHome();
    }

    void showHome() {
        removeActiveGame();
        cards.show(this, HOME);
        // 메뉴 BGM을 재생한다.
        AudioManager.get().playMenuBgm();
    }

    void startGame() {
        removeActiveGame();
        GamePanel panel = new GamePanel();
        panel.setHomeCallback(this::showHome);
        gamePanel = panel;
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
        panel.setHomeCallback(this::showHome);
        TutorialController tutorial = new TutorialController(game, panel);
        tutorial.setCompletionCallback(this::showHome);
        tutorial.start();
        gamePanel = panel;
        add(panel, "tutorial");
        cards.show(this, "tutorial");
        // 튜토리얼 BGM을 재생한다.
        AudioManager.get().playTutorialBgm();
        revalidate();
        repaint();
        panel.requestFocusInWindow();
    }



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
        if (gamePanel != null) {
            gamePanel.stop();
            remove(gamePanel);
            gamePanel = null;
            revalidate();
            repaint();
        }
    }
}
