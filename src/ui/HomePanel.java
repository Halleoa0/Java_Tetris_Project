package ui;

import app.ScreenManager;
import audio.AudioManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
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
    private static final BufferedImage LOGO = loadImage("Images/Logo.png");
    private static final BufferedImage CREDIT = loadImage("Images/Credit.png");
    private static final Font BUTTON_FONT = MENU_FONT.deriveFont(24f);

    private final List<TetrisBlockButton> buttons;
    private final MenuNavigation navigation;
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

        navigation = new MenuNavigation(this, buttons, () -> {});
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
                navigation.useMouse(pressedButton);
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

    private static BufferedImage loadImage(String path) {
        try {
            BufferedImage image = ImageIO.read(Path.of(path).toFile());
            if (image == null) throw new IOException("Unsupported image format");
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    private void updateHover(int x, int y) {
        TetrisBlockButton next = buttonAt(x, y);
        navigation.useMouse(next);
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
        g.drawImage(LOGO, (getWidth() - LOGO.getWidth()) / 2,
                135 - LOGO.getHeight() / 2, null);
        for (TetrisBlockButton button : buttons) button.draw(g, navigation.highlightedButton(hoveredButton) == button,
                pressedButton == button, BUTTON_FONT, 24);
        g.drawImage(CREDIT, 20, getHeight() - CREDIT.getHeight() - 20, null);
        g.dispose();
    }
}

