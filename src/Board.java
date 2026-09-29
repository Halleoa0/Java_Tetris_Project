import java.util.Arrays;

final class Board {

    // 실제 저장 높이는 22칸이며 위쪽 2칸은 새 블록이 들어오는 숨김 영역
    static final int WIDTH = 10, HEIGHT = 22, HIDDEN_ROWS = 2;

    // 가로 10 x 세로 22, 테트리스 보드의 전체를 담는 변수 (예 : 천장에서 두 번째 칸 : cells[0][1] / 맨 아래에서 네 번째 칸 : cells[19][3])
    private final Tetromino[][] cells = new Tetromino[HEIGHT][WIDTH];

    /// 생성된 미노가 현재 보드에 놓일 수 있는지 검사
    /// type : 미노의 종류 (I미노, T미노 등) / x, y : 놓일 위치 / rotation : 0 ~ 3의 범위 => 각각 0도, 90도, 180도, 270도
    boolean canPlace(Tetromino type, int x, int y, int rotation) {
        // cell : 미노를 이루는 한 칸. 반복문을 미노의 한 칸들이 놓일 수 있는지 검사.
        for (int[] cell : type.cells(rotation)) {
            int px = x + cell[0], py = y + cell[1]; // px, py : 놓일 위치에서 상대 좌표 (예 : I미노는 놓일 위치에서 {0,1}, {1,1}, {2,1}, {3,1}를 검사)
            if (px < 0 || px >= WIDTH || py >= HEIGHT) return false; // px, py가 보드의 바깥에서 생성된다면 false
            if (py >= 0 && cells[py][px] != null) return false; // 미노가 생성될 위치에 블록이 있다면 false
        }
        return true; // 위의 false 조건들을 다 통과했다면 true, 즉 놓을 수 있음.
    }

    /// 미노를 보드에 놓았을 때, 미노에 정의된 상대 좌표를 이용하여 cells(테트리스 보드)에 기록
    /// type : 미노의 종류 (I미노, T미노 등) / x, y : 놓일 위치 / rotation : 0 ~ 3의 범위 => 각각 0도, 90도, 180도, 270도
    void lock(Tetromino type, int x, int y, int rotation) {
        for (int[] cell : type.cells(rotation)) {
            int px = x + cell[0], py = y + cell[1];
            if (py >= 0 && py < HEIGHT && px >= 0 && px < WIDTH) cells[py][px] = type;
        }
    }

    /// 미노로 꽉 채워진 한 줄을 지움. 추가로 지워진 줄의 윗줄들을 아래로 내림. (한 줄 사라졌으니까 내려야 됨)
    int clearLines() {
        int cleared = 0; // 지운 줄 개수. 테트리스라면 4
        for (int row = HEIGHT - 1; row >= 0; row--) { // row는 보드의 맨 아랫줄을 의미. 즉, 맨 아래에서 맨 윗줄까지 반복
            boolean full = true; // 한 줄 검사 boolean 타입 변수
            for (Tetromino cell : cells[row]) if (cell == null) { full = false; break; } // 한 줄이 다 안 찼다면 false
            if (full) { // 한 줄이 꽉 찼는가? 검사
                cleared++; // 지운 줄 + 1
                for (int y = row; y > 0; y--) cells[y] = Arrays.copyOf(cells[y - 1], WIDTH); // 윗줄이 아랫줄을 덮어 씌우는 식으로 줄 삭제
                cells[0] = new Tetromino[WIDTH]; // 맨 윗줄은 지움. (덮어 씌울 게 없어서)
                row++; // 그 다음 줄로
            }
        }
        return cleared; // 지운 라인 수
    }

    // x, y값에 블록이 존재하는지 확인
    boolean occupied(int x, int y) { return y >= 0 && y < HEIGHT && cells[y][x] != null; }
    Tetromino get(int x, int y) { return cells[y][x]; }

    // 보드가 완전히 비었는지 (퍼펙트 클리어 판정용)
    boolean isEmpty() {
        for (Tetromino[] row : cells ) for (Tetromino c : row) if (c != null) return false;
        return true;
    }

    // 보드에 있는 미노 다 지우기
    void clear() { for (Tetromino[] row : cells) Arrays.fill(row, null); }
}
