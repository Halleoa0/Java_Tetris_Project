package ui;

/** 게임 화면의 보드/매트릭스 좌표. 1인용 화면 기준이며, 대전 화면은 이 좌표로 그린 뒤 옆으로 옮긴다. */
public final class BoardLayout {
    /** 보드 한 칸의 크기(px). */
    public static final int CELL = 25;
    /** 보드(첫 번째 보이는 칸)의 왼쪽 위 좌표. */
    public static final int BOARD_X = 517, BOARD_Y = 111;
    /** Matrix.png를 그리는 위치와 크기. */
    public static final int MATRIX_X = 378, MATRIX_Y = 111, MATRIX_W = 528, MATRIX_H = 505;

    private BoardLayout() { }
}