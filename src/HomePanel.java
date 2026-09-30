import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** 테트로미노 버튼으로 화면 전환을 제공하는 메인 메뉴. */
final class HomePanel extends JPanel {
    private static final int CELL_SIZE = 72;
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private static final Color I_COLOR = new Color(75, 205, 235);
    private static final Color O_COLOR = new Color(245, 210, 55);
    private static final Color T_COLOR = new Color(170, 85, 205);
    private static final Color L_COLOR = new Color(240, 145, 55);
    private static final Font MENU_FONT = MenuFonts.loadPressStart2P();
    private static final Font TITLE_FONT = MENU_FONT.deriveFont(40f);
    private static final Font BUTTON_FONT = MENU_FONT.deriveFont(24f);

    private final List<BlockButton> buttons;
    private BlockButton hoveredButton;
    private BlockButton pressedButton;

    HomePanel(ScreenManager screens) {
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setOpaque(true);
        buttons = List.of(
                new BlockButton("Play", new String[]{"##", "##"}, O_COLOR, 380, 270, screens::startGame),
                new BlockButton("Tutorial", new String[]{"####"}, I_COLOR, 640, 285, screens::startTutorial),
                new BlockButton("Shop", new String[]{"###", ".#."}, T_COLOR, 380, 475, screens::showShop),
                new BlockButton("Settings", new String[]{"..#", "###"}, L_COLOR, 760, 425, screens::showSettings));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { updateHover(event.getX(), event.getY()); }
            @Override public void mouseDragged(MouseEvent event) { updateHover(event.getX(), event.getY()); }
            @Override public void mouseExited(MouseEvent event) {
                hoveredButton = null;
                repaint();
            }
            @Override public void mousePressed(MouseEvent event) {
                pressedButton = buttonAt(event.getX(), event.getY());
                hoveredButton = pressedButton;
                repaint();
            }
            @Override public void mouseReleased(MouseEvent event) {
                BlockButton released = buttonAt(event.getX(), event.getY());
                if (pressedButton != null && pressedButton == released) {
                    AudioManager.get().playMenuSelect(); // 메인 메뉴 버튼 클릭 효과음
                    pressedButton.action.run();
                }
                pressedButton = null;
                updateHover(event.getX(), event.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void updateHover(int x, int y) {
        BlockButton next = buttonAt(x, y);
        if (hoveredButton != next) {
            hoveredButton = next;
            repaint();
        }
    }

    private BlockButton buttonAt(int x, int y) {
        for (BlockButton button : buttons) if (button.contains(x, y)) return button;
        return null;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        FontMetrics titleMetrics = g.getFontMetrics();
        String title = "TETRIS";
        g.drawString(title, (getWidth() - titleMetrics.stringWidth(title)) / 2, 155);
        for (BlockButton button : buttons) drawButton(g, button);
        g.dispose();
    }

    private void drawButton(Graphics2D g, BlockButton button) {
        boolean pressed = pressedButton == button && hoveredButton == button;
        int offset = pressed ? 2 : 0;
        Color face = pressed ? scale(button.color, 0.78f)
                : hoveredButton == button ? brighten(button.color) : button.color;
        int widestRow = button.widestRow();
        int firstCell = button.firstCellInRow(widestRow);
        int widestCells = button.occupiedCellsInRow(widestRow);

        g.setColor(face);
        for (int row = 0; row < button.rows.length; row++) {
            for (int col = 0; col < button.rows[row].length(); col++) {
                if (button.rows[row].charAt(col) == '#') {
                    g.fillRect(button.x + col * CELL_SIZE + offset,
                            button.y + row * CELL_SIZE + offset, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        g.setColor(brighten(button.color));
        g.setStroke(new BasicStroke(3));
        for (int row = 0; row < button.rows.length; row++) {
            for (int col = 0; col < button.rows[row].length(); col++) {
                if (button.rows[row].charAt(col) != '#') continue;
                int left = button.x + col * CELL_SIZE + offset;
                int top = button.y + row * CELL_SIZE + offset;
                if (!button.hasCell(row - 1, col)) g.drawLine(left, top + 1, left + CELL_SIZE, top + 1);
                if (!button.hasCell(row + 1, col)) g.drawLine(left, top + CELL_SIZE - 1, left + CELL_SIZE, top + CELL_SIZE - 1);
                if (!button.hasCell(row, col - 1)) g.drawLine(left + 1, top, left + 1, top + CELL_SIZE);
                if (!button.hasCell(row, col + 1)) g.drawLine(left + CELL_SIZE - 1, top, left + CELL_SIZE - 1, top + CELL_SIZE);
            }
        }

        g.setColor(Color.BLACK);
        g.setFont(BUTTON_FONT);
        FontMetrics metrics = g.getFontMetrics();
        String label = button.label;
        int textX = button.x + firstCell * CELL_SIZE
                + (widestCells * CELL_SIZE - metrics.stringWidth(label)) / 2 + offset;
        int textY = button.y + widestRow * CELL_SIZE
                + (CELL_SIZE - metrics.getHeight()) / 2 + metrics.getAscent() + offset
                + ("Play".equals(label) ? 35 : 0);
        g.drawString(label, textX, textY);
    }

    private Color brighten(Color color) { return mix(color, Color.WHITE, 0.28f); }

    private Color scale(Color color, float factor) {
        return new Color(Math.round(color.getRed() * factor), Math.round(color.getGreen() * factor),
                Math.round(color.getBlue() * factor));
    }

    private Color mix(Color first, Color second, float amount) {
        return new Color(Math.round(first.getRed() * (1 - amount) + second.getRed() * amount),
                Math.round(first.getGreen() * (1 - amount) + second.getGreen() * amount),
                Math.round(first.getBlue() * (1 - amount) + second.getBlue() * amount));
    }

    private static final class BlockButton {
        private final String label;
        private final String[] rows;
        private final Color color;
        private final int x;
        private final int y;
        private final Runnable action;

        private BlockButton(String label, String[] rows, Color color, int x, int y, Runnable action) {
            this.label = label;
            this.rows = rows;
            this.color = color;
            this.x = x;
            this.y = y;
            this.action = action;
        }

        private boolean contains(int x, int y) {
            int col = Math.floorDiv(x - this.x, CELL_SIZE);
            int row = Math.floorDiv(y - this.y, CELL_SIZE);
            return hasCell(row, col);
        }

        private boolean hasCell(int row, int col) {
            return row >= 0 && row < rows.length && col >= 0 && col < rows[row].length()
                    && rows[row].charAt(col) == '#';
        }

        private int widestRow() {
            int widest = 0;
            for (int row = 1; row < rows.length; row++) {
                if (occupiedCellsInRow(row) > occupiedCellsInRow(widest)) widest = row;
            }
            return widest;
        }

        private int firstCellInRow(int row) {
            return rows[row].indexOf('#');
        }

        private int occupiedCellsInRow(int row) {
            int count = 0;
            for (int col = 0; col < rows[row].length(); col++) {
                if (rows[row].charAt(col) == '#') count++;
            }
            return count;
        }
    }
}
