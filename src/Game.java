import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

final class Game {

    /** 화면에 미리 표시할 다음 블록 수. */
    static final int PREVIEW_COUNT = 4;
    private static final int LOCK_DELAY_MS = 500, MAX_LOCK_RESETS = 15;
    final Board board = new Board();
    private final Random random = new Random();
    // bag은 7종 블록을 섞어 담고, queue는 다음 블록의 순서를 보관한다.
    private final Deque<Tetromino> bag = new ArrayDeque<>(), queue = new ArrayDeque<>();

    // 현재 블록과 Hold 블록의 상태. x, y는 블록의 기준 격자 원점이다.
    Tetromino active, held;
    int x, y, rotation;
    int score, lines, level = 1;

    int dropCount = 0; // 쌓은 블록 수
    float pps, timer = 0.0f; // Pieces Per Second - 초당 쌓은 블록
    boolean startCal = false;


    boolean gameOver, holdUsed, lastMoveWasRotation;

    private int lastKickIndex;
    private long lockElapsed;
    private int lockResets;

    Game() { restart(); }

    float calPPS() {
        pps = dropCount / timer;
        return pps;
    }

    /** 보드와 점수, 블록 대기열을 초기 상태로 되돌린다. */
    void restart() {
        dropCount = 0; startCal = false; timer = 0.0f;
        board.clear(); bag.clear(); queue.clear(); held = null;
        score = lines = 0; level = 1; gameOver = false; holdUsed = false;
        lockElapsed = 0; lockResets = 0; gravityElapsed = 0;
        for (int i = 0; i < PREVIEW_COUNT + 1; i++) queue.addLast(drawPiece());
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

    /** 화면에 보여줄 다음 블록 목록을 반환한다. */
    List<Tetromino> preview() { return new ArrayList<>(queue).subList(0, Math.min(PREVIEW_COUNT, queue.size())); }

    /// Next 미노를 생성
    private void spawnNext() {
        // 새 블록은 보드 위쪽 숨김 영역에서 시작한다.
        // Next를 최신화한다. (최근 미노 제거, 마지막미노 생성)
        active = queue.removeFirst(); queue.addLast(drawPiece());
        x = 3; y = -1; rotation = 0; holdUsed = false;
        lastMoveWasRotation = false; lockElapsed = 0; lockResets = 0; gravityElapsed = 0;
        if (!board.canPlace(active, x, y, rotation)) gameOver = true;
    }

    /** dx/dy만큼 이동한다. 충돌하면 false, 이동하면 true를 반환한다. */
    boolean move(int dx, int dy) {
        if (gameOver || !board.canPlace(active, x + dx, y + dy, rotation)) return false;
        x += dx; y += dy; lastMoveWasRotation = false;
        afterPlayerMove(); return true;
    }

    /** direction이 양수면 시계 방향, 음수면 반시계 방향으로 SRS 회전을 시도한다. */
    boolean rotate(int direction) {
        if (gameOver || active == Tetromino.Omino) return false;
        int from = rotation, to = (rotation + (direction > 0 ? 1 : 3)) & 3;
        // 회전 후 겹치면 SRS 킥 후보를 순서대로 적용해 옆이나 위로 이동을 시도한다.
        int[][] tests = kickTests(active, from, to);
        for (int i = 0; i < tests.length; i++) {
            int nx = x + tests[i][0], ny = y + tests[i][1];
            if (board.canPlace(active, nx, ny, to)) {
                x = nx; y = ny; rotation = to; lastMoveWasRotation = true; lastKickIndex = i;
                afterPlayerMove(); return true;
            }
        }
        return false;
    }


    /// 테트리스의 락 딜레이 시스템을 위한 변수. 락 딜레이는 블록이 바닥에 닿자마자 놓아지는 것이 아니라 약간의 유예시간을 주는 시스템
    private void afterPlayerMove() {
        if (board.canPlace(active, x, y + 1, rotation)) lockElapsed = 0;
        else if (lockResets < MAX_LOCK_RESETS) { lockElapsed = 0; lockResets++; }
    }

    /** 가능한 가장 아래까지 내린 뒤 고정한다. 낙하 거리에 따라 점수를 준다. */
    void hardDrop() {
        if (gameOver) return;
        int distance = 0;
        while (board.canPlace(active, x, y + 1, rotation)) { y++; distance++; }
        score += distance * 2;
        lockPiece();
    }

    /** 현재 블록을 Hold 칸과 바꾸며, 블록 하나당 한 번만 허용한다. */
    void hold() {
        if (gameOver || holdUsed) return;
        Tetromino current = active;
        if (held == null) { held = current; spawnNext(); }
        else {
            active = held; held = current; x = 3; y = -1; rotation = 0;
            lastMoveWasRotation = false; lockElapsed = 0; lockResets = 0;
            if (!board.canPlace(active, x, y, rotation)) gameOver = true;
        }
        holdUsed = true;
    }

    /** 타이머가 전달한 경과 시간만큼 중력 낙하와 락 지연을 진행한다. */
    void tick(int elapsedMs) {
        if (gameOver) return;

        if (startCal) {
            timer += elapsedMs / 1000.0f;
        }

        int gravity = Math.max(70, 800 - (level - 1) * 60);
        gravityElapsed += elapsedMs;
        while (gravityElapsed >= gravity) {
            gravityElapsed -= gravity;
            if (!board.canPlace(active, x, y + 1, rotation)) break;
            y++; lastMoveWasRotation = false;
        }
        if (!board.canPlace(active, x, y + 1, rotation)) {
            lockElapsed += elapsedMs;
            if (lockElapsed >= LOCK_DELAY_MS) lockPiece();
        } else lockElapsed = 0;
    }
    private int gravityElapsed;

    private void lockPiece() {
        // 마지막 조작이 회전이고 T 블록의 세 모서리가 막힌 경우 T-spin 점수를 적용한다.
        boolean spin = active == Tetromino.Tmino && lastMoveWasRotation && isTSpin();
        boolean mini = spin && isMiniTSpin();
        boolean entirelyInHiddenRows = true;
        for (int[] cell : active.cells(rotation)) {
            int cellY = y + cell[1];
            if (cellY < 0 || cellY >= Board.HIDDEN_ROWS) {
                entirelyInHiddenRows = false;
                break;
            }
        }

        board.lock(active, x, y, rotation);
        int cleared = board.clearLines();
        int[] normal = {0, 100, 300, 500, 800};
        int[] fullSpin = {400, 800, 1200, 1600};
        int[] miniSpin = {100, 200, 400};
        if (spin) score += (mini ? miniSpin[Math.min(cleared, miniSpin.length - 1)] : fullSpin[Math.min(cleared, fullSpin.length - 1)]) * level;
        else score += normal[Math.min(cleared, 4)] * level;
        if (cleared > 0) { lines += cleared; level = lines / 10 + 1; }
        if (entirelyInHiddenRows) { gameOver = true; return; }
        startCal = true; // pps 계산 시작
        dropCount++; // 드랍 수 + 1
        spawnNext();
    }

    private boolean isTSpin() {
        int px = x + 1, py = y + 1, corners = 0;
        for (int[] d : new int[][]{{-1,-1},{1,-1},{-1,1},{1,1}})
            if (isCornerOccupied(px + d[0], py + d[1])) corners++;
        return corners >= 3;
    }
    private boolean isCornerOccupied(int cx, int cy) {
        return cx < 0 || cx >= Board.WIDTH || cy >= Board.HEIGHT || (cy >= 0 && board.occupied(cx, cy));
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
