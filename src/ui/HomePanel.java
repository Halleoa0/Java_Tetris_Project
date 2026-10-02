package ui;

import app.ScreenManager;
import audio.AudioManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;



/** 테트로미노 버튼으로 화면 전환을 제공하는 메인 메뉴. */
public final class HomePanel extends JPanel {
    private static final int CELL_SIZE = 72;
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private static final Color I_COLOR = new Color(75, 205, 235);
    private static final Color O_COLOR = new Color(245, 210, 55);
    private static final Color T_COLOR = new Color(170, 85, 205);
    private static final Color L_COLOR = new Color(240, 145, 55);
    private static final Font MENU_FONT = MenuFonts.loadPressStart2P();
    private static final Font TITLE_FONT = MENU_FONT.deriveFont(40f);
    private static final Font BUTTON_FONT = MENU_FONT.deriveFont(24f);

    private final List<TetrisBlockButton> buttons;
    private TetrisBlockButton hoveredButton;
    private TetrisBlockButton pressedButton;

    public HomePanel(ScreenManager screens) {
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setOpaque(true);
        buttons = List.of(
                new TetrisBlockButton("Play", new String[]{"##", "##"}, O_COLOR, 380, 270, CELL_SIZE, screens::showModeSelect, true),
                new TetrisBlockButton("Tutorial", new String[]{"####"}, I_COLOR, 640, 285, CELL_SIZE, screens::startTutorial, true),
                new TetrisBlockButton("Shop", new String[]{"###", ".#."}, T_COLOR, 380, 475, CELL_SIZE, screens::showShop, true),
                new TetrisBlockButton("Settings", new String[]{"..#", "###"}, L_COLOR, 760, 425, CELL_SIZE, screens::showSettings, true));

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
                TetrisBlockButton released = buttonAt(event.getX(), event.getY());
                if (pressedButton != null && pressedButton == released) {
                    AudioManager.get().playMenuSelect(); // 메뉴 선택 효과음을 재생한다.
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
        TetrisBlockButton next = buttonAt(x, y);
        if (hoveredButton != next) {
            hoveredButton = next;
            repaint();
        }
    }

    private TetrisBlockButton buttonAt(int x, int y) {
        for (TetrisBlockButton button : buttons) if (button.contains(x, y)) return button;
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
        for (TetrisBlockButton button : buttons) button.draw(g, hoveredButton == button,
                pressedButton == button, BUTTON_FONT, 24);
        g.dispose();
    }
}

