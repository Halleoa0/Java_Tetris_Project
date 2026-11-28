package ui;

import audio.AudioManager;
import game.Board;
import game.Game;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.font.TextAttribute;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.Timer;
import settings.Settings;



/* Swing 화면, 키 입력, 주기적인 게임 업데이트를 연결한다.
 * 사용 예: IntelliJ에서 Main을 실행한 뒤 방향키로 이동하고 Space로 즉시 낙하한다.
 * 조작: ←/→ 이동, ↓ 소프트드롭, ↑ 또는 X 시계 회전, Z 반시계 회전,
 * Space 하드드롭, C Hold, R 재시작.
 */
public final class GamePanel extends JPanel {
    /** 특정 게임 동작(예: "hardDrop", "hold")의 입력을 허용할지 결정하는 필터. 튜토리얼 등에서 사용한다. */
    @FunctionalInterface
    public interface InputFilter {
        /** 해당 동작의 입력을 허용하면 true를 반환한다. */
        boolean allow(String action);
    }

    // 조각의 크기는 25 (25x25), 보드의 위치는 x 517, x 111
    private static final int CELL = 25, BOARD_X = 517, BOARD_Y = 111;
    private static final int MATRIX_X = 378, MATRIX_Y = 111, MATRIX_W = 528, MATRIX_H = 505;
    private static final Color BACKGROUND = new Color(41, 41, 41);

    private final Game game;
    private Consumer<Graphics2D> overlayRenderer;
    private long lastTick = System.nanoTime();

    /// 이미지 불러오기
    private final LabelUI labelUI;
    private final MinoTile assets;
    private final BoardRenderer boardRenderer;
    private final GameOverUI gameOverUI;
    private final PauseUI pauseUI;

    /// 폰트 불러오기
    private final Fonts fonts = Fonts.load();
    public final Font interBlack = fonts.interBlack();
    public final Font interMedium = fonts.interMedium();
    public final Font sansKRBlack = fonts.sansKRBlack();
    public final Font orbitBlack = fonts.orbitBlack();
    public final Font orbitBold = fonts.orbitBold();

    private final HeldKeyController heldKeyController;
    private final GameInputController inputController;

    private final Timer timer;
    private boolean paused;
    private Runnable homeCallback = () -> { };
    // 일시정지 메뉴의 재시작 콜백. null이면 game.restart()를 수행한다.
    private Runnable pauseRestartCallback;

    private final shop.ToastOverlay toast = new shop.ToastOverlay(); // 미션 달성 알림
    // 현재 진행 중 모드
    private String gameName = "튜토리얼";

    // 스프린트 결과 (목표 달성 순간에 정해지고, 결과 화면에서 그림)
    // 키를 게임 동작에 연결하고 16ms 간격으로 게임 상태를 갱신한다.
    public GamePanel() { this(new Game()); }

    /** 주어진 게임을 "튜토리얼" 이름으로 표시하는 게임 화면을 만든다. */
    public GamePanel(Game game) {
        this(game, "튜토리얼");
    }

    /**
     * 게임 화면을 구성한다. 이미지/폰트/렌더러를 준비하고, 모든 게임 키를 등록하고,
     * 효과음·기록·미션 리스너를 연결한 뒤 16ms 간격의 게임 루프 타이머를 시작한다.
     */
    public GamePanel(Game game, String gameName) {
        this.game = game;
        this.gameName = gameName;
        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setFocusable(true);

        assets = new MinoTile();
        labelUI = new LabelUI(game, fonts, assets.gameNamePanelImage(), gameName);
        boardRenderer = new BoardRenderer(game, assets.minoTiles(), this);
        gameOverUI = new GameOverUI(game, gameName, fonts, this,
                () -> game.gameOver && !paused, this::restartFromGameOver, () -> homeCallback.run());
        pauseUI = new PauseUI(this, assets.pauseGuideImage(), fonts);
        heldKeyController = new HeldKeyController(game, this);
        inputController = new GameInputController(this, game, heldKeyController,
                () -> paused, () -> game.gameOver, this::handlePauseInput, gameOverUI::confirmSelection);
        inputController.bindHeld(Settings.Action.MOVE_LEFT, () -> game.move(-1, 0));
        inputController.bindHeld(Settings.Action.MOVE_RIGHT, () -> game.move(1, 0));
        inputController.bindHeld(Settings.Action.SOFT_DROP, () -> {
            game.setSoftDropHeld(true);
            game.softDrop();
        });
        inputController.bindInitial(Settings.Action.ROTATE_CW, "rotateCW", () -> game.rotate(1));
        inputController.bindInitial(Settings.Action.ROTATE_CCW, "rotateCCW", () -> game.rotate(-1));
        inputController.bind(Settings.Action.HARD_DROP, "hardDrop", game::hardDrop);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(Settings.Action.HARD_DROP), "pauseSpaceReleased");
        getActionMap().put("pauseSpaceReleased", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { inputController.spaceReleased(); }
        });
        inputController.bindInitial(Settings.Action.HOLD, "hold", game::hold);
        inputController.bind(Settings.Action.RESTART, "restart", () -> {
            game.restart();
            AudioManager.get().startGameBgm();
        });
        inputController.bindPauseNavigation("UP", -1, () -> pauseUI.moveSelection(-1));
        inputController.bindPauseNavigation("DOWN", 1, () -> pauseUI.moveSelection(1));
        inputController.bindGameOverNavigation("LEFT", -1, () -> gameOverUI.selectOption(-1));
        inputController.bindGameOverNavigation("RIGHT", 1, () -> gameOverUI.selectOption(1));

        inputController.bindUnfiltered("ESCAPE", "pauseToggle", () -> {
            if (paused) setPaused(false);
            else if (game.gameOver) homeCallback.run();
            else setPaused(true);
        });
        inputController.bindUnfiltered("M", "menu", () -> { if (paused || game.gameOver) homeCallback.run(); });

        // 위클리 미션 진행도 기록(튜토리얼 제외) + 달성 알림
        if (!"튜토리얼".equals(gameName)) game.addListener(new shop.MissionTracker(game));
        toast.attach();

        game.addListener(new GameAudioListener());
        game.addListener(new GameRecordListener(game, gameName));

        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                clearHeldKeys();
                inputController.spaceReleased();
                AudioManager.get().setWindowActive(false);
            }
            @Override public void focusGained(FocusEvent e) {
                AudioManager.get().setWindowActive(true);
            }
        });

        timer = new Timer(16, e -> {
            long now = System.nanoTime();
            int clockElapsed = (int) Math.min(Integer.MAX_VALUE, (now - lastTick) / 1_000_000L);
            int elapsed = Math.min(100, clockElapsed);
            lastTick = now;
            game.tick(clockElapsed);
            heldKeyController.tickHeldKeys(Math.min(1_000, clockElapsed));
            game.scoring.tick(elapsed);
            gameOverUI.updateVisibility();
            repaint();
        });
        timer.start();
    }

    /** 허용할 입력을 고르는 필터를 지정한다. null이면 모든 입력을 허용한다. */
    public void setInputFilter(InputFilter inputFilter) {
        inputController.setInputFilter(inputFilter);
    }

    /** 게임 화면 위에 추가로 그릴 내용(예: 튜토리얼 안내)을 지정한다. */
    public void setOverlayRenderer(Consumer<Graphics2D> overlayRenderer) { this.overlayRenderer = overlayRenderer; }

    /** 일시정지 메뉴에서 RESTART를 골랐을 때 실행할 동작을 지정한다. null이면 기본 game.restart(). */
    public void setPauseRestartCallback(Runnable callback) { this.pauseRestartCallback = callback; }

    /** 메인 화면으로 돌아갈 때 실행할 동작을 지정한다. null이면 아무 동작도 하지 않는다. */
    public void setHomeCallback(Runnable homeCallback) {
        this.homeCallback = homeCallback == null ? () -> { } : homeCallback;
    }

    /** 일시정지 상태를 바꾼다. 일시정지 시 타이머와 BGM을 멈추고, 해제 시 다시 재개한다. */
    private void setPaused(boolean paused) {
        if (this.paused == paused) return;
        this.paused = paused;
        clearHeldKeys();
        if (paused) {
            pauseUI.resetSelection();
            timer.stop();
            AudioManager.get().pauseBgm(); // BGM을 일시정지한다.
        }
        else {
            lastTick = System.nanoTime();
            timer.restart();
            AudioManager.get().resumeBgm(); // BGM을 재개한다.
        }
        gameOverUI.updateVisibility();
        repaint();
    }

    /** 게임 오버 화면의 "다시 시작" 버튼 동작. 게임과 BGM을 다시 시작하고 키보드 포커스를 되찾는다. */
    private void restartFromGameOver() {
        game.restart();
        AudioManager.get().startGameBgm();
        gameOverUI.updateVisibility();
        repaint();
        requestFocusInWindow();
    }

    /** 화면에서 제거될 때 주기적인 게임 업데이트를 멈춘다. */
    public void stop() {
        toast.detach();
        timer.stop();
        clearHeldKeys();
    }

    /** 누르고 있던 키와 IRS/IHS 입력 상태를 모두 초기화한다. */
    private void clearHeldKeys() {
        heldKeyController.clear();
        inputController.clear();
    }

    /// 실제로 그리기
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        shop.ThemeManager.paintBackground((Graphics2D) graphics, getWidth(), getHeight()); // 상점에서 장착한 배경화면
        graphics.drawImage(assets.matrixImage(), MATRIX_X, MATRIX_Y, MATRIX_W, MATRIX_H, this);
        graphics.drawImage(assets.guideImage(), 40, 545, this);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boardRenderer.draw(g);
        if (game.isCountingDown()) drawCountdown(g);
        labelUI.drawGameNamePanel(g);
        labelUI.drawPpsLabel(g);
        labelUI.drawLineLabel(g);
        labelUI.drawScoreValueLabel(g);
        labelUI.drawTimeLabel(g);
        labelUI.drawScoreLabel(g);
        if (game.gameOver) gameOverUI.draw(g);
        toast.draw(g, getWidth(), sansKRBlack);
        if (overlayRenderer != null) overlayRenderer.accept(g);
        if (paused) pauseUI.draw(g);
        g.dispose();
    }

    /// 이 아래의 코드는 디자인 변화 시 수정될 수 있는 코드입니다.
    /// 보드 및 미노(블록)을 png 파일로 대체하여 아래보다 코드가 간단해질 수 있습니다.

    /** 스프린트 시작 전 3, 2, 1 카운트다운. 보드를 살짝 어둡게하고 가운데에 숫자를 보여준다. */
    private void drawCountdown(Graphics2D g) {
        int boardW = Board.WIDTH * CELL, boardH = (Board.HEIGHT - Board.HIDDEN_ROWS) * CELL;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(BOARD_X, BOARD_Y, boardW, boardH);
        int centerX = BOARD_X + boardW / 2, centerY = BOARD_Y + boardH / 2;
        g.setColor(new Color(255, 255, 255, 190));
        g.setFont(orbitBold.deriveFont(Map.of(TextAttribute.SIZE, 20f, TextAttribute.TRACKING, 0.3f)));
        String ready = "READY";
        g.drawString(ready, centerX - g.getFontMetrics().stringWidth(ready) / 2, centerY - 60);
        g.setColor(Color.WHITE);
        g.setFont(orbitBlack.deriveFont(96f));
        String number = String.valueOf(game.countdownSeconds());
        g.drawString(number, centerX - g.getFontMetrics().stringWidth(number) / 2, centerY + 30);
    }

    /// 일시정지 메뉴에서 선택한 항목을 실행한다.
    private void confirmPauseSelection() {
        AudioManager.get().playMenuSelect(); // 옵션 선택 효과음
        switch (pauseUI.selection()) {
            case 0 -> setPaused(false); // 계속하기
            case 1 -> { // 다시 시작
                if (pauseRestartCallback != null) pauseRestartCallback.run();
                else {
                    game.restart();
                    AudioManager.get().startGameBgm();
                }
                setPaused(false);
            }
            case 2 -> homeCallback.run(); // 메인 화면으로 이동
        }
    }

    /** 일시정지 중 들어온 게임 키 입력을 처리한다. 하드 드롭 키는 메뉴 선택 확정으로 사용한다. */
    private void handlePauseInput(Settings.Action a) {
        if (a == Settings.Action.HARD_DROP) {
            confirmPauseSelection();
        }
    }

}
