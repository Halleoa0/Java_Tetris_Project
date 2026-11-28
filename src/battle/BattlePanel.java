package battle;

import audio.AudioManager;
import game.GameListener;
import graphics.ScreenStyle;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.Timer;
import settings.Settings;
import ui.Fonts;
import ui.GameAudioListener;
import ui.GameInputController;
import ui.MinoTile;
import ui.PauseUI;

/**
 * 1:1 테트리스 배틀(유저 vs CPU 봇) 화면 패널 클래스이다.
 * 게임 루프, 일시정지, 화면 흐름(다음 라운드, 메뉴)을 담당하고 나머지는 아래 클래스에 맡긴다.
 * - 그리기 : BattleRenderer(보드, 가운데 정보), BattleResultOverlay(결과 상자), ui.PauseUI(일시정지)
 * - 입력 : ui.GameInputController(키 바인딩, DAS/ARR, IRS/IHS), SpawnInputBuffer(스폰 대기 중 입력)
 */
public final class BattlePanel extends JPanel {

    private static final int FRAME_MS = 16;          // 화면 갱신 간격(ms)
    private static final int MAX_FRAME_MS = 100;     // 한 번에 처리하는 최대 경과 시간(ms)

    private final BattleGame battle;
    private Runnable homeCallback = () -> { };
    private final Timer gameLoopTimer;
    private long lastTick;

    private final Fonts fonts = Fonts.load();
    private final BattleRenderer renderer;
    private final BattleResultOverlay resultOverlay;
    private final PauseUI pauseUI;
    private final GameInputController inputController;
    private final SpawnInputBuffer spawnBuffer = new SpawnInputBuffer();

    private boolean paused = false;
    private final shop.ToastOverlay toast = new shop.ToastOverlay(); // 미션 달성 알림
    private Point mouse; // 지금 마우스 위치 (마우스를 올린 글씨를 밝게 그리는 데 씀)

    public BattlePanel(BotBrain.Difficulty difficulty) {
        this.battle = new BattleGame(difficulty);
        setLayout(null);
        setPreferredSize(new Dimension(ScreenStyle.WIDTH, ScreenStyle.HEIGHT));
        setBackground(ScreenStyle.BACKGROUND);
        setFocusable(true);

        // 기존 프로젝트 에셋들을 불러온다.
        MinoTile assets = new MinoTile();
        String botName = "CPU (" + difficulty.label + ")"; // CPU는 난이도도 같이 보여줌
        this.renderer = new BattleRenderer(battle, assets, fonts, botName, this);
        this.resultOverlay = new BattleResultOverlay(battle, fonts);
        this.pauseUI = new PauseUI(this, assets.pauseGuideImage(), fonts,
                "RESUME", "RESTART ROUND", "RESTART MATCH", "QUIT"); // 순서는 confirmPauseSelection과 같아야 함
        this.inputController = new GameInputController(this, battle.playerGame, createInputHost());

        bindKeys();
        bindMouse();
        bindFocus();
        addGameListeners();
        toast.attach();

        this.gameLoopTimer = new Timer(FRAME_MS, e -> onFrame());
    }

    public void setHomeCallback(Runnable callback) {
        this.homeCallback = callback != null ? callback : () -> { };
    }

    /// 배틀 매치를 시작한다.
    public void start() {
        battle.startMatch();
        lastTick = System.nanoTime();
        gameLoopTimer.start();
        AudioManager.get().startGameBgm();
        requestFocusInWindow();
    }

    /// 게임을 정지하고 리소스를 정리한다.
    public void stop() {
        toast.detach();
        gameLoopTimer.stop();
        battle.stop();
    }

    /// 한 프레임만큼 게임 상태를 갱신하고 다시 그린다. (GamePanel과 같은 방식)
    private void onFrame() {
        long now = System.nanoTime();
        int elapsed = (int) Math.min(MAX_FRAME_MS, (now - lastTick) / 1_000_000L); // 지난 번 이후 흐른 시간(ms)
        lastTick = now;

        if (!paused) {
            battle.tick(elapsed);
            battle.playerGame.tick(elapsed);
            battle.botGame.tick(elapsed);
            if (isPlaying()) inputController.tickHeldKeys(elapsed); // DAS/ARR 반복 입력
            battle.playerGame.scoring.tick(elapsed); // 점수 글씨(T-SPIN, COMBO)가 사라지는 시간
            battle.botGame.scoring.tick(elapsed);
        }
        // 게임 안 하면 버퍼링, 누르고 있던 키 삭제
        if (!isPlaying()) {
            spawnBuffer.clear();
            if (inputController.hasHeldKeys()) inputController.releaseAllKeys();
        }
        repaint();
    }

    private boolean isPlaying() {
        return battle.getState() == BattleGame.State.PLAYING;
    }

    /// 플레이어의 게임에 미션 기록, 스폰 입력 버퍼, 효과음 리스너를 연결한다.
    private void addGameListeners() {
        battle.playerGame.addListener(new shop.MissionTracker(battle.playerGame));
        battle.playerGame.addListener(new GameListener() {
            @Override public void onRestart() { spawnBuffer.clear(); }
            @Override public void onSpawn() { applyBufferedSpawnInputs(); }
        });
        battle.playerGame.addListener(new GameAudioListener());
    }

    /// 스폰 대기가 끝나(블록 쌓을 수 있을 때) 버퍼에 저장됐던 입력을 가져옴
    private void applyBufferedSpawnInputs() {
        if (isPlaying()) spawnBuffer.applyTo(battle.playerGame, inputController::isInitialKeyHeld);
        else spawnBuffer.clear();
    }

    /// 윈도우 포커스를 잃었을 때 자동으로 게임을 일시정지한다.
    private void bindFocus() {
        addFocusListener(new FocusListener() {
            @Override public void focusGained(FocusEvent e) {
                AudioManager.get().setWindowActive(true);
            }
            @Override public void focusLost(FocusEvent e) {
                AudioManager.get().setWindowActive(false);
                inputController.clear(); // 창을 벗어나면 키를 뗀 것을 못 받으므로 누르고 있던 키를 지움
                if (isPlaying() && !paused) setPaused(true);
            }
        });
    }

    /// 일시정지 상태를 변경하고 CPU 봇 타이머를 동결하거나 재개한다.
    private void setPaused(boolean paused) {
        if (this.paused == paused) return;
        if (paused && !isPlaying()) return;

        this.paused = paused;
        inputController.releaseAllKeys(); // GamePanel처럼 일시정지를 켜고 끌 때 누르고 있던 키를 지움
        if (paused) {
            pauseUI.resetSelection();
            battle.pause();
            AudioManager.get().pauseBgm();
        } else {
            battle.resume();
            AudioManager.get().resumeBgm();
        }
        repaint();
    }

    /// 일시정지 메뉴 선택 항목을 실행한다.
    private void confirmPauseSelection() {
        AudioManager.get().playMenuSelect();
        switch (pauseUI.selection()) {
            case 0 -> setPaused(false); // 계속하기
            case 1 -> { // 라운드 다시 시작
                setPaused(false);
                spawnBuffer.clear(); // 게임 다시 시작하니까 버퍼 입력 초기화
                battle.startRound();
            }
            case 2 -> { // 매치 다시 시작
                setPaused(false);
                spawnBuffer.clear();
                battle.startMatch();
            }
            case 3 -> { // 메인 메뉴로 나가기
                setPaused(false);
                stop();
                homeCallback.run();
            }
        }
    }

    /// 재시작 키(R)로 지금 라운드를 다시 시작한다.
    private void quickRestart() {
        AudioManager.get().playMenuSelect();
        spawnBuffer.clear();
        battle.startRound();
    }

    /// 결과 화면에서 다음으로 넘어간다. 라운드가 끝났으면 다음 라운드, 매치가 끝났으면 다시 대결.
    private void confirmResult() {
        if (battle.getState() == BattleGame.State.ROUND_OVER) battle.nextRound();
        else if (battle.getState() == BattleGame.State.MATCH_OVER) battle.startMatch();
        else return; // 게임 중이면 아무것도 안 함
        AudioManager.get().playMenuSelect();
    }

    /// 대전을 멈추고 메인 메뉴로 나간다.
    private void goHome() {
        stop();
        AudioManager.get().playMenuSelect();
        homeCallback.run();
    }

    /// 입력 컨트롤러에 대전 화면의 상태와 규칙을 알려준다.
    /// (라운드 진행 중일 때만 입력을 받고, 스폰 대기 중 입력은 버퍼에 저장하는 것이 1인용과 다름)
    private GameInputController.Host createInputHost() {
        return new GameInputController.Host() {
            @Override public boolean isPaused() { return paused; }
            @Override public boolean isFinished() { return !isPlaying(); }
            @Override public boolean acceptsGameplay() { return isPlaying(); }

            @Override public void onPauseInput(Settings.Action action) {
                if (action == Settings.Action.HARD_DROP) confirmPauseSelection(); // 일시정지 중 스페이스 = 메뉴 선택
            }

            @Override public void onConfirmResult() { confirmResult(); }

            @Override public void dispatch(Settings.Action action, Runnable run) {
                if (battle.playerGame.isSpawnDelayed()) spawnBuffer.store(action);
                else run.run();
            }

            @Override public void beforeHeldPress(Settings.Action action, boolean alreadyHeld) {
                if (battle.playerGame.isSpawnDelayed() && !alreadyHeld) spawnBuffer.store(action);
            }
        };
    }

    /// 키 입력을 액션에 연결한다.
    private void bindKeys() {
        inputController.bindGameplayKeys(this::quickRestart);

        // 일시정지 화면에서 위/아래 키로 메뉴 이동
        inputController.bindPauseNavigation("UP", () -> movePauseSelection(-1));
        inputController.bindPauseNavigation("DOWN", () -> movePauseSelection(1));

        // ESC : 일시정지 켜기/끄기 (라운드가 끝난 화면에서는 메인 메뉴로)
        inputController.bindUnfiltered("ESCAPE", "escapeAction", () -> {
            if (paused) setPaused(false);
            else if (isPlaying()) setPaused(true);
            else goHome();
        });

        // M : 메인 메뉴로. GamePanel처럼 일시정지 중이거나 라운드가 끝났을 때만 (게임 중 실수로 누르는 것 방지)
        inputController.bindUnfiltered("M", "menuAction", () -> {
            if (paused || !isPlaying()) goHome();
        });
    }

    private void movePauseSelection(int direction) {
        pauseUI.moveSelection(direction);
        AudioManager.get().playMenuSelect();
    }

    /// 일시정지 메뉴와 결과 상자의 글씨를 마우스로도 누를 수 있게 한다.
    private void bindMouse() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint(); // 화면은 16ms마다 다시 그려지니까 위치만 기억하면 됨
                // 일시정지 메뉴는 키보드 위/아래처럼 마우스를 올린 항목이 선택됨
                int index = pauseUI.optionAt(mouse);
                if (paused && index >= 0) pauseUI.select(index);
            }
            @Override public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (paused) {
                    int index = pauseUI.optionAt(p);
                    if (index < 0) return; // 메뉴 글씨가 아닌 곳을 누름
                    pauseUI.select(index);
                    confirmPauseSelection();
                } else if (!isPlaying()) { // 결과 상자가 떠 있을 때
                    if (resultOverlay.isNextAt(p)) confirmResult();
                    else if (resultOverlay.isMenuAt(p)) goHome();
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        shop.ThemeManager.paintBackground(g, getWidth(), getHeight()); // 상점에서 장착한 배경화면

        renderer.draw(g);

        // 라운드 및 매치 결과, 일시정지 화면
        if (!isPlaying()) resultOverlay.draw(g, mouse);
        else if (paused) pauseUI.draw(g);
        toast.draw(g, getWidth(), fonts.sansKRBlack());

        g.dispose();
    }
}