import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.List;

/** Swing 화면, 키 입력, 주기적인 게임 업데이트를 연결한다.
 * 사용 예: IntelliJ에서 Main을 실행한 뒤 방향키로 이동하고 Space로 즉시 낙하한다.
 * 조작: ←/→ 이동, ↓ 소프트드롭, ↑ 또는 X 시계 회전, Z 반시계 회전,
 * Space 하드드롭, C Hold, R 재시작.
 */
final class GamePanel extends JPanel {
    private static final int CELL = 28, BOARD_X = 34, BOARD_Y = 30;
    private static final Color BACKGROUND = new Color(19, 23, 34);
    private static final Color PANEL = new Color(30, 36, 51);
    private final Game game = new Game();
    private long lastTick = System.nanoTime();

    // 키를 게임 동작에 연결하고 16ms 간격으로 게임 상태를 갱신한다.
    GamePanel() {
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setFocusable(true);
        bind("LEFT", "left", () -> game.move(-1, 0));
        bind("RIGHT", "right", () -> game.move(1, 0));
        bind("DOWN", "softDrop", () -> { if (game.move(0, 1)) game.score += 1; });
        bind("UP", "rotateCW", () -> game.rotate(1));
        bind("X", "rotateCWX", () -> game.rotate(1));
        bind("Z", "rotateCCW", () -> game.rotate(-1));
        bind("SPACE", "hardDrop", game::hardDrop);
        bind("C", "hold", game::hold);
        bind("R", "restart", game::restart);
        new Timer(16, e -> {
            long now = System.nanoTime();
            int elapsed = (int) Math.min(100, (now - lastTick) / 1_000_000L);
            lastTick = now;
            game.tick(elapsed);
            repaint();
        }).start();
    }


    // 게임 윈도우 창을 만듦
    void showWindow() {
        JFrame frame = new JFrame("Modern Tetris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(this);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        requestFocusInWindow();
    }

    private void bind(String key, String name, Runnable action) {
        // Swing의 Key Binding으로 키 입력을 게임 동작에 연결한다.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); repaint(); }
        });
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBoard(g);
        drawSidebar(g);
        if (game.gameOver) drawGameOver(g);
        g.dispose();
    }


    /// 이 아래의 코드는 디자인 변화 시 수정될 수 있는 코드입니다.
    /// 보드 및 미노(블록)을 png 파일로 대체하여 아래보다 코드가 간단해질 수 있습니다.


    // 보드와 쌓은 블록, 현재블록과 고스트를 그림
    private void drawBoard(Graphics2D g) {
        int boardW = Board.WIDTH * CELL, boardH = (Board.HEIGHT - Board.HIDDEN_ROWS) * CELL;
        g.setColor(PANEL);
        g.fillRoundRect(BOARD_X - 7, BOARD_Y - 7, boardW + 14, boardH + 14, 12, 12);
        for (int row = Board.HIDDEN_ROWS; row < Board.HEIGHT; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                int sx = BOARD_X + col * CELL, sy = BOARD_Y + (row - Board.HIDDEN_ROWS) * CELL;
                g.setColor(new Color(39, 46, 62));
                g.fillRect(sx, sy, CELL - 1, CELL - 1);
                Tetromino locked = game.board.get(col, row);
                if (locked != null) drawCell(g, sx, sy, locked.color, false);
            }
        }
        if (!game.gameOver) {
            // 현재 블록을 아래로 복사 이동해 예상 착지 위치(고스트)를 먼저 그린다.
            int ghostY = game.y;
            while (game.board.canPlace(game.active, game.x, ghostY + 1, game.rotation)) ghostY++;
            drawPiece(g, game.active, game.x, ghostY, game.rotation, true);
            drawPiece(g, game.active, game.x, game.y, game.rotation, false);
        }
    }


    // 미노 만들기
    private void drawPiece(Graphics2D g, Tetromino type, int x, int y, int rotation, boolean ghost) {
        for (int[] cell : type.cells(rotation)) {
            int bx = x + cell[0], by = y + cell[1];
            int visibleY = by - Board.HIDDEN_ROWS;
            if (visibleY < 0 || visibleY >= Board.HEIGHT - Board.HIDDEN_ROWS) continue;
            drawCell(g, BOARD_X + bx * CELL, BOARD_Y + visibleY * CELL, type.color, ghost);
        }
    }


    // 보드 뒤에 격자 만들기
    private void drawCell(Graphics2D g, int x, int y, Color color, boolean ghost) {
        if (ghost) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
            g.fillRoundRect(x + 3, y + 3, CELL - 7, CELL - 7, 5, 5);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 170));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x + 3, y + 3, CELL - 7, CELL - 7, 5, 5);
            return;
        }
        g.setColor(color.darker()); g.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 6, 6);
        g.setColor(color); g.fillRoundRect(x + 3, y + 3, CELL - 7, CELL - 7, 5, 5);
        g.setColor(new Color(255,255,255,85)); g.drawLine(x + 5, y + 5, x + CELL - 7, y + 5);
    }

    private void drawSidebar(Graphics2D g) {
        int x = 340;
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 23));
        g.drawString("TETRIS", x, 45);
        drawInfoBox(g, x, 64, 156, 74, "SCORE", String.valueOf(game.score));
        drawInfoBox(g, x, 148, 74, 66, "LEVEL", String.valueOf(game.level));
        drawInfoBox(g, x + 82, 148, 74, 66, "LINES", String.valueOf(game.lines));
        drawPreview(g, x, 226, "NEXT", game.preview(), false);
        drawPreview(g, x, 442, "HOLD", game.held == null ? List.of() : List.of(game.held), true);
        g.setColor(new Color(188, 196, 213)); g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("← → Move    ↓ Soft drop", x, 594);
        g.drawString("↑ / X Rotate    Z Reverse", x, 612);
        g.drawString("Space Drop    C Hold    R Restart", x, 630);
    }

    private void drawInfoBox(Graphics2D g, int x, int y, int w, int h, String label, String value) {
        g.setColor(PANEL); g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(164, 174, 195)); g.setFont(new Font("SansSerif", Font.BOLD, 11)); g.drawString(label, x + 12, y + 19);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 20)); g.drawString(value, x + 12, y + h - 14);
    }

    private void drawPreview(Graphics2D g, int x, int y, String title, List<Tetromino> pieces, boolean hold) {
        g.setColor(PANEL); g.fillRoundRect(x, y, 156, hold ? 120 : 204, 10, 10);
        g.setColor(new Color(164, 174, 195)); g.setFont(new Font("SansSerif", Font.BOLD, 11)); g.drawString(title, x + 12, y + 20);
        int slotH = hold ? 86 : 35;
        for (int i = 0; i < pieces.size(); i++) {
            Tetromino type = pieces.get(i);
            int minX=4, maxX=0, minY=4, maxY=0;
            for (int[] c : type.cells(0)) { minX=Math.min(minX,c[0]); maxX=Math.max(maxX,c[0]); minY=Math.min(minY,c[1]); maxY=Math.max(maxY,c[1]); }
            int unit = hold ? 18 : 14;
            int startX = x + (156 - (maxX-minX+1)*unit)/2;
            int startY = y + 27 + i*slotH + (slotH - (maxY-minY+1)*unit)/2;
            for (int[] c : type.cells(0)) drawMiniCell(g, startX + (c[0]-minX)*unit, startY + (c[1]-minY)*unit, unit, type.color);
        }
    }

    private void drawMiniCell(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(color.darker()); g.fillRoundRect(x, y, size - 2, size - 2, 4, 4);
        g.setColor(color); g.fillRoundRect(x + 2, y + 2, size - 5, size - 5, 3, 3);
    }

    private void drawGameOver(Graphics2D g) {
        int x = BOARD_X - 7, y = BOARD_Y + 230;
        g.setColor(new Color(12, 16, 25, 220)); g.fillRoundRect(x + 15, y, 250, 100, 12, 12);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics(); String text = "GAME OVER";
        g.drawString(text, x + (280 - fm.stringWidth(text))/2, y + 39);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        text = "Press R to restart"; fm = g.getFontMetrics();
        g.drawString(text, x + (280 - fm.stringWidth(text))/2, y + 68);
    }
}
