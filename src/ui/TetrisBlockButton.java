package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/** 테트로미노 모양 버튼의 모양(생김새 데이터?)과 렌더링 */
final class TetrisBlockButton {
    private static final Color DISABLED_COLOR = new Color(92, 92, 92); // 비활성 버튼 색
    private static final int PRESSED_OFFSET = 2;   // 눌렸을 때 버튼이 아래·오른쪽으로 밀리는 거리(px)
    private static final int PLAY_LABEL_DROP = 35; // Play 버튼 글자를 아래로 내리는 거리(px)
    private static final Color BACK_COLOR = new Color(110, 190, 120); // BACK 버튼 색(초록)

    /** 버튼의 모양(색, '#'이 칸인 문자열 배열)을 묶은 객체. */
    static final class BlockShape {
        final Color color;
        final String[] rows;
        BlockShape(Color color, String... rows) { this.color = color; this.rows = rows; }
    }

    /** 버튼의 위치(왼쪽 위 좌표, 칸 크기)를 묶은 객체. */
    record Position(int x, int y, int cellSize) { }

    final String label;
    final String[] rows;
    final Color color;
    final int x, y, cellSize;
    final Runnable action;
    final boolean enabled;

    /** 라벨, 모양, 위치, 클릭 동작, 활성 여부로 버튼을 만든다. */
    TetrisBlockButton(String label, BlockShape shape, Position at, Runnable action, boolean enabled) {
        this.label = label; this.rows = shape.rows; this.color = shape.color;
        this.x = at.x(); this.y = at.y(); this.cellSize = at.cellSize();
        this.action = action; this.enabled = enabled;
    }

    /** 모드 선택, CPU 선택, 상점, 기록 화면에서 공통으로 쓰는 BACK 버튼을 만든다. */
    static TetrisBlockButton back(Runnable action) {
        return new TetrisBlockButton("BACK", new BlockShape(BACK_COLOR, "####"), new Position(35, 35, 28), action, true);
    }

    /** 좌표(px, py)가 버튼의 블록 칸 위에 있는지 확인한다. */
    boolean contains(int px, int py) {
        return hasCell(Math.floorDiv(py - y, cellSize), Math.floorDiv(px - x, cellSize));
    }
    /** 모양에서 (row, col) 위치가 채워진 칸(#)인지 확인한다. 범위 밖이면 false. */
    boolean hasCell(int row, int col) {
        return row >= 0 && row < rows.length && col >= 0 && col < rows[row].length()
                && rows[row].charAt(col) == '#';
    }
    /** 채워진 칸이 가장 많은 행의 번호를 반환한다. (라벨을 그릴 행) */
    private int widestRow() {
        int widest = 0;
        for (int row = 1; row < rows.length; row++)
            if (occupiedCells(row) > occupiedCells(widest)) widest = row;
        return widest;
    }
    /** 해당 행에서 채워진 칸의 개수를 센다. */
    private int occupiedCells(int row) {
        int count = 0;
        for (int col = 0; col < rows[row].length(); col++) if (rows[row].charAt(col) == '#') count++;
        return count;
    }
    /** 버튼을 그린다. hover/press 상태에 따라 색을 어둡게 하고, 외곽선을 그린 뒤 가장 넓은 행 가운데에 라벨을 적는다. */
    void draw(Graphics2D g, boolean hovered, boolean pressed, Font font, int fontSize) {
        boolean pressedDown = pressed && hovered;
        int offset = pressedDown ? PRESSED_OFFSET : 0;
        Color base = enabled ? color : DISABLED_COLOR;
        drawCells(g, faceColor(base, hovered, pressedDown), offset);
        drawOutline(g, mix(base, Color.WHITE, .28f), offset);
        drawLabel(g, font, fontSize, offset);
    }

    /** 칸을 칠할 색을 반환한다. 누른 상태면 더 어둡게, hover 상태면 조금 어둡게 한다. */
    private Color faceColor(Color base, boolean hovered, boolean pressedDown) {
        if (pressedDown) return scale(base, .45f);
        if (hovered && enabled) return scale(base, .60f);
        return base;
    }

    /** 모양에서 채워진 칸(#)을 모두 칠한다. */
    private void drawCells(Graphics2D g, Color face, int offset) {
        g.setColor(face);
        for (int row = 0; row < rows.length; row++) for (int col = 0; col < rows[row].length(); col++)
            if (hasCell(row, col)) g.fillRect(cellLeft(col, offset), cellTop(row, offset), cellSize, cellSize);
    }

    /** 블록 모양의 바깥쪽 변에만 밝은 외곽선을 그린다. */
    private void drawOutline(Graphics2D g, Color edge, int offset) {
        g.setColor(edge);
        g.setStroke(new BasicStroke(3));
        for (int row = 0; row < rows.length; row++) for (int col = 0; col < rows[row].length(); col++)
            if (hasCell(row, col)) drawOuterEdges(g, row, col, offset);
    }

    /** 칸 하나에서 이웃 칸이 없는 쪽(위, 아래, 왼쪽, 오른쪽)에 선을 긋는다. */
    private void drawOuterEdges(Graphics2D g, int row, int col, int offset) {
        int left = cellLeft(col, offset), top = cellTop(row, offset);
        if (!hasCell(row - 1, col)) g.drawLine(left, top + 1, left + cellSize, top + 1);
        if (!hasCell(row + 1, col)) g.drawLine(left, top + cellSize - 1, left + cellSize, top + cellSize - 1);
        if (!hasCell(row, col - 1)) g.drawLine(left + 1, top, left + 1, top + cellSize);
        if (!hasCell(row, col + 1)) g.drawLine(left + cellSize - 1, top, left + cellSize - 1, top + cellSize);
    }

    /** 가장 넓은 행 가운데에 라벨을 적는다. (Play 버튼은 2x2 O미노라 두 줄 사이로 35px 내린다) */
    private void drawLabel(Graphics2D g, Font font, int fontSize, int offset) {
        int widest = widestRow(), firstCell = rows[widest].indexOf('#'), count = occupiedCells(widest);
        g.setColor(Color.BLACK);
        g.setFont(font.deriveFont((float) fontSize));
        FontMetrics fm = g.getFontMetrics();
        int tx = cellLeft(firstCell, offset) + (count * cellSize - fm.stringWidth(label)) / 2;
        int ty = cellTop(widest, offset) + (cellSize - fm.getHeight()) / 2 + fm.getAscent()
                + ("Play".equals(label) ? PLAY_LABEL_DROP : 0);
        g.drawString(label, tx, ty);
    }

    /** col번째 칸의 왼쪽 x 좌표를 반환한다. */
    private int cellLeft(int col, int offset) { return x + col * cellSize + offset; }
    /** row번째 칸의 위쪽 y 좌표를 반환한다. */
    private int cellTop(int row, int offset) { return y + row * cellSize + offset; }
    /** 색상의 RGB 값에 f를 곱해 밝기를 조절한 색을 반환한다. */
    private static Color scale(Color c,float f){return new Color(Math.round(c.getRed()*f),Math.round(c.getGreen()*f),Math.round(c.getBlue()*f));}
    /** 두 색상 a, b를 비율 f(0이면 a, 1이면 b)로 섞은 색을 반환한다. */
    private static Color mix(Color a,Color b,float f){return new Color(Math.round(a.getRed()*(1-f)+b.getRed()*f),Math.round(a.getGreen()*(1-f)+b.getGreen()*f),Math.round(a.getBlue()*(1-f)+b.getBlue()*f));}
}

