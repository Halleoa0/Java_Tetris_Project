package battle;

import audio.AudioManager;
import game.Board;
import game.Game;
import game.GameListener;
import game.Tetromino;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import settings.Settings;
import ui.LabelUI;
import ui.MenuFonts;

/**
 * 1:1 테트리스 배틀(유저 vs CPU 봇) 화면 패널 클래스이다.
 * 1인용 GamePanel과 같은 그림(Matrix.png, Mino.png, GameNamePanel.png)과 같은 글씨(LabelUI)를 그대로 쓴다.
 * 1인용 화면 좌표로 그린 뒤 g.translate로 왼쪽(플레이어) / 오른쪽(CPU)으로 옮겨서 두 번 그리는 방식이다.
 */
public final class BattlePanel extends JPanel {

    private static final int WIDTH = 1280, HEIGHT = 720;
    private static final int CELL = 25;
    private static final Color BACKGROUND = new Color(41, 41, 41); // 기존 단독 플레이와 동일한 다크 그레이 배경이다.

    // 1인용 GamePanel의 좌표 (Matrix.png가 x 378에 있을 때). 대전에서도 이 숫자 그대로 그린다.
    private static final int SOLO_MATRIX_X = 378, BOARD_X = 517, BOARD_Y = 111;
    private static final int MATRIX_W = 528, MATRIX_H = 505;

    // 대전에서 Matrix.png를 놓을 x 위치. 왼쪽(플레이어), 오른쪽(CPU)
    private static final int P1_MATRIX_X = 30, BOT_MATRIX_X = 722;

    private final BattleGame battle;
    private Runnable homeCallback = () -> { };
    private final Timer gameLoopTimer;
    private long lastTick;

    // 기존 게임 폰트 불러오기 //
    public final Font interBlack = MenuFonts.loadBlack();
    public final Font interMedium = MenuFonts.loadMedium();
    public final Font sansKRBlack = MenuFonts.loadKRBlack();
    public final Font orbitBlack = MenuFonts.loadOrbitBlack();
    public final Font orbitBold = MenuFonts.loadOrbitBold();

    // 기존 게임 이미지 에셋 불러오기 //
    private final Image matrixImage;
    private final Image pauseGuideImage;
    private final Image gameNamePanelImage;
    private final BufferedImage[] minoTiles;

    // 1인용과 같은 글씨(PPS, LINES, SCORE, 모드 이름)를 그리는 LabelUI. 플레이어용, CPU용 하나씩.
    private final LabelUI playerLabelUI;
    private final LabelUI botLabelUI;

    // 일시정지 상태 및 옵션 //
    private boolean paused = false;
    private static final String[] PAUSE_OPTIONS = {
            "RESUME",
            "RESTART ROUND",
            "RESTART MATCH",
            "QUIT"
    };
    private int pauseSelection = 0;
    private boolean ignoreSpaceUntilRelease = false;

    // 블록을 쌓으면 이전 입력이 무시되는 오류가 있어서 버퍼링을 사용.
    private int bufferedHorizontal; // 놓기 전, 좌우 이동 값을 저장 (-1좌, 0안함, 1우)
    private int bufferedRotation; // 놓기 전, 회전 방향 (-1반시계, 1시계) - IRS
    private boolean bufferedHold; // 놓기 전, 홀드를 눌렀는지 확인 - IHS
    private boolean bufferedSoftDrop; // 소프트 드롭 버퍼링
    private boolean bufferedHardDrop; // 하드 드롭 버퍼링

    // 키를 누르고 있는 상태 (GamePanel과 같은 방식). OS 키 반복 대신 직접 DAS/ARR로 반복 입력을 넣는다.
    private final Set<String> heldKeys = new HashSet<>();               // 지금 누르고 있는 이동 키 (좌, 우, 소프트 드롭)
    private final Set<String> pressedMoveKeys = new HashSet<>();        // 눌린 좌우 키 (반대 키로 바로 넘기기 위함)
    private final Set<String> repeatingKeys = new HashSet<>();          // DAS가 지나 ARR 반복 중인 키
    private final Map<String, Integer> heldKeyElapsed = new HashMap<>(); // 키별로 누른 뒤 흐른 시간(ms)
    private final Map<String, Runnable> heldKeyActions = new HashMap<>(); // 키별로 실행할 동작
    private final Set<Settings.Action> initialKeys = new HashSet<>();   // 누르고 있는 회전/홀드 키 (IRS/IHS)
    private int lastInitialRotation = 1;                                // 회전 키 두 개를 같이 누르면 마지막에 누른 방향

    // 마우스로 누를 수 있는 글씨들. 화면에 그릴 때 글씨가 있는 사각형을 기억해 두고, 클릭하면 그 안인지 확인한다.
    private final Rectangle[] pauseOptionBounds = new Rectangle[PAUSE_OPTIONS.length]; // 일시정지 메뉴 항목들
    private Rectangle resultNextBounds; // 결과 상자의 "NEXT ROUND" 또는 "REMATCH"
    private Rectangle resultMenuBounds; // 결과 상자의 "MENU"
    private final shop.ToastOverlay toast = new shop.ToastOverlay(); // 미션 달성 알림
    private Point mouse;                // 지금 마우스 위치 (마우스를 올린 글씨를 밝게 그리는 데 씀)

    public BattlePanel(BotBrain.Difficulty difficulty) {
        this.battle = new BattleGame(difficulty);
        setLayout(null);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BACKGROUND);
        setFocusable(true);

        // 기존 프로젝트 에셋들을 불러온다.
        this.matrixImage = loadImage("Images/Board/Matrix.png");
        this.pauseGuideImage = loadImage("Images/PauseGuide.png");
        this.gameNamePanelImage = loadImage("Images/Board/GameNamePanel.png");
        this.minoTiles = loadMinoTiles("Images/Mino.png");

        // GameNamePanel.png 안에 들어갈 이름. CPU는 난이도도 같이 보여줌.
        String botName = "CPU (" + difficulty.label + ")";
        this.playerLabelUI = new LabelUI(battle.playerGame, interBlack, interMedium, sansKRBlack, orbitBlack, orbitBold, gameNamePanelImage, "PLAYER 1");
        this.botLabelUI = new LabelUI(battle.botGame, interBlack, interMedium, sansKRBlack, orbitBlack, orbitBold, gameNamePanelImage, botName);

        bindKeys();
        bindMouse();

        // 위클리 미션 진행도 기록 + 달성 알림
        battle.playerGame.addListener(new shop.MissionTracker(battle.playerGame));
        toast.attach();

        // 16ms 간격으로 게임 상태를 갱신한다. (GamePanel과 같은 방식)
        this.gameLoopTimer = new Timer(16, e -> {
            long now = System.nanoTime();
            int elapsed = (int) Math.min(100, (now - lastTick) / 1_000_000L); // 지난 번 이후 흐른 시간(ms)
            lastTick = now;

            if (!paused) {
                battle.tick(elapsed);
                battle.playerGame.tick(elapsed);
                battle.botGame.tick(elapsed);
                if (battle.getState() == BattleGame.State.PLAYING) tickHeldKeys(elapsed); // DAS/ARR 반복 입력
                battle.playerGame.scoring.tick(elapsed); // 점수 글씨(T-SPIN, COMBO)가 사라지는 시간
                battle.botGame.scoring.tick(elapsed);
            }
            // 게임 안 하면 버퍼링, 누르고 있던 키 삭제
            if (battle.getState() != BattleGame.State.PLAYING) {
                clearBufferedSpawnInputs();
                if (!heldKeys.isEmpty() || !initialKeys.isEmpty()) clearHeldKeys();
            }
            repaint();
        });

        // 윈도우 포커스를 잃었을 때 자동으로 게임을 일시정지한다.
        addFocusListener(new FocusListener() {
            @Override public void focusGained(FocusEvent e) {
                AudioManager.get().setWindowActive(true);
            }
            @Override public void focusLost(FocusEvent e) {
                AudioManager.get().setWindowActive(false);
                ignoreSpaceUntilRelease = false;
                clearHeldKeys(); // 창을 벗어나면 키를 뗀 것을 못 받으므로 누르고 있던 키를 지움
                if (battle.getState() == BattleGame.State.PLAYING && !paused) {
                    setPaused(true);
                }
            }
        });

        // 플레이어 동작에 맞춰 효과음을 재생한다.
        battle.playerGame.addListener(new GameListener() {
            @Override public void onRestart() { clearBufferedSpawnInputs(); }
            @Override public void onSpawn() { applyBufferedSpawnInputs(); }
            @Override public void onMove(int dx, int dy) { AudioManager.get().playMove(); }
            @Override public void onRotate(int d) { AudioManager.get().playRotate(); }
            @Override public void onSoftDrop() { AudioManager.get().playSoftDrop(); }
            @Override public void onHardDrop() { AudioManager.get().playHardDrop(); }
            @Override public void onHold() { AudioManager.get().playHold(); }
            @Override public void onLock(Tetromino t) { AudioManager.get().playLock(); }
            @Override public void onLinesCleared(int l) { AudioManager.get().playLineClear(l); }
            @Override public void onGameOver() { AudioManager.get().playGameOver(); }
        });
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

    /// 일시정지 상태를 변경하고 CPU 봇 타이머를 동결하거나 재개한다.
    private void setPaused(boolean paused) {
        if (this.paused == paused) return;
        if (paused && battle.getState() != BattleGame.State.PLAYING) return;

        this.paused = paused;
        clearHeldKeys(); // GamePanel처럼 일시정지를 켜고 끌 때 누르고 있던 키를 지움
        if (paused) {
            pauseSelection = 0;
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
        switch (pauseSelection) {
            case 0 -> setPaused(false); // 계속하기
            case 1 -> { // 라운드 다시 시작
                setPaused(false);
                clearBufferedSpawnInputs(); // 게임 다시 시작하니까 버파 입력 초기화
                battle.startRound();
            }
            case 2 -> { // 매치 다시 시작
                setPaused(false);
                clearBufferedSpawnInputs();
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
        clearBufferedSpawnInputs();
        battle.startRound();
    }

    /// 키 입력을 액션에 연결한다.
    private void bindKeys() {
        // 좌우 이동, 소프트 드롭 : 누르고 있으면 DAS/ARR(소프트 드롭은 SDF)로 반복 (GamePanel의 bindHeld)
        bindHeld(Settings.Action.MOVE_LEFT,  () -> battle.playerGame.move(-1, 0));
        bindHeld(Settings.Action.MOVE_RIGHT, () -> battle.playerGame.move(1, 0));
        bindHeld(Settings.Action.SOFT_DROP,  () -> {
            battle.playerGame.setSoftDropHeld(true);
            battle.playerGame.softDrop();
        });
        // 회전, 홀드 : 누르고 있어도 한 번만 실행하고, 누르고 있는 상태는 다음 블록의 IRS/IHS에 사용 (GamePanel의 bindInitial)
        bindInitial(Settings.Action.ROTATE_CW,  () -> battle.playerGame.rotate(1));
        bindInitial(Settings.Action.ROTATE_CCW, () -> battle.playerGame.rotate(-1));
        bindInitial(Settings.Action.HOLD,       () -> battle.playerGame.hold());
        bind(Settings.Action.HARD_DROP,  () -> battle.playerGame.hardDrop());
        bind(Settings.Action.RESTART,    this::quickRestart);

        // 하드드롭 후 스페이스 뗌 감지 //
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(Settings.Action.HARD_DROP), "pauseSpaceReleased");
        getActionMap().put("pauseSpaceReleased", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { ignoreSpaceUntilRelease = false; }
        });

        // 일시정지 화면에서 위/아래 키로 메뉴 이동 (GamePanel과 같음)
        bindPauseNavigation("UP", -1);
        bindPauseNavigation("DOWN", 1);

        // ESC : 일시정지 켜기/끄기 (라운드가 끝난 화면에서는 setPaused가 알아서 무시함)
        bindUnfiltered("ESCAPE", "escapeAction", () -> {
            if (paused) setPaused(false);
            else if (battle.getState() == BattleGame.State.PLAYING) setPaused(true);
            else goHome();
        });

        // M : 메인 메뉴로. GamePanel처럼 일시정지 중이거나 라운드가 끝났을 때만 (게임 중 실수로 누르는 것 방지)
        bindUnfiltered("M", "menuAction", () -> {
            if (!paused && battle.getState() == BattleGame.State.PLAYING) return;
            goHome();
        });
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

    /// 일시정지 메뉴와 결과 상자의 글씨를 마우스로도 누를 수 있게 한다.
    private void bindMouse() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint(); // 화면은 16ms마다 다시 그려지니까 위치만 기억하면 됨
                // 일시정지 메뉴는 키보드 위/아래처럼 마우스를 올린 항목이 선택됨
                int index = pauseOptionAt(mouse);
                if (paused && index >= 0) pauseSelection = index;
            }
            @Override public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (paused) {
                    int index = pauseOptionAt(p);
                    if (index < 0) return; // 메뉴 글씨가 아닌 곳을 누름
                    pauseSelection = index;
                    confirmPauseSelection();
                } else if (battle.getState() != BattleGame.State.PLAYING) { // 결과 상자가 떠 있을 때
                    if (resultNextBounds != null && resultNextBounds.contains(p)) confirmResult();
                    else if (resultMenuBounds != null && resultMenuBounds.contains(p)) goHome();
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    /// p 위치에 있는 일시정지 메뉴 항목의 번호. 없으면 -1.
    private int pauseOptionAt(Point p) {
        for (int i = 0; i < pauseOptionBounds.length; i++) {
            if (pauseOptionBounds[i] != null && pauseOptionBounds[i].contains(p)) return i;
        }
        return -1;
    }

    private void bind(Settings.Action action, Runnable run) {
        String key = action.name();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (action == Settings.Action.HARD_DROP) { // GamePanel처럼 스페이스를 누르고 있어도 한 번만 하드 드롭
                    if (ignoreSpaceUntilRelease) return;
                    ignoreSpaceUntilRelease = true;
                }
                if (paused) {
                    if (action == Settings.Action.HARD_DROP) confirmPauseSelection(); // 일시정지 중 스페이스 = 메뉴 선택
                } else if (battle.getState() == BattleGame.State.PLAYING) {
                    if (battle.playerGame.isSpawnDelayed()) bufferSpawnInput(action);
                    else run.run();
                    repaint();
                } else if (action == Settings.Action.HARD_DROP) {
                    confirmResult();
                }
            }
        });
    }

    /// 입력을 버퍼링. 쌓고 나면 다시 키를 입력해야 하는 문제를 해결
    private void bufferSpawnInput(Settings.Action action) {
        switch (action) {
            case MOVE_LEFT -> bufferedHorizontal = -1;
            case MOVE_RIGHT -> bufferedHorizontal = 1;
            case ROTATE_CW -> bufferedRotation = 1;
            case ROTATE_CCW -> bufferedRotation = -1;
            case HOLD -> bufferedHold = true;
            case SOFT_DROP -> bufferedSoftDrop = true;
            case HARD_DROP -> bufferedHardDrop = true;
            default -> { }
        }
    }

    /// 스폰 대기가 끝나(블록 쌓을 수 있을 때) 버퍼에 저장됐던 입력을 가져옴
    private void applyBufferedSpawnInputs() {
        if (battle.getState() != BattleGame.State.PLAYING
                || battle.playerGame.active == null || battle.playerGame.gameOver) {
            clearBufferedSpawnInputs();
            return;
        }

        int horizontal = bufferedHorizontal;
        int rotation = bufferedRotation;
        boolean hold = bufferedHold;
        boolean softDrop = bufferedSoftDrop;
        boolean hardDrop = bufferedHardDrop;
        clearBufferedSpawnInputs();

        // 키를 아직 누르고 있으면 Game이 IHS/IRS로 이미 처리했으므로 버퍼는 건너뜀 (두 번 홀드/회전 방지)
        if (Settings.get().ihs() && initialKeys.contains(Settings.Action.HOLD)) hold = false;
        if (Settings.get().irs() && (initialKeys.contains(Settings.Action.ROTATE_CW)
                || initialKeys.contains(Settings.Action.ROTATE_CCW))) rotation = 0;

        if (hold) battle.playerGame.hold();
        if (rotation != 0) battle.playerGame.rotate(rotation);
        if (horizontal != 0) battle.playerGame.move(horizontal, 0);
        if (softDrop) battle.playerGame.softDrop();
        if (hardDrop) battle.playerGame.hardDrop();
    }

    /// 버퍼링 초기화 (게임 다시 시작, 멈췄을 떄 등에 적용)
    private void clearBufferedSpawnInputs() {
        bufferedHorizontal = 0;
        bufferedRotation = 0;
        bufferedHold = false;
        bufferedSoftDrop = false;
        bufferedHardDrop = false;
    }


    /// 이 아래의 키 유지 입력(DAS/ARR, SDF, IRS/IHS) 코드는 GamePanel과 같습니다.
    /// (대전에서는 라운드 진행 중일 때만 입력을 받고, 스폰 대기 중 입력은 위의 버퍼에도 저장하는 것만 다름)


    /// 회전, 홀드 키. 처음 눌렀을 때 한 번만 실행하고 누르고 있는 동안의 OS 반복 입력은 무시한다.
    private void bindInitial(Settings.Action action, Runnable run) {
        String pressedName = action.name() + "InitialPressed";
        String releasedName = action.name() + "InitialReleased";
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), pressedName);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), releasedName);
        getActionMap().put(pressedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused || battle.getState() != BattleGame.State.PLAYING) return;
                if (!initialKeys.add(action)) return; // 이미 누르고 있던 키 (OS 반복 입력)
                if (action == Settings.Action.ROTATE_CW) lastInitialRotation = 1;
                else if (action == Settings.Action.ROTATE_CCW) lastInitialRotation = -1;
                updateInitialInputs();
                if (battle.playerGame.isSpawnDelayed()) bufferSpawnInput(action);
                else run.run();
                repaint();
            }
        });
        getActionMap().put(releasedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                initialKeys.remove(action);
                updateInitialInputs();
            }
        });
    }

    /// 누르고 있는 회전/홀드 키를 Game에 알려서 다음 블록이 나올 때 IRS/IHS로 쓰게 한다.
    private void updateInitialInputs() {
        boolean cw = Settings.get().irs() && initialKeys.contains(Settings.Action.ROTATE_CW);
        boolean ccw = Settings.get().irs() && initialKeys.contains(Settings.Action.ROTATE_CCW);
        int direction = cw && ccw ? lastInitialRotation : cw ? 1 : ccw ? -1 : 0;
        battle.playerGame.setInitialInputs(initialKeys.contains(Settings.Action.HOLD), direction);
    }

    /// 좌우 이동, 소프트 드롭 키. 누를 때 한 번 실행하고, 누르고 있으면 tickHeldKeys가 반복한다.
    private void bindHeld(Settings.Action action, Runnable run) {
        String key = action.name();
        String pressedName = key + "HeldPressed";
        String releasedName = key + "HeldReleased";
        heldKeyActions.put(key, run);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), pressedName);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), releasedName);
        getActionMap().put(pressedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused || battle.getState() != BattleGame.State.PLAYING) return;
                if (battle.playerGame.isSpawnDelayed() && !heldKeys.contains(key)) bufferSpawnInput(action);
                if (action == Settings.Action.MOVE_LEFT || action == Settings.Action.MOVE_RIGHT)
                    pressedMoveKeys.add(key);
                if (!oppositeMoveHeld(action)) activateHeldKey(key);
            }
        });
        getActionMap().put(releasedName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { releaseHeldKey(key); }
        });
    }

    private void activateHeldKey(String key) {
        if (!heldKeys.add(key)) return; // 이미 누르고 있던 키 (OS 반복 입력은 무시하고 DAS/ARR로 반복)
        heldKeyElapsed.put(key, 0);
        repeatingKeys.remove(key);
        heldKeyActions.get(key).run();
        repaint();
    }

    private boolean oppositeMoveHeld(Settings.Action action) {
        return (action == Settings.Action.MOVE_LEFT && heldKeys.contains(Settings.Action.MOVE_RIGHT.name()))
                || (action == Settings.Action.MOVE_RIGHT && heldKeys.contains(Settings.Action.MOVE_LEFT.name()));
    }

    /// 누르고 있는 키를 DAS(처음 기다리는 시간) 뒤 ARR(반복 간격)마다 다시 실행한다.
    private void tickHeldKeys(int elapsedMs) {
        for (String key : heldKeys) {
            int elapsed = heldKeyElapsed.getOrDefault(key, 0) + elapsedMs;
            boolean softDrop = key.equals(Settings.Action.SOFT_DROP.name());

            if (!repeatingKeys.contains(key)) {
                int das = Settings.get().dasMs();
                if (elapsed < das) {
                    heldKeyElapsed.put(key, elapsed);
                    continue;
                }
                elapsed -= das;
                repeatingKeys.add(key);
                if (!softDrop) heldKeyActions.get(key).run();
            }

            // 소프트 드롭은 Game.tick()의 SDF가 낙하 속도를 담당하므로 여기서 반복하지 않음
            if (!softDrop) {
                int arr = Settings.get().arrMs();
                if (arr == 0) {
                    for (int i = 0; i < Board.WIDTH; i++) heldKeyActions.get(key).run();
                    elapsed = 0;
                } else {
                    int repeats = Math.min(Board.WIDTH, elapsed / arr);
                    for (int i = 0; i < repeats; i++) heldKeyActions.get(key).run();
                    elapsed %= arr;
                }
            }
            heldKeyElapsed.put(key, elapsed);
        }
    }

    private void releaseHeldKey(String key) {
        boolean wasHeld = heldKeys.remove(key);
        repeatingKeys.remove(key);
        heldKeyElapsed.remove(key);
        if (key.equals(Settings.Action.SOFT_DROP.name())) battle.playerGame.setSoftDropHeld(false);
        pressedMoveKeys.remove(key);
        // 반대 키가 이미 눌려 있으면 OS 반복 입력을 기다리지 않고 즉시 전환한다.
        String opposite = key.equals(Settings.Action.MOVE_LEFT.name()) ? Settings.Action.MOVE_RIGHT.name()
                : key.equals(Settings.Action.MOVE_RIGHT.name()) ? Settings.Action.MOVE_LEFT.name() : null;
        if (wasHeld && opposite != null && pressedMoveKeys.contains(opposite)) activateHeldKey(opposite);
    }

    /// 누르고 있던 키를 모두 뗀 상태로 만든다. (일시정지, 창 벗어남, 라운드 끝)
    private void clearHeldKeys() {
        heldKeys.clear();
        pressedMoveKeys.clear();
        repeatingKeys.clear();
        heldKeyElapsed.clear();
        battle.playerGame.setSoftDropHeld(false);
        initialKeys.clear();
        battle.playerGame.setInitialInputs(false, 0);
    }

    private void bindPauseNavigation(String key, int direction) {
        KeyStroke stroke = KeyStroke.getKeyStroke("pressed " + key);
        Object gameplayBinding = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(stroke);
        javax.swing.Action gameplayAction = gameplayBinding == null ? null : getActionMap().get(gameplayBinding);
        String bindingName = "pauseNav_" + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, bindingName);
        getActionMap().put(bindingName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused) {
                    pauseSelection = Math.floorMod(pauseSelection + direction, PAUSE_OPTIONS.length);
                    AudioManager.get().playMenuSelect();
                    repaint();
                } else if (gameplayAction != null) {
                    gameplayAction.actionPerformed(e);
                }
            }
        });
    }

    private void bindUnfiltered(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                action.run();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        shop.ThemeManager.paintBackground(g, getWidth(), getHeight()); // 상점에서 장착한 배경화면

        // 왼쪽: 플레이어 1 / 오른쪽: CPU (그리는 방법이 같아서 drawSide 하나로 그림)
        drawSide(g, P1_MATRIX_X, playerLabelUI, battle.playerGame, battle.playerGarbage, battle.getPlayerTotalAttack());
        drawSide(g, BOT_MATRIX_X, botLabelUI, battle.botGame, battle.botGarbage, battle.getBotTotalAttack());

        // 가운데: 라운드, 승리 수, 남은 시간, 목표 점수
        drawCenterInfo(g);

        // 라운드 및 매치 결과, 일시정지 화면
        if (battle.getState() == BattleGame.State.ROUND_OVER) {
            drawRoundOver(g);
        } else if (battle.getState() == BattleGame.State.MATCH_OVER) {
            drawMatchOver(g);
        } else if (paused) {
            drawPauseOverlay(g); // GamePanel과 같은 일시정지 화면
        }
        toast.draw(g, getWidth(), sansKRBlack);

        g.dispose();
    }

    /// 한 사람의 화면(Matrix.png, 홀드, 넥스트, 보드, 방해 줄 게이지, 글씨)을 그린다.
    /// 1인용 GamePanel과 똑같은 좌표로 그리고, g.translate로 matrixX 위치까지 옮긴다.
    private void drawSide(Graphics2D g, int matrixX, LabelUI labelUI, Game game, GarbageQueue garbage, int attack) {
        int shift = matrixX - SOLO_MATRIX_X; // 1인용 화면보다 얼마나 옮길지 (왼쪽이면 음수)
        g.translate(shift, 0); // 지금부터 그리는 것은 전부 shift만큼 옮겨져서 그려짐

        g.drawImage(matrixImage, SOLO_MATRIX_X, BOARD_Y, MATRIX_W, MATRIX_H, this);
        drawMatrixPreviews(g, game);
        drawBoard(g, game);
        drawGarbageBar(g, BOARD_X - 14, BOARD_Y, garbage.getPendingCount());

        // 1인용과 같은 글씨들 (LabelUI)
        labelUI.drawGameNamePanel(g);
        labelUI.drawPpsLabel(g);
        labelUI.drawLineLabel(g);
        labelUI.drawScoreValueLabel(g);
        drawAttackLabel(g, attack); // 1인용의 TIME 자리. 대전은 시간을 가운데에 따로 보여줘서 공격량을 대신 보여줌
        labelUI.drawScoreLabel(g);  // T-SPIN, COMBO 같은 점수 글씨

        g.translate(-shift, 0); // 원래 위치로 되돌림
    }

    /// LabelUI의 TIME 글씨와 같은 모양으로 지금까지 보낸 공격 줄 수를 그린다.
    private void drawAttackLabel(Graphics2D g, int attack) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        g.drawString("ATTACK", 783, 539);
        g.setFont(interBlack.deriveFont(24f));
        g.drawString(String.valueOf(attack), 783, 539 + 28);
    }

    /// 가운데 정보. LabelUI와 같은 글씨 모양(작은 제목 + 큰 숫자)으로 그린다.
    private void drawCenterInfo(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(interBlack.deriveFont(36f));
        drawCentered(g, "VS", 170);

        drawCenterLabel(g, "ROUND", battle.getCurrentRound() + " / 3", 250, Color.WHITE);
        drawCenterLabel(g, "WINS", battle.getPlayerWins() + " : " + battle.getBotWins(), 330, Color.WHITE);

        // 남은 시간. 마지막 45초(HURRY UP)에는 공격이 1.5배가 되고, 글씨가 빨간색으로 바뀜
        int timeSec = (int) Math.ceil(battle.getRoundTimer());
        String timeText = String.format(Locale.ROOT, "%02d:%02d", timeSec / 60, timeSec % 60);
        if (battle.isHurryUp()) drawCenterLabel(g, "ATTACK x1.5", timeText, 466, new Color(255, 80, 80));
        else drawCenterLabel(g, "TIME", timeText, 466, Color.WHITE);

        drawCenterLabel(g, "TARGET", String.format(Locale.US, "%,d", BattleGame.TARGET_SCORE), 539, Color.WHITE);
    }

    /// 작은 제목(16) 아래에 큰 값(24)을 화면 가운데 정렬로 그린다. (LabelUI와 같은 글씨 크기)
    private void drawCenterLabel(Graphics2D g, String title, String value, int y, Color valueColor) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        drawCentered(g, title, y);
        g.setColor(valueColor);
        g.setFont(interBlack.deriveFont(24f));
        drawCentered(g, value, y + 28);
    }

    /// 글씨를 화면 가로 가운데에 그린다.
    private void drawCentered(Graphics2D g, String text, int y) {
        g.drawString(text, (WIDTH - g.getFontMetrics().stringWidth(text)) / 2, y);
    }


    /// 이 아래의 보드, 미노, 넥스트, 홀드를 그리는 코드는 GamePanel과 같습니다.
    /// (game을 매개변수로 받는 것과 방해 줄을 회색으로 그리는 것만 다름)


    // 보드와 쌓은 블록, 현재블록과 고스트를 그림
    private void drawBoard(Graphics2D g, Game game) {
        for (int row = Board.MIN_ROW; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                int sx = BOARD_X + col * CELL, sy = BOARD_Y + (row - Board.HIDDEN_ROWS) * CELL;
                Tetromino locked = game.board.get(col, row);
                if (locked == null) continue;
                if (game.board.isGarbageCell(col, row)) drawTile(g, sx, sy, 7); // 방해 줄은 회색 타일(7)
                else drawTile(g, sx, sy, minoTileIndex(locked));
            }
        }

        if (!game.gameOver && game.active != null) { // 스폰 딜레이 중에는 현재 블록이 없음(null)
            if (Settings.get().ghostPiece()) {
                // 현재 블록을 아래로 복사 이동해 예상 착지 위치(고스트)를 먼저 그린다.
                int ghostY = game.y;
                while (game.board.canPlace(game.active, game.x, ghostY + 1, game.rotation)) ghostY++;
                drawPiece(g, game.active, game.x, ghostY, game.rotation, true);
            }
            drawPiece(g, game.active, game.x, game.y, game.rotation, false);
        }
    }

    // 미노 만들기
    private void drawPiece(Graphics2D g, Tetromino type, int x, int y, int rotation, boolean ghost) {
        for (int[] cell : type.cells(rotation)) {
            int bx = x + cell[0], by = y + cell[1];
            if (by < Board.MIN_ROW || by >= Board.HEIGHT) continue;
            int visibleY = by - Board.HIDDEN_ROWS;
            drawTile(g, BOARD_X + bx * CELL, BOARD_Y + visibleY * CELL, ghost ? 7 : minoTileIndex(type));
        }
    }

    // Mino.png에서 잘라 둔 타일 한 칸을 그림 (tile : 0 ~ 6 미노, 7 고스트/방해 줄)
    private void drawTile(Graphics2D g, int x, int y, int tile) {
        g.drawImage(minoTiles[tile], x, y, CELL, CELL, this);
    }

    private int minoTileIndex(Tetromino type) {
        return switch (type) {
            case Zmino -> 0;
            case Lmino -> 1;
            case Omino -> 2;
            case Smino -> 3;
            case Imino -> 4;
            case Jmino -> 5;
            case Tmino -> 6;
        };
    }

    // Next와 Hold에 미노를 표시합니다.
    private void drawMatrixPreviews(Graphics2D g, Game game) {
        if (game.held != null) {
            drawPreviewPiece(g, game.held, 386, 112, 126, 78, 25);
        }

        List<Tetromino> next = game.preview();
        for (int i = 0; i < next.size(); i++) {
            drawPreviewPiece(g, next.get(i), 774, 120 + i * 68, 126, 50, 25);
        }
    }

    // 4개까지 NEXT 미노를 보여줌
    private void drawPreviewPiece(Graphics2D g, Tetromino type, int x, int y, int width, int height, int unit) {
        int minX = 4, maxX = 0, minY = 4, maxY = 0;
        int[][] cells = type.cells(0);
        for (int[] cell : cells) {
            minX = Math.min(minX, cell[0]); maxX = Math.max(maxX, cell[0]);
            minY = Math.min(minY, cell[1]); maxY = Math.max(maxY, cell[1]);
        }
        int pieceWidth = (maxX - minX + 1) * unit;
        int pieceHeight = (maxY - minY + 1) * unit;
        int startX = x + (width - pieceWidth) / 2 - minX * unit;
        int startY = y + (height - pieceHeight) / 2 - minY * unit;
        for (int[] cell : cells) {
            g.drawImage(minoTiles[minoTileIndex(type)], startX + cell[0] * unit, startY + cell[1] * unit, unit, unit, this);
        }
    }

    /// 방해 줄 대기열 게이지. 기다리는 방해 줄 수만큼 보드 왼쪽에 노란색(4줄 이상이면 빨간색)으로 채운다.
    private void drawGarbageBar(Graphics2D g, int x, int y, int pendingLines) {
        int barH = 20 * CELL;
        g.setColor(new Color(25, 28, 36));
        g.fillRect(x, y, 8, barH);

        if (pendingLines > 0) {
            int fillH = Math.min(barH, pendingLines * CELL);
            g.setColor(pendingLines >= 4 ? new Color(255, 80, 80) : Color.YELLOW);
            g.fillRect(x + 1, y + barH - fillH, 6, fillH);
        }
    }


    /// ---------- 결과 / 일시정지 화면 ----------


    /// 라운드 종료 화면
    private void drawRoundOver(Graphics2D g) {
        String title = battle.getRoundWinner() == BattleGame.Winner.PLAYER ? "PLAYER 1 WINS ROUND" : "CPU WINS ROUND";
        String reason = switch (battle.getWinReason()) {
            case KO -> battle.getRoundWinner() == BattleGame.Winner.PLAYER ? "KO - CPU TOPPED OUT" : "KO - YOU TOPPED OUT";
            case TARGET_SCORE -> String.format(Locale.US, "REACHED %,d POINTS FIRST", BattleGame.TARGET_SCORE);
            case TIME_OUT -> "TIME UP - MORE ATTACK / SCORE WINS";
            default -> "";
        };
        drawResultBox(g, title, reason, "[SPACE] NEXT ROUND");
    }

    /// 매치(3판 2선승) 종료 화면
    private void drawMatchOver(Graphics2D g) {
        String title = battle.getMatchWinner() == BattleGame.Winner.PLAYER ? "YOU WIN!" : "CPU WINS!";
        String score = "FINAL " + battle.getPlayerWins() + " : " + battle.getBotWins();
        drawResultBox(g, title, score, "[SPACE] REMATCH");
    }

    /// 결과 상자. GamePanel의 GAME OVER 상자와 같은 모양(어두운 둥근 상자 + 흰 글씨)이다.
    /// nextText : 누를 수 있는 글씨 (다음 라운드 또는 다시 대결). 옆에 "(M) MENU"도 같이 그린다.
    private void drawResultBox(Graphics2D g, String title, String line1, String nextText) {
        int w = 440, h = 130, x = (WIDTH - w) / 2, y = (HEIGHT - h) / 2;
        g.setColor(new Color(12, 16, 25, 220));
        g.fillRoundRect(x, y, w, h, 12, 12);

        g.setColor(Color.WHITE);
        g.setFont(interBlack.deriveFont(24f));
        drawCentered(g, title, y + 45);
        g.setFont(interMedium.deriveFont(14f));
        drawCentered(g, line1, y + 78);

        // 아래 줄 : 누를 수 있는 글씨 2개를 가운데에 나란히 그림 (사이 간격 40)
        g.setFont(interBlack.deriveFont(15f));
        String menuText = "[M] MENU";
        FontMetrics fm = g.getFontMetrics();
        int startX = (WIDTH - fm.stringWidth(nextText) - 40 - fm.stringWidth(menuText)) / 2;
        resultNextBounds = drawClickableText(g, nextText, startX, y + 108);
        resultMenuBounds = drawClickableText(g, menuText, startX + fm.stringWidth(nextText) + 40, y + 108);
    }

    /// 누를 수 있는 글씨를 그리고, 글씨가 있는 사각형을 돌려준다. 마우스를 올리면 흰색, 아니면 회색.
    private Rectangle drawClickableText(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        Rectangle bounds = new Rectangle(x, y - fm.getAscent(), fm.stringWidth(text), fm.getHeight());
        bounds.grow(8, 4); // 글씨보다 조금 넓게 눌러도 되도록
        g.setColor(mouse != null && bounds.contains(mouse) ? Color.WHITE : new Color(150, 150, 150));
        g.drawString(text, x, y);
        return bounds;
    }

    /// GamePanel과 같은 전체화면 일시정지 화면을 그린다.
    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        Map<TextAttribute, Float> fontAttributes = Map.of(TextAttribute.TRACKING, 0.3f);
        g.setColor(Color.WHITE);
        g.setFont(orbitBlack.deriveFont(30f).deriveFont(fontAttributes));
        String title = "PAUSE";
        g.drawString(title, 40, 70);

        int spacing = 80;
        int firstCenterY = getHeight() / 2 - (PAUSE_OPTIONS.length - 1) * spacing / 2;
        for (int i = 0; i < PAUSE_OPTIONS.length; i++) {
            g.setColor(i == pauseSelection ? Color.WHITE : new Color(70, 70, 70));
            g.setFont((i == pauseSelection ? interBlack : interMedium).deriveFont(32f).deriveFont(fontAttributes));
            FontMetrics metrics = g.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(PAUSE_OPTIONS[i])) / 2;
            int y = firstCenterY + i * spacing + (metrics.getAscent() - metrics.getDescent()) / 2;
            g.drawString(PAUSE_OPTIONS[i], x, y);

            // 마우스 클릭용으로 글씨가 있는 사각형을 기억해 둠 (조금 넓게)
            pauseOptionBounds[i] = new Rectangle(x, y - metrics.getAscent(), metrics.stringWidth(PAUSE_OPTIONS[i]), metrics.getHeight());
            pauseOptionBounds[i].grow(20, 10);
        }
        int guideX = (getWidth() - pauseGuideImage.getWidth(this)) / 2;
        int guideY = getHeight() - pauseGuideImage.getHeight(this) - 50;
        g.drawImage(pauseGuideImage, guideX, guideY, this);
    }


    /// 파일 경로로부터 이미지를 로드한다.
    private Image loadImage(String path) {
        try {
            return javax.imageio.ImageIO.read(Path.of(path).toFile());
        } catch (IOException e) {
            return null;
        }
    }

    /// 미노 스프라이트 시트(Mino.png)를 25x25 단위 8개 타일로 자르고 색맹 모드를 반영한다.
    private BufferedImage[] loadMinoTiles(String path) {
        try {
            BufferedImage sheet = javax.imageio.ImageIO.read(Path.of(path).toFile());
            if (sheet.getWidth() != 200 || sheet.getHeight() != 25) {
                throw new IllegalStateException("Image size is not 200x25: " + path);
            }

            BufferedImage[] tiles = new BufferedImage[8];
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = sheet.getSubimage(i * 25, 0, 25, 25);
            }

            // 색맹 모드 적용 (설정에 맞춰 미노 타일 색상을 변환한다)
            if (Settings.get().colorMode() != Settings.ColorMode.OFF) {
                for (Tetromino t : Tetromino.values()) {
                    int idx = minoTileIndex(t);
                    tiles[idx] = tint(tiles[idx], Settings.get().minoColor(t));
                }
            }

            shop.ThemeManager.applyMinoSkin(tiles, this::minoTileIndex); // 상점에서 장착한 미노 스킨
            return tiles;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mino image: " + path, e);
        }
    }

    /// 원본 타일의 명암은 유지하고 대상 색상으로 틴팅한다.
    private static BufferedImage tint(BufferedImage src, Color target) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float base = (target.getRed() * 0.299f + target.getGreen() * 0.587f + target.getBlue() * 0.114f) / 255f;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                float lum = (((p >> 16) & 255) * 0.299f + ((p >> 8) & 255) * 0.587f + (p & 255) * 0.114f) / 255f;
                float k = base == 0 ? 1f : lum / base;
                int r = Math.min(255, Math.round(target.getRed() * k));
                int gr = Math.min(255, Math.round(target.getGreen() * k));
                int b = Math.min(255, Math.round(target.getBlue() * k));
                out.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
        }
        return out;
    }
}
