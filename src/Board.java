import java.util.Arrays;

final class Board {
    // 실제 저장 높이는 22칸이며 위쪽 2칸은 새 블록이 들어오는 숨김 영역이다.
    static final int WIDTH = 10, HEIGHT = 22, HIDDEN_ROWS = 2;
    // null은 빈칸, 값이 있으면 그 위치에 고정된 블록이 있다는 뜻이다.
    private final Tetromino[][] cells = new Tetromino[HEIGHT][WIDTH];

    /** 현재 보드에 블록을 놓을 수 있는지 검사한다. y가 음수인 숨김 바깥 좌표는 허용한다. */
    boolean canPlace(Tetromino type, int x, int y, int rotation) {
        for (int[] cell : type.cells(rotation)) {
            int px = x + cell[0], py = y + cell[1];
            if (px < 0 || px >= WIDTH || py >= HEIGHT) return false;
            if (py >= 0 && cells[py][px] != null) return false;
        }
        return true;
    }

    /** 움직임이 끝난 블록의 네 칸을 보드에 고정한다. */
    void lock(Tetromino type, int x, int y, int rotation) {
        for (int[] cell : type.cells(rotation)) {
            int px = x + cell[0], py = y + cell[1];
            if (py >= 0 && py < HEIGHT && px >= 0 && px < WIDTH) cells[py][px] = type;
        }
    }

    /** 가득 찬 행을 제거하고 위쪽 행을 내려, 제거한 행 수를 반환한다. */
    int clearLines() {
        int cleared = 0;
        for (int row = HEIGHT - 1; row >= 0; row--) {
            boolean full = true;
            for (Tetromino cell : cells[row]) if (cell == null) { full = false; break; }
            if (full) {
                cleared++;
                for (int y = row; y > 0; y--) cells[y] = Arrays.copyOf(cells[y - 1], WIDTH);
                cells[0] = new Tetromino[WIDTH];
                row++;
            }
        }
        return cleared;
    }

    boolean occupied(int x, int y) { return y >= 0 && y < HEIGHT && cells[y][x] != null; }
    Tetromino get(int x, int y) { return cells[y][x]; }
    /** 숨김 영역에 고정 블록이 남아 있으면 게임오버 조건이다. */
    boolean hasHiddenBlocks() {
        for (int y = 0; y < HIDDEN_ROWS; y++) for (Tetromino cell : cells[y]) if (cell != null) return true;
        return false;
    }
    /** 새 게임을 위해 모든 칸을 비운다. */
    void clear() { for (Tetromino[] row : cells) Arrays.fill(row, null); }
}
