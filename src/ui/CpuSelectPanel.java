package ui;

import app.ScreenManager;
import audio.AudioManager;
import battle.BotBrain;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;

/// 모드 선택에서 vs CPU를 누르면 나오는 CPU 난이도 선택 화면. 난이도 버튼을 누르면 그 난이도로 대전을 시작한다.
public final class CpuSelectPanel extends JPanel {
    private static final int CELL_SIZE = 72;
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private static final Color S_COLOR = new Color(85, 210, 110);
    private static final Color O_COLOR = new Color(245, 210, 55);
    private static final Color L_COLOR = new Color(240, 145, 55);
    private static final Color Z_COLOR = new Color(235, 55, 65);
    private static final Font MENU_FONT = MenuFonts.loadPressStart2P();

    // 난이도 버튼 4개(EASY ~ MASTER) + BACK 버튼. 난이도 버튼은 BotBrain.Difficulty 순서와 같다.
    private final List<TetrisBlockButton> buttons = new ArrayList<>();
    private final MenuNavigation navigation;
    private TetrisBlockButton hoveredButton;
    private TetrisBlockButton pressedButton;

    public CpuSelectPanel(ScreenManager screens) {
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setOpaque(true);

        // 가로로 긴 I 모양 버튼을 화면 가운데에 세로로 쌓는다. (x = (1280 - 4칸) / 2)
        Color[] colors = {S_COLOR, O_COLOR, L_COLOR, Z_COLOR};
        BotBrain.Difficulty[] levels = BotBrain.Difficulty.values();
        for (int i = 0; i < levels.length; i++) {
            BotBrain.Difficulty difficulty = levels[i];
            buttons.add(new TetrisBlockButton(difficulty.label, new String[]{"####"}, colors[i % colors.length],
                    (1280 - 4 * CELL_SIZE) / 2, 195 + i * 90, CELL_SIZE, () -> screens.startBattle(difficulty), true));
        }
        // 모드 선택 화면과 같은 위치, 같은 모양의 BACK 버튼 (누르면 모드 선택으로 돌아감)
        buttons.add(new TetrisBlockButton("BACK", new String[]{"####"}, new Color(110, 190, 120), 35, 35, 28, screens::showModeSelect, true));

        navigation = new MenuNavigation(this, buttons, () -> {}, screens::showModeSelect);
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

        // 1. 제목 그리기 (모드 선택 화면의 SELECT MODE와 같은 글꼴, 같은 높이)
        g.setColor(Color.WHITE);
        g.setFont(MENU_FONT.deriveFont(34f));
        String title = "VS CPU";
        g.drawString(title, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2, 145);

        // 2. 버튼 그리기 (BACK 버튼만 글씨를 작게)
        TetrisBlockButton highlighted = navigation.highlightedButton(hoveredButton);
        for (TetrisBlockButton button : buttons)
            button.draw(g, highlighted == button, pressedButton == button, MENU_FONT, "BACK".equals(button.label) ? 14 : 24);

        // 3. 난이도 버튼 오른쪽에 PPS(1초에 놓는 블록 수)를 적는다.
        drawDifficultyNotes(g);
        g.dispose();
    }

    private void drawDifficultyNotes(Graphics2D g) {
        g.setFont(MENU_FONT.deriveFont(16f));
        g.setColor(new Color(170, 170, 170));
        BotBrain.Difficulty[] levels = BotBrain.Difficulty.values();
        for (int i = 0; i < levels.length; i++) {
            TetrisBlockButton button = buttons.get(i);
            String note = String.format(Locale.ROOT, "%.2f PPS", levels[i].pps);
            g.drawString(note, button.x + 4 * CELL_SIZE + 30, button.y + CELL_SIZE / 2 + 8);
        }
    }
}
