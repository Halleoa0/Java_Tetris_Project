package game;

import java.util.Arrays;

public final class Board {

    // 기존 좌표/바닥은 유지하고 위쪽 8줄(-8..-1)까지 저장한다.
    public static final int WIDTH = 10, HEIGHT = 22, HIDDEN_ROWS = 2;
    public static final int STORED_ROWS = 30, MIN_ROW = HEIGHT - STORED_ROWS;

    // 논리 좌표 y=-8..21을 배열의 행 0..29에 대응시킨다.
    private final Tetromino[][] cells = new Tetromino[STORED_ROWS][WIDTH];
    // 대전 모드용. 그 칸이 상대가 보낸 방해 줄(가비지)이면 true. 화면에서 회색으로 그리기 위해 기록함. (cells와 같은 크기)
    private final boolean[][] garbage = new boolean[STORED_ROWS][WIDTH];

    /// 생성된 미노가 현재 보드에 놓일 수 있는지 검사
    /// type : 미노의 종류 (I미노, T미노 등) / x, y : 놓일 위치 / rotation : 0 ~ 3의 범위 => 각각 0도, 90도, 180도, 270도
    public boolean canPlace(Tetromino type, int x, int y, int rotation) {
        // cell : 미노를 이루는 한 칸. 반복문을 미노의 한 칸들이 놓일 수 있는지 검사.
        for (int[] cell : type.cells(rotation)) {
            int px = x + cell[0], py = y + cell[1]; // px, py : 놓일 위치에서 상대 좌표 (예 : I미노는 놓일 위치에서 {0,1}, {1,1}, {2,1}, {3,1}를 검사)
            if (px < 0 || px >= WIDTH || py < MIN_ROW || py >= HEIGHT) return false;
            if (cells[py - MIN_ROW][px] != null) return false;
        }
        return true; // 위의 false 조건들을 다 통과했다면 true, 즉 놓을 수 있음.
    }

    /// 미노를 보드에 놓았을 때, 미노에 정의된 상대 좌표를 이용하여 cells(테트리스 보드)에 기록
    /// type : 미노의 종류 (I미노, T미노 등) / x, y : 놓일 위치 / rotation : 0 ~ 3의 범위 => 각각 0도, 90도, 180도, 270도
    public void lock(Tetromino type, int x, int y, int rotation) {
        for (int[] cell : type.cells(rotation)) {
            int px = x + cell[0], py = y + cell[1];
            if (py >= MIN_ROW && py < HEIGHT && px >= 0 && px < WIDTH) {
                cells[py - MIN_ROW][px] = type;
                garbage[py - MIN_ROW][px] = false; // 내가 놓은 블록이니까 가비지가 아님
            }
        }
    }

    /// 미노로 꽉 채워진 한 줄을 지움. 추가로 지워진 줄의 윗줄들을 아래로 내림. (한 줄 사라졌으니까 내려야 됨)
    public int clearLines() {
        int cleared = 0; // 지운 줄 개수. 테트리스라면 4
        for (int row = cells.length - 1; row >= 0; row--) { // 저장한 30줄 전체를 검사한다.
            boolean full = true; // 한 줄 검사 boolean 타입 변수
            for (Tetromino cell : cells[row]) if (cell == null) { full = false; break; } // 한 줄이 다 안 찼다면 false
            if (full) { // 한 줄이 꽉 찼는가? 검사
                cleared++; // 지운 줄 + 1
                for (int y = row; y > 0; y--) {
                    cells[y] = Arrays.copyOf(cells[y - 1], WIDTH); // 윗줄이 아랫줄을 덮어 씌우는 식으로 줄 삭제
                    garbage[y] = Arrays.copyOf(garbage[y - 1], WIDTH); // 가비지 기록도 같이 내림
                }
                cells[0] = new Tetromino[WIDTH]; // 맨 윗줄은 지움. (덮어 씌울 게 없어서)
                garbage[0] = new boolean[WIDTH];
                row++; // 그 다음 줄로
            }
        }
        return cleared; // 지운 라인 수
    }

    // x, y값에 블록이 존재하는지 확인
    public boolean occupied(int x, int y) { return y >= MIN_ROW && y < HEIGHT && cells[y - MIN_ROW][x] != null; }
    public Tetromino get(int x, int y) { return cells[y - MIN_ROW][x]; }

    // 보드가 완전히 비었는지 (퍼펙트 클리어 판정용)
    public boolean isEmpty() {
        for (Tetromino[] row : cells ) for (Tetromino c : row) if (c != null) return false;
        return true;
    }

    // 보드에 있는 미노 다 지우기
    public void clear() {
        for (Tetromino[] row : cells) Arrays.fill(row, null);
        for (boolean[] row : garbage) Arrays.fill(row, false);
    }


    /// ---------- 아래는 대전 모드(CPU 봇)에서 쓰는 기능 ----------
    /// cells 배열의 행 번호(0 ~ 29)를 그대로 쓰는 함수들이 많음. (행 0이 맨 위, 행 29가 맨 아래)


    // x, y값이 상대가 보낸 방해 줄(가비지)인지 확인
    public boolean isGarbage(int x, int y) { return y >= MIN_ROW && y < HEIGHT && garbage[y - MIN_ROW][x]; }

    /// 방해 줄을 맨 아래에 lines줄 넣음. 기존 블록들은 그만큼 위로 밀려 올라감.
    /// holeCol : 방해 줄에서 비어 있는 칸의 x 위치 (이 칸을 채우면 방해 줄을 지울 수 있음)
    public void insertGarbage(int lines, int holeCol) {
        // 1. 기존 줄들을 lines칸 위로 올림 (맨 위 lines줄은 밀려서 사라짐)
        for (int row = 0; row < STORED_ROWS - lines; row++) {
            cells[row] = Arrays.copyOf(cells[row + lines], WIDTH);
            garbage[row] = Arrays.copyOf(garbage[row + lines], WIDTH);
        }
        // 2. 비워진 맨 아래 lines줄을 방해 줄로 채움 (holeCol 한 칸만 비워둠)
        for (int row = STORED_ROWS - lines; row < STORED_ROWS; row++) {
            cells[row] = new Tetromino[WIDTH];
            garbage[row] = new boolean[WIDTH];
            for (int x = 0; x < WIDTH; x++) {
                if (x == holeCol) continue;
                cells[row][x] = Tetromino.Omino; // 아무 미노나 넣어도 되지만 화면에서는 회색으로 그려짐
                garbage[row][x] = true;
            }
        }
    }

    /// 지금 보드와 똑같은 새 보드를 만듦. CPU 봇이 "여기에 놓으면 어떨까?"를 미리 해볼 때 씀.
    /// (복사본에 블록을 놓아봐도 진짜 보드는 바뀌지 않음)
    public Board copy() {
        Board result = new Board();
        for (int row = 0; row < STORED_ROWS; row++) result.cells[row] = Arrays.copyOf(cells[row], WIDTH);
        return result;
    }

    /// x열의 높이. 맨 아래부터 그 열에서 가장 높은 블록까지의 칸 수 (빈 열이면 0)
    public int columnHeight(int x) {
        for (int row = 0; row < STORED_ROWS; row++) { // 위에서부터 내려오다가
            if (cells[row][x] != null) return STORED_ROWS - row; // 처음 만난 블록의 높이를 반환
        }
        return 0;
    }

    /// 모든 열의 높이를 더한 값. 클수록 블록이 높이 쌓였다는 뜻.
    public int totalHeight() {
        int sum = 0;
        for (int x = 0; x < WIDTH; x++) sum += columnHeight(x);
        return sum;
    }

    /// 구멍 개수. 구멍 = 위에 블록이 있어서 위에서 채울 수 없는 빈칸.
    public int holeCount() {
        int holes = 0;
        for (int x = 0; x < WIDTH; x++) {
            boolean blockAbove = false; // 지금 칸 위에 블록이 있었는지
            for (int row = 0; row < STORED_ROWS; row++) {
                if (cells[row][x] != null) blockAbove = true;
                else if (blockAbove) holes++; // 위에 블록이 있는데 여기는 비어 있으면 구멍
            }
        }
        return holes;
    }

    /// 가로 바뀜 횟수. 한 줄씩 왼쪽에서 오른쪽으로 보면서 "찬 칸 <-> 빈 칸"이 바뀌는 횟수를 모두 더한 값.
    /// 예) [■■■□□■■■■■] => 2번 바뀜 / [■□■□■□■■■■] => 6번 바뀜
    /// 숫자가 작을수록 줄이 깔끔하게 채워져 있다는 뜻. (양쪽 벽은 찬 칸으로 침)
    public int rowTransitions() {
        int count = 0;
        for (int row = 0; row < STORED_ROWS; row++) {
            boolean before = true; // 왼쪽 벽은 찬 칸
            for (int x = 0; x < WIDTH; x++) {
                boolean now = cells[row][x] != null;
                if (now != before) count++;
                before = now;
            }
            if (!before) count++; // 마지막 칸이 비어 있으면 오른쪽 벽과 바뀜
        }
        return count;
    }

    /// 세로 바뀜 횟수. 한 열씩 위에서 아래로 보면서 "찬 칸 <-> 빈 칸"이 바뀌는 횟수를 모두 더한 값.
    /// 블록 밑에 빈칸이 끼어 있으면 숫자가 커짐. (바닥은 찬 칸으로 침)
    public int columnTransitions() {
        int count = 0;
        for (int x = 0; x < WIDTH; x++) {
            boolean before = false; // 맨 위(천장 위)는 빈 칸
            for (int row = 0; row < STORED_ROWS; row++) {
                boolean now = cells[row][x] != null;
                if (now != before) count++;
                before = now;
            }
            if (!before) count++; // 맨 아래 칸이 비어 있으면 바닥과 바뀜
        }
        return count;
    }
}
