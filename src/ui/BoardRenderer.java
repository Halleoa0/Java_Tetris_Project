package ui;

import game.Board;
import game.Game;
import game.Tetromino;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;
import settings.Settings;

/** 테트리스 보드와 홀드/다음 블록 미리보기를 그리는 클래스. */
final class BoardRenderer {
    private static final int CELL = 25, BOARD_X = 517, BOARD_Y = 111;

    private final Game game;
    private final BufferedImage[] minoTiles;
    private final JPanel observer;

    /** 그릴 게임 상태, 미노 타일 이미지, 이미지 옵저버(패널)를 받아 렌더러를 만든다. */
    BoardRenderer(Game game, BufferedImage[] minoTiles, JPanel observer) {
        this.game = game;
        this.minoTiles = minoTiles;
        this.observer = observer;
    }

    /** 미리보기(홀드/다음 블록)와 보드를 순서대로 그린다. */
    void draw(Graphics2D g) {
        drawMatrixPreviews(g);
        drawBoard(g);
    }

    /** 테트로미노 종류를 미노 타일 시트(Mino.png)에서의 인덱스로 변환한다. (7번은 고스트 타일) */
    static int minoTileIndex(Tetromino type) {
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

    /** 보드에 고정된 블록(가비지 포함)을 그리고, 게임 중이면 고스트 블록과 현재 조작 중인 블록을 그린다. */
    private void drawBoard(Graphics2D g) {
        for (int row = Board.MIN_ROW; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                int sx = BOARD_X + col * CELL, sy = BOARD_Y + (row - Board.HIDDEN_ROWS) * CELL;
                Tetromino locked = game.board.get(col, row);
                if (locked != null) {
                    if (game.board.isGarbageCell(col, row)) drawGarbageCell(g, sx, sy);
                    else drawCell(g, sx, sy, locked, false);
                }
            }
        }
        if (!game.gameOver && game.active != null) {
            if (Settings.get().ghostPiece()) {
                int ghostY = game.y;
                while (game.board.canPlace(game.active, game.x, ghostY + 1, game.rotation)) ghostY++;
                drawPiece(g, game.active, game.x, ghostY, game.rotation, true);
            }
            drawPiece(g, game.active, game.x, game.y, game.rotation, false);
        }
    }

    /** 지정한 위치와 회전 상태로 블록 하나를 보드 위에 그린다. 숨겨진 행 밖의 칸은 건너뛴다. */
    private void drawPiece(Graphics2D g, Tetromino type, int x, int y, int rotation, boolean ghost) {
        for (int[] cell : type.cells(rotation)) {
            int bx = x + cell[0], by = y + cell[1];
            if (by < Board.MIN_ROW || by >= Board.HEIGHT) continue;
            int visibleY = by - Board.HIDDEN_ROWS;
            drawCell(g, BOARD_X + bx * CELL, BOARD_Y + visibleY * CELL, type, ghost);
        }
    }

    /** 보드 한 칸을 미노 타일 이미지로 그린다. 고스트이면 고스트 타일을 사용한다. */
    private void drawCell(Graphics2D g, int x, int y, Tetromino type, boolean ghost) {
        int tile = ghost ? 7 : minoTileIndex(type);
        g.drawImage(minoTiles[tile], x, y, CELL, CELL, observer);
    }

    /** 가비지 칸을 회색 사각형과 테두리로 그린다. */
    private void drawGarbageCell(Graphics2D g, int x, int y) {
        g.setColor(new Color(145, 145, 145));
        g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
        g.setColor(new Color(75, 75, 75));
        g.drawRect(x, y, CELL - 1, CELL - 1);
    }

    /** 미리보기용으로 원하는 크기의 미노 타일 한 칸을 그린다. */
    private void drawMiniCell(Graphics2D g, int x, int y, int size, Tetromino type) {
        g.drawImage(minoTiles[minoTileIndex(type)], x, y, size, size, observer);
    }

    /** 왼쪽에 홀드 블록, 오른쪽에 다음 블록 목록을 그린다. */
    private void drawMatrixPreviews(Graphics2D g) {
        if (game.held != null) drawPreviewPiece(g, game.held, 386, 112, 126, 78, 25);
        List<Tetromino> next = game.preview();
        for (int i = 0; i < next.size(); i++)
            drawPreviewPiece(g, next.get(i), 774, 120 + i * 68, 126, 50, 25);
    }

    /** 블록의 실제 크기를 계산해 주어진 영역(x, y, width, height)의 가운데에 맞춰 그린다. */
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
        for (int[] cell : cells) drawMiniCell(g, startX + cell[0] * unit, startY + cell[1] * unit, unit, type);
    }
}
