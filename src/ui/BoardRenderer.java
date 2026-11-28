package ui;

import static ui.BoardLayout.BOARD_X;
import static ui.BoardLayout.BOARD_Y;
import static ui.BoardLayout.CELL;

import game.Board;
import game.Game;
import game.Tetromino;
import graphics.MinoSprites;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;
import settings.Settings;

/** 테트리스 보드와 홀드/다음 블록 미리보기를 그리는 클래스. 1인용 화면과 대전 화면이 함께 쓴다. */
public final class BoardRenderer {

    /** 방해 줄(가비지)을 그리는 모양. */
    public enum GarbageStyle {
        /** 회색 사각형과 테두리 (1인용) */
        GRAY_BOX,
        /** Mino.png의 고스트 타일 (대전) */
        GHOST_TILE
    }

    // 홀드 칸, NEXT 칸의 위치와 크기
    private static final int HOLD_X = 386, HOLD_Y = 112, HOLD_H = 78;
    private static final int NEXT_X = 774, NEXT_Y = 120, NEXT_H = 50, NEXT_SPACING = 68;
    private static final int PREVIEW_W = 126;

    private final BufferedImage[] minoTiles;
    private final ImageObserver observer;
    private final GarbageStyle garbageStyle;

    /** 미노 타일 이미지, 이미지 옵저버(패널), 방해 줄 모양을 받아 렌더러를 만든다. */
    public BoardRenderer(BufferedImage[] minoTiles, ImageObserver observer, GarbageStyle garbageStyle) {
        this.minoTiles = minoTiles;
        this.observer = observer;
        this.garbageStyle = garbageStyle;
    }

    /** 미리보기(홀드/다음 블록)와 보드를 순서대로 그린다. */
    public void draw(Graphics2D g, Game game) {
        drawMatrixPreviews(g, game);
        drawBoard(g, game);
    }

    /** 보드에 고정된 블록(가비지 포함)을 그리고, 게임 중이면 고스트 블록과 현재 조작 중인 블록을 그린다. */
    private void drawBoard(Graphics2D g, Game game) {
        for (int row = Board.MIN_ROW; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                Tetromino locked = game.board.get(col, row);
                if (locked == null) continue;
                int sx = BOARD_X + col * CELL, sy = BOARD_Y + (row - Board.HIDDEN_ROWS) * CELL;
                if (game.board.isGarbageCell(col, row)) drawGarbageCell(g, sx, sy);
                else drawTile(g, sx, sy, CELL, MinoSprites.indexOf(locked));
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

    /** 지정한 위치와 회전 상태로 블록 하나를 보드 위에 그린다. 숨겨진 행 밖의 칸은 건너뛴다. */
    private void drawPiece(Graphics2D g, Tetromino type, int x, int y, int rotation, boolean ghost) {
        int tile = ghost ? MinoSprites.GHOST_TILE : MinoSprites.indexOf(type);
        for (int[] cell : type.cells(rotation)) {
            int bx = x + cell[0], by = y + cell[1];
            if (by < Board.MIN_ROW || by >= Board.HEIGHT) continue;
            int visibleY = by - Board.HIDDEN_ROWS;
            drawTile(g, BOARD_X + bx * CELL, BOARD_Y + visibleY * CELL, CELL, tile);
        }
    }

    /** 타일 시트의 타일 한 칸을 원하는 크기로 그린다. */
    private void drawTile(Graphics2D g, int x, int y, int size, int tile) {
        g.drawImage(minoTiles[tile], x, y, size, size, observer);
    }

    /** 가비지 칸을 지정된 모양으로 그린다. */
    private void drawGarbageCell(Graphics2D g, int x, int y) {
        if (garbageStyle == GarbageStyle.GHOST_TILE) {
            drawTile(g, x, y, CELL, MinoSprites.GHOST_TILE);
            return;
        }
        g.setColor(new Color(145, 145, 145));
        g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
        g.setColor(new Color(75, 75, 75));
        g.drawRect(x, y, CELL - 1, CELL - 1);
    }

    /** 왼쪽에 홀드 블록, 오른쪽에 다음 블록 목록을 그린다. */
    private void drawMatrixPreviews(Graphics2D g, Game game) {
        if (game.held != null) drawPreviewPiece(g, game.held, HOLD_X, HOLD_Y, PREVIEW_W, HOLD_H);
        List<Tetromino> next = game.preview();
        for (int i = 0; i < next.size(); i++)
            drawPreviewPiece(g, next.get(i), NEXT_X, NEXT_Y + i * NEXT_SPACING, PREVIEW_W, NEXT_H);
    }

    /** 블록의 실제 크기를 계산해 주어진 영역(x, y, width, height)의 가운데에 맞춰 그린다. */
    private void drawPreviewPiece(Graphics2D g, Tetromino type, int x, int y, int width, int height) {
        int minX = 4, maxX = 0, minY = 4, maxY = 0;
        int[][] cells = type.cells(0);
        for (int[] cell : cells) {
            minX = Math.min(minX, cell[0]); maxX = Math.max(maxX, cell[0]);
            minY = Math.min(minY, cell[1]); maxY = Math.max(maxY, cell[1]);
        }
        int pieceWidth = (maxX - minX + 1) * CELL;
        int pieceHeight = (maxY - minY + 1) * CELL;
        int startX = x + (width - pieceWidth) / 2 - minX * CELL;
        int startY = y + (height - pieceHeight) / 2 - minY * CELL;
        for (int[] cell : cells)
            drawTile(g, startX + cell[0] * CELL, startY + cell[1] * CELL, CELL, MinoSprites.indexOf(type));
    }
}