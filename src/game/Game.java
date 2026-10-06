package game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import settings.Settings;


public final class Game {

    /** 화면에 미리 표시할 다음 블록 수. */
    public static final int PREVIEW_COUNT = 4;
    private static final int MAX_LOCK_RESETS = 15;
    public final Board board = new Board();
    private final Random random = new Random();
    // bag은 7종 블록을 섞어 담고, queue는 다음 블록의 순서를 보관한다.
    private final Deque<Tetromino> bag = new ArrayDeque<>(), queue = new ArrayDeque<>();

    // 현재 블록과 Hold 블록의 상태. x, y는 블록의 기준 격자 원점이다.
    public Tetromino active, held;
    public int x, y, rotation;
    public int dropCount = 0; // 쌓은 블록 수
    public float pps, timer = 0.0f; // Pieces Per Second - 초당 쌓은 블록
    public boolean startCal = false;


    public final ScoreManager scoring = new ScoreManager();
    public boolean gameOver, holdUsed, lastMoveWasRotation;

    private int lastKickIndex;
    private long lockElapsed;
    private int lockResets;
    private final List<GameListener> listeners = new ArrayList<>();
    private PieceGenerator pieceGenerator;
    private boolean gravityEnabled = true;
    private boolean spawnVisible;
    private boolean initialHold;
    private int initialRotation;
    private long spawnDelayElapsed;
    private boolean spawnDelayed;
    private final int timeLimitMs;
    private int elapsedTimeMs;
    // 스프린트: 목표 줄 수(0이면 해당 없음), 시작 카운트다운, 목표 달성 여부
    private static final int COUNTDOWN_MS = 3000;
    private final int targetLines;
    private int countdownMs;
    private boolean goalReached;

    public Game() { this(null); }

    public Game(int timeLimitMs) { this(null, timeLimitMs, 0); }

    /** 목표 줄 수를 가장 빨리 채우는 스프린트용 게임. 시작할 때 3초 카운트다운 */
    public static Game sprint(int targetLines) { return new Game(null, 0, Math.max(1, targetLines)); }

    /** generator가 null이면 기존 7-bag 랜덤 생성을 사용 */
    public Game(PieceGenerator generator) {
        this(generator, 0, 0);
    }

    private Game(PieceGenerator generator, int timeLimitMs, int targetLines) {
        pieceGenerator = generator;
        this.timeLimitMs = Math.max(0, timeLimitMs);
        this.targetLines = Math.max(0, targetLines);
        restart();
    }

    /** 제한 시간이 있는 모드는 남은 초, 기본 모드는 경과 초를 표시한다. */
    public int displayTimeSeconds() {
        if (targetLines > 0) return elapsedTimeMs / 1000;
        return timeLimitMs > 0 ? (timeLimitMs - elapsedTimeMs + 999) / 1000 : (int) timer;
    }

    /** 목표 줄 수. 0이면 목표가 없는 모드. */
    public int targetLines() { return targetLines; }

    /** 목표를 달성해서 끝난 게임인지. (게임 오버와 구분하기 위함. 이때 gameOver도 true가 됨) */
    public boolean goalReached() { return goalReached; }

    /** 스프린트 경과 시간(ms). 카운트다운이 끝난 뒤부터 셈 */
    public int elapsedTimeMs() { return elapsedTimeMs; }

    /** 시작 카운트다운 중인지. 이 동안은 조작·중력·시간이 모두 멈춤 */
    public boolean isCountingDown() { return countdownMs > 0; }

    /** 화면에 보여줄 카운트다운 숫자(3, 2, 1). 카운트다운이 아니면 0. */
    public int countdownSeconds() { return (countdownMs + 999) / 1000; }

    /** 생성기는 다음 restart()부터 적용된다. null은 기본 7-bag 생성기를 뜻한다. */
    public void setPieceGenerator(PieceGenerator generator) { pieceGenerator = generator; }

    public void addListener(GameListener listener) { if (listener != null) listeners.add(listener); }
    public void removeListener(GameListener listener) { listeners.remove(listener); }

    public void setGravityEnabled(boolean enabled) {
        gravityEnabled = enabled;
        gravityElapsed = 0;
        lockElapsed = 0;
    }

    /** 다음 블록을 화면에 보이는 행에서 시작할지 설정한다. 다음 스폰/재시작부터 적용된다. */
    public void setSpawnVisible(boolean visible) { spawnVisible = visible; }

    /** 유지 중인 키를 다음 미노의 IHS/IRS에 사용한다. 회전 방향은 -1, 0, 1. */
    public void setInitialInputs(boolean hold, int rotationDirection) {
        Settings settings = Settings.get();
        initialHold = settings.ihs() && hold;
        initialRotation = settings.irs() ? Integer.signum(rotationDirection) : 0;
        //initialHold = hold;
        // initialRotation = Integer.signum(rotationDirection);
    }

    public boolean isSpawnDelayed() { return spawnDelayed; }

    public float calPPS() {
        pps = dropCount / timer;
        return pps;
    }

    /** 보드와 점수, 블록 대기열을 초기 상태로 되돌린다. */
    public void restart() {
        elapsedTimeMs = 0;
        goalReached = false;
        countdownMs = targetLines > 0 ? COUNTDOWN_MS : 0;
        dropCount = 0; startCal = false; timer = 0.0f;
        board.clear(); bag.clear(); queue.clear(); held = null;
        scoring.reset();
        scoring.level = Settings.get().startLevel(); gameOver = false; holdUsed = false;
        lockElapsed = 0; lockResets = 0; gravityElapsed = 0; spawnDelayElapsed = 0; spawnDelayed = false;
        for (int i = 0; i < PREVIEW_COUNT + 1; i++) queue.addLast(nextPiece());
        spawnNext();
    }

    /// 테트리스의 7-beg 시스템을 위한 함수로, 7개의 미노가 균등하게 나오게 하기 위함이다.
    private Tetromino drawPiece() {
        if (bag.isEmpty()) {
            // 한 묶음에 각 블록을 하나씩 넣고 섞어, 블록 종류를 고르게 공급한다.
            List<Tetromino> pieces = new ArrayList<>(List.of(Tetromino.values()));
            Collections.shuffle(pieces, random);
            bag.addAll(pieces);
        }
        return bag.removeFirst();
    }

    private Tetromino nextPiece() { return pieceGenerator == null ? drawPiece() : pieceGenerator.next(); }

    /** 화면에 보여줄 다음 블록 목록을 반환한다. */
    public List<Tetromino> preview() { return new ArrayList<>(queue).subList(0, Math.min(PREVIEW_COUNT, queue.size())); }

    /// Next 미노를 생성
    private void spawnNext() {
        // 새 블록은 보드 위쪽 숨김 영역에서 시작한다.
        // Next를 최신화한다. (최근 미노 제거, 마지막미노 생성)
        active = takeNextPiece();
        holdUsed = false;
        gravityElapsed = 0;
        // 홀드로 최종 미노를 먼저 결정한 뒤 한 번만 회전한다.
        if (initialHold) {
            swapHeldPiece();
            holdUsed = true;
        }
        enterPiece();
    }

    private Tetromino takeNextPiece() {
        Tetromino piece = queue.removeFirst();
        queue.addLast(nextPiece());
        return piece;
    }

    private void swapHeldPiece() {
        Tetromino current = active;
        active = held == null ? takeNextPiece() : held;
        held = current;
    }

    private void enterPiece() {
        x = 3; y = spawnVisible ? Board.HIDDEN_ROWS : -1; rotation = 0;
        lastMoveWasRotation = false; lockElapsed = 0; lockResets = 0;
        // IRS가 스폰 충돌을 해소할 수 있으므로 최종 위치에서 게임오버를 판정한다.
        if (initialRotation != 0) rotate(initialRotation);
        if (!board.canPlace(active, x, y, rotation)) setGameOver();
    }

    /** dx/dy만큼 이동한다. 충돌하면 false, 이동하면 true를 반환한다. */
    public boolean move(int dx, int dy) {
        if (gameOver || spawnDelayed || active == null || !board.canPlace(active, x + dx, y + dy, rotation)) return false;
        x += dx; y += dy; lastMoveWasRotation = false;
        for (GameListener listener : List.copyOf(listeners)) listener.onMove(dx, dy);
        afterPlayerMove(); return true;
    }

    /** direction이 양수면 시계 방향, 음수면 반시계 방향으로 SRS 회전을 시도한다. */
    public boolean rotate(int direction) {
        int rotationDirection = direction > 0 ? 1 : -1;
        if (countdownMs > 0) return false;
        if (gameOver || spawnDelayed || active == null || active == Tetromino.Omino) {
            notifyRotate(rotationDirection);
            return false;
        }
        int from = rotation, to = (rotation + (direction > 0 ? 1 : 3)) & 3;
        // 회전 후 겹치면 SRS 킥 후보를 순서대로 적용해 옆이나 위로 이동을 시도한다.
        int[][] tests = kickTests(active, from, to);
        for (int i = 0; i < tests.length; i++) {
            int nx = x + tests[i][0], ny = y + tests[i][1];
            if (board.canPlace(active, nx, ny, to)) {
                x = nx; y = ny; rotation = to; lastMoveWasRotation = true; lastKickIndex = i;
                afterPlayerMove();
                notifyRotate(rotationDirection);
                return true;
            }
        }
        notifyRotate(rotationDirection);
        return false;
    }

    private void notifyRotate(int direction) {
        for (GameListener listener : List.copyOf(listeners)) listener.onRotate(direction);
    }


    /// 테트리스의 락 딜레이 시스템을 위한 변수. 락 딜레이는 블록이 바닥에 닿자마자 놓아지는 것이 아니라 약간의 유예시간을 주는 시스템
    private void afterPlayerMove() {
        if (board.canPlace(active, x, y + 1, rotation)) lockElapsed = 0;
        else if (lockResets < MAX_LOCK_RESETS) { lockElapsed = 0; lockResets++; }
    }

    /** 가능한 가장 아래까지 내린 뒤 고정한다. 낙하 거리에 따라 점수를 준다. */
    public void hardDrop() {
        if (gameOver || countdownMs > 0) return;
        int distance = 0;
        while (board.canPlace(active, x, y + 1, rotation)) { y++; distance++; }
        scoring.onHardDrop(distance);
        lockPiece();
    }

    /**
     * 소프트 드롭. 키를 누르고 있는 동안 GamePanel의 반복 입력 대신에도
     * 실제 중력 속도를 기준으로 처리할 수 있도록 한 칸을 즉시 내림
     */
    public void softDrop() {
        if (gameOver) return;
        if (move(0, 1)) scoring.onSoftDrop(1);
    }

    /** 현재 블록을 Hold 칸과 바꾸며, 블록 하나당 한 번만 허용한다. */
    public void hold() {
        if (gameOver || holdUsed || spawnDelayed || active == null) return;
        if (held == null) gravityElapsed = 0;
        swapHeldPiece();
        holdUsed = true;
        enterPiece();
    }

    /** 타이머가 전달한 경과 시간만큼 중력 낙하와 락 지연을 진행한다. */
    public void tick(int elapsedMs) {
        if (gameOver) return;
        if (countdownMs > 0) {   // 스프린트 시작 카운트다운: 끝날 때까지 시간과 중력이 멈춘다.
            countdownMs = Math.max(0, countdownMs - elapsedMs);
            return;
        }
        if (targetLines > 0) elapsedTimeMs += Math.max(0, elapsedMs);
        if (timeLimitMs > 0) {
            elapsedTimeMs += Math.min(Math.max(0, elapsedMs), timeLimitMs - elapsedTimeMs);
            if (elapsedTimeMs >= timeLimitMs) {
                setGameOver();
                return;
            }
        }
        if (!gravityEnabled) return;
        elapsedMs = Math.min(100, elapsedMs);

        if (spawnDelayed) {
            spawnDelayElapsed += elapsedMs;
            if (spawnDelayElapsed >= Settings.get().spawnDelayMs()) {
                spawnDelayed = false;
                spawnDelayElapsed = 0;
                spawnNext();
            }
            return;
        }

        if (startCal) {
            timer += elapsedMs / 1000.0f;
        }
        Settings settings = Settings.get();
        int gravity = settings.gravityMs(scoring.level);
        // SDF는 Down 키를 누르고 있는 동안 중력 간격을 줄여 빠르게 낙하시킴
        // GamePanel의 반복 입력과 중복으로 과도하게 빨라지지 않도록 별도 누적값으로 처리
        if (softDropHeld) gravity = settings.softDropGravityMs(scoring.level);
        gravityElapsed += elapsedMs;
        while (gravityElapsed >= gravity) {
            gravityElapsed -= gravity;
            if (!board.canPlace(active, x, y + 1, rotation)) break;
            y++; lastMoveWasRotation = false;
        }
        if (!board.canPlace(active, x, y + 1, rotation)) {
            lockElapsed += elapsedMs;
            if (lockElapsed >= settings.lockDelayMs(scoring.level)) lockPiece();
        } else lockElapsed = 0;
    }
    private int gravityElapsed;
    private boolean softDropHeld;

    public void setSoftDropHeld(boolean held) {
        if (softDropHeld != held) gravityElapsed = 0;
        softDropHeld = held;
    }

    private void lockPiece() {
        // 마지막 조작이 회전이고 T 블록의 세 모서리가 막힌 경우 T-spin 점수를 적용한다.
        boolean spin = active == Tetromino.Tmino && lastMoveWasRotation && isTSpin();
        boolean mini = spin && isMiniTSpin();
        Tetromino lockedType = active;
        boolean entirelyInHiddenRows = true;
        for (int[] cell : active.cells(rotation)) {
            if (y + cell[1] >= Board.HIDDEN_ROWS) {
                entirelyInHiddenRows = false;
                break;
            }
        }

        board.lock(active, x, y, rotation);
        for (GameListener listener : List.copyOf(listeners)) listener.onLock(lockedType);
        int cleared = board.clearLines();
        if (cleared > 0)
            for (GameListener listener : List.copyOf(listeners)) listener.onLinesCleared(cleared);
        if (entirelyInHiddenRows) { setGameOver(); return; }
        startCal = true; // pps 계산 시작
        dropCount++; // 드랍 수 + 1
        ScoreManager.Spin spinType = !spin ? ScoreManager.Spin.NONE
                : (mini ? ScoreManager.Spin.MINI : ScoreManager.Spin.FULL);
        scoring.onLock(cleared, spinType, cleared > 0 && board.isEmpty());
        if (targetLines > 0 && scoring.lines >= targetLines) { setGoalReached(); return; }
        int delay = Settings.get().spawnDelayMs();
        if(delay > 0) {
            active = null;
            spawnDelayed = true;
            spawnDelayElapsed = 0;
        } else {
            spawnNext();
        }
    }

    /** 목표 줄 수를 채워 끝낸다. 게임 오버와 달리 onGameOver 대신 onGoalReached가 호출된다. */
    private void setGoalReached() {
        if (gameOver) return;
        goalReached = true;
        gameOver = true;
        for (GameListener listener : List.copyOf(listeners)) listener.onGoalReached();
    }

    private void setGameOver() {
        if (gameOver) return;
        gameOver = true;
        for (GameListener listener : List.copyOf(listeners)) listener.onGameOver();
    }

    private boolean isTSpin() {
        int px = x + 1, py = y + 1, corners = 0;
        for (int[] d : new int[][]{{-1,-1},{1,-1},{-1,1},{1,1}})
            if (isCornerOccupied(px + d[0], py + d[1])) corners++;
        return corners >= 3;
    }
    private boolean isCornerOccupied(int cx, int cy) {
        return cx < 0 || cx >= Board.WIDTH || cy < Board.MIN_ROW || cy >= Board.HEIGHT
                || board.occupied(cx, cy);
    }
    private boolean isMiniTSpin() {
        if (lastKickIndex == 4) return false;
        int px = x + 1, py = y + 1, front = 0;
        int[][] corners = switch (rotation) {
            case 0 -> new int[][]{{-1,-1},{1,-1}};
            case 1 -> new int[][]{{1,-1},{1,1}};
            case 2 -> new int[][]{{-1,1},{1,1}};
            default -> new int[][]{{-1,-1},{-1,1}};
        };
        for (int[] d : corners) if (isCornerOccupied(px + d[0], py + d[1])) front++;
        return front < 2;
    }


    // 미노가 벽에 딱 붙어서 회전을 할 때, 기존에는 회전이 안 되지만 회전을 위한 함수
    private static int[][] kickTests(Tetromino type, int from, int to) {
        // 킥 표의 y 좌표는 화면 좌표계(아래쪽이 양수)에 맞춰 저장되어 있다.
        if (type == Tetromino.Imino) return switch (from + "-" + to) {
            case "0-1" -> kicks(0,0, -2,0, 1,0, -2,1, 1,-2);
            case "1-0" -> kicks(0,0, 2,0, -1,0, 2,-1, -1,2);
            case "1-2" -> kicks(0,0, -1,0, 2,0, -1,2, 2,-1);
            case "2-1" -> kicks(0,0, 1,0, -2,0, 1,-2, -2,1);
            case "2-3" -> kicks(0,0, 2,0, -1,0, 2,-1, -1,2);
            case "3-2" -> kicks(0,0, -2,0, 1,0, -2,1, 1,-2);
            case "3-0" -> kicks(0,0, 1,0, -2,0, 1,-2, -2,1);
            default -> kicks(0,0, -1,0, 2,0, -1,2, 2,-1);
        };
        return switch (from + "-" + to) {
            case "0-1" -> kicks(0,0, -1,0, -1,-1, 0,2, -1,2);
            case "1-0" -> kicks(0,0, 1,0, 1,1, 0,-2, 1,-2);
            case "1-2" -> kicks(0,0, 1,0, 1,1, 0,-2, 1,-2);
            case "2-1" -> kicks(0,0, -1,0, -1,-1, 0,2, -1,2);
            case "2-3" -> kicks(0,0, 1,0, 1,-1, 0,2, 1,2);
            case "3-2" -> kicks(0,0, -1,0, -1,1, 0,-2, -1,-2);
            case "3-0" -> kicks(0,0, -1,0, -1,1, 0,-2, -1,-2);
            default -> kicks(0,0, 1,0, 1,-1, 0,2, 1,2);
        };
    }

    // 실제로 벽에서 회전을 위한 부분
    private static int[][] kicks(int... values) {
        int[][] result = new int[values.length / 2][2];
        for (int i = 0; i < result.length; i++) { result[i][0] = values[i * 2]; result[i][1] = values[i * 2 + 1]; }
        return result;
    }
}
