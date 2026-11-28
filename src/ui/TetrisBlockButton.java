package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/** 테트로미노 모양 버튼의 모양(생김새 데이터?)과 렌더링 */
final class TetrisBlockButton {
    final String label;
    final String[] rows;
    final Color color;
    final int x, y, cellSize;
    final Runnable action;
    final boolean enabled;

    /** 라벨, 모양('#'이 칸인 문자열 배열), 색상, 위치, 칸 크기, 클릭 동작, 활성 여부로 버튼을 만든다. */
    TetrisBlockButton(String label, String[] rows, Color color, int x, int y,
                      int cellSize, Runnable action, boolean enabled) {
        this.label = label; this.rows = rows; this.color = color; this.x = x; this.y = y;
        this.cellSize = cellSize; this.action = action; this.enabled = enabled;
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
        int offset = pressed && hovered ? 2 : 0;
        Color base = enabled ? color : new Color(92, 92, 92);
        Color face = pressed && hovered ? scale(base, .45f) : hovered && enabled ? scale(base, .60f) : base;
        int widest = widestRow(), firstCell = rows[widest].indexOf('#'), count = occupiedCells(widest);
        g.setColor(face);
        for (int row = 0; row < rows.length; row++) for (int col = 0; col < rows[row].length(); col++)
            if (hasCell(row, col)) g.fillRect(x + col * cellSize + offset, y + row * cellSize + offset, cellSize, cellSize);
        g.setColor(mix(base, Color.WHITE, .28f)); g.setStroke(new BasicStroke(3));
        for (int row = 0; row < rows.length; row++) for (int col = 0; col < rows[row].length(); col++) {
            if (!hasCell(row, col)) continue;
            int left=x+col*cellSize+offset, top=y+row*cellSize+offset;
            if (!hasCell(row-1,col)) g.drawLine(left,top+1,left+cellSize,top+1);
            if (!hasCell(row+1,col)) g.drawLine(left,top+cellSize-1,left+cellSize,top+cellSize-1);
            if (!hasCell(row,col-1)) g.drawLine(left+1,top,left+1,top+cellSize);
            if (!hasCell(row,col+1)) g.drawLine(left+cellSize-1,top,left+cellSize-1,top+cellSize);
        }
        g.setColor(Color.BLACK); g.setFont(font.deriveFont((float)fontSize)); FontMetrics fm=g.getFontMetrics();
        int tx=x+firstCell*cellSize+(count*cellSize-fm.stringWidth(label))/2+offset;
        int ty=y+widest*cellSize+(cellSize-fm.getHeight())/2+fm.getAscent()+offset+("Play".equals(label)?35:0);
        g.drawString(label,tx,ty);
    }
    /** 색상의 RGB 값에 f를 곱해 밝기를 조절한 색을 반환한다. */
    private static Color scale(Color c,float f){return new Color(Math.round(c.getRed()*f),Math.round(c.getGreen()*f),Math.round(c.getBlue()*f));}
    /** 두 색상 a, b를 비율 f(0이면 a, 1이면 b)로 섞은 색을 반환한다. */
    private static Color mix(Color a,Color b,float f){return new Color(Math.round(a.getRed()*(1-f)+b.getRed()*f),Math.round(a.getGreen()*(1-f)+b.getGreen()*f),Math.round(a.getBlue()*(1-f)+b.getBlue()*f));}
}

