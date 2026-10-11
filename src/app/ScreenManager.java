package app;

import audio.AudioManager;
import game.Game;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import tutorial.TutorialController;
import ui.GamePanel;
import ui.HomePanel;
import ui.SettingsPanel;
import ui.ShopPanel;
import ui.ModeSelectPanel;

/// 이곳에서 TETRIS의 화면을 미리 담아두고 메뉴 선택에 따라 그 화면을 바꾸는 관리자 역할
public final class ScreenManager extends JPanel {

    // 메인에는 4가지 화면 HOME, SETTINGS, SHOP, MODE_SELECT 존재.
    private static final String HOME = "home";
    private static final String SETTINGS = "settings";
    private static final String SHOP = "shop";
    private static final String MODE_SELECT = "modeSelect";

    // Home, Game, Settings 등 패널들을 Card에 넣어서 관리. 여러 패널들 중 하나를 골라 보여주는 식
    private final CardLayout cards = new CardLayout();

    private SettingsPanel settingsPanel; // 설정 화면 -> SETTINGS
    private GamePanel gamePanel; // 실제 게임을 하기 위한 화면
    private ShopPanel shopPanel; // 상점 화면 - 아직 미구현

    /// 생성자. 1280x720 사이즈, 게임 시작 시 HOME을 보여준다. (settingsPanel은 최신 정보를 동적으로 보여주기 위해서 뺌)
    public ScreenManager() {
        setLayout(cards);
        setPreferredSize(new Dimension(1280, 720));
        add(new HomePanel(this), HOME);
        add(new ModeSelectPanel(this), MODE_SELECT);
        add(new ShopPanel(this), SHOP);
        showHome();
    }


    /// 홈 화면을 보여준다.
    public void showHome() {
        removeActiveGame();
        cards.show(this, HOME);
        AudioManager.get().playMenuBgm();
    }

    /// 모드 설정 화면을 보여준다.
    public void showModeSelect() {
        removeActiveGame();
        cards.show(this, MODE_SELECT);
        AudioManager.get().playMenuBgm();
        refreshDisplay();
    }


    /// 게임을 시작합니다. game은 상태와 규칙을, gameName은 현재 모드를 띄울 텍스트입니다.
    /// 울트라 : new Game(TIME_LIMIT_MS) - 제한시간 규칙
    /// 스프린트 : Game.sprint(20) - 20줄 지우기 규칙
    public void startGame(Game game, String gameName) {
        removeActiveGame();                              // 이전에 진행 중이었던 게임 종료
        GamePanel panel = new GamePanel(game, gameName); // 새 게임을 만듦
        panel.setHomeCallback(this::showHome);           // 게임에서 HOME으로 돌아가기 위해서 연결
        gamePanel = panel;                               // gamePanel에 현재 게임을 저장 (게임 정지, 제거를 위함)
        add(panel, "game");                     // game으로 패널 등록
        cards.show(this, "game");            // Card에서 game을 꺼내서 보여줌

        AudioManager.get().startGameBgm();               // 브금 재생
        refreshDisplay();                                // 화면 새로고침
        panel.requestFocusInWindow();                    // 창 포커스 잡기
    }

    /// 튜토리얼을 시작
    public void startTutorial() {
        removeActiveGame();

        Game game = new Game();
        GamePanel panel = new GamePanel(game);
        panel.setHomeCallback(this::showHome);          // 일시정지 및 메인 화면 가기로 나가기 위한 Callback
        TutorialController tutorial = new TutorialController(game, panel);
        tutorial.setCompletionCallback(this::showHome); // 튜토리얼이 완료 됐을 경우 나가기 위한 Callback
        tutorial.start();

        gamePanel = panel;
        add(panel, "tutorial");
        cards.show(this, "tutorial");

        AudioManager.get().playTutorialBgm();
        refreshDisplay();
        panel.requestFocusInWindow();
    }


    /// 설정창을 띄운다.
    public void showSettings() {
        removeActiveGame();
        if (settingsPanel != null) remove(settingsPanel);
        settingsPanel = new SettingsPanel(this);
        add(settingsPanel, SETTINGS);
        cards.show(this, SETTINGS);
        AudioManager.get().playMenuBgm();
        refreshDisplay();
    }

    /// 상점을 띄운다.
    public void showShop() {
        removeActiveGame();
        cards.show(this, SHOP);
        AudioManager.get().playMenuBgm();
    }

    /// 게임 화면을 새로 만들고 이전 게임을 정리하기 위해서 게임 패널 삭제.
    private void removeActiveGame() {
        if (gamePanel != null) {
            gamePanel.stop();
            remove(gamePanel);
            gamePanel = null;
            refreshDisplay();
        }
    }

    /// 다시 그리기, 다시 계산하기 위해서 호출.
    private void refreshDisplay() {
        revalidate();
        repaint();
    }
}

/// ScreenManager와 연관 클래스
// Main - ScreenManager를 만듦
// HomePanel, ModeSelectPanel, SettingsPanel, ShopPanel - 화면으로 이동
// GameMode - 게임 시작시킨다.