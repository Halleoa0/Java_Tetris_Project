package ui;

import app.ScreenManager;
import app.ScreenManager.BgmMode;
import audio.AudioManager;
import battle.BotBrain;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import settings.Records;
import settings.Settings;

/** 플레이어 별 게임 모드 최고 기록 표. 마우스 휠로 스크롤. */
public final class RecordsPanel extends JPanel {
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private static final Color ROW = new Color(30, 36, 51);
    private static final Color ROW_ME = new Color(52, 62, 92);
    private static final Color GOLD = new Color(255, 205, 60);

    private static final int TABLE_X = 90, HEADER_Y = 140, ROW_Y = 190, ROW_H = 62, VISIBLE_ROWS = 8;
    private static final String[] HEADERS = {"PLAYER", "CLASSIC", "ULTRA", "SPRINT 20L", "TOP-OUT TIME", "vs CPU 승리"};
    private static final int[] COL_X = {0, 210, 370, 530, 720, 890};
    private static final int TABLE_W = 1100;

    private final Font pixel = MenuFonts.loadPressStart2P();
    private final Font kr = MenuFonts.loadKRBlack();
    private final TetrisBlockButton backButton;
    private final List<String> players = Settings.get().profileNames();
    private final String current = Settings.get().currentProfile();
    private boolean backHover, backPressed;
    private int scroll; // 맨 위로 보이는 행 번호

    /** BACK 버튼과 ESC/방향키/마우스 휠 입력을 등록한다. */
    public RecordsPanel(ScreenManager screens) {
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setOpaque(true);
        setFocusable(true);
        backButton = TetrisBlockButton.back(() -> screens.showMenuScreen(BgmMode.HOME));

        bind("ESCAPE", () -> screens.showMenuScreen(BgmMode.HOME));
        bind("UP", () -> scrollBy(-1));
        bind("DOWN", () -> scrollBy(1));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover(e); }
            @Override public void mouseDragged(MouseEvent e) { hover(e); }
            @Override public void mouseExited(MouseEvent e) { backHover = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { backPressed = backButton.contains(e.getX(), e.getY()); repaint(); }
            @Override public void mouseReleased(MouseEvent e) {
                boolean back = backPressed && backButton.contains(e.getX(), e.getY());
                backPressed = false;
                if (back) { AudioManager.get().playMenuSelect(); screens.showMenuScreen(BgmMode.HOME); }
                repaint();
            }
            @Override public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) { scrollBy(e.getWheelRotation()); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    /** 마우스가 BACK 버튼 위에 있는지 확인하고, 바뀌었을 때만 다시 그린다. */
    private void hover(MouseEvent e) {
        boolean now = backButton.contains(e.getX(), e.getY());
        if (now != backHover) { backHover = now; repaint(); }
    }

    /** 표를 delta 행만큼 스크롤한다. 범위를 벗어나지 않도록 제한한다. */
    private void scrollBy(int delta) {
        int max = Math.max(0, players.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(max, scroll + delta));
        repaint();
    }

    /** 키 입력을 동작에 연결한다. 패널이 화면에 보일 때만 실행된다. */
    private void bind(String key, Runnable action) {
        String name = "records." + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) action.run(); }
        });
    }

    /** 제목, 표 헤더, 플레이어별 최고 기록 행(현재 사용자는 강조), 하단 안내 문구를 그린다. */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        backButton.draw(g, backHover, backPressed && backHover, pixel, 14);
        g.setColor(Color.WHITE);
        center(g, pixel.deriveFont(30f), "BEST RECORDS", 640, 75);

        // 헤더
        g.setColor(new Color(170, 175, 190));
        for (int c = 0; c < HEADERS.length; c++) {
            g.setFont(c == 5 ? kr.deriveFont(15f) : pixel.deriveFont(12f));
            g.drawString(HEADERS[c], TABLE_X + 20 + COL_X[c], HEADER_Y + 20);
        }

        Records rec = Records.get();
        BotBrain.Difficulty[] levels = BotBrain.Difficulty.values();
        int shown = Math.min(VISIBLE_ROWS, players.size() - scroll);
        for (int r = 0; r < shown; r++) {
            String name = players.get(scroll + r);
            int y = ROW_Y + r * (ROW_H + 6);
            boolean me = name.equals(current);
            g.setColor(me ? ROW_ME : ROW);
            g.fillRoundRect(TABLE_X, y, TABLE_W, ROW_H, 12, 12);

            g.setColor(me ? GOLD : Color.WHITE);
            g.setFont(kr.deriveFont(22f));
            g.drawString(shorten(g, name, 170), TABLE_X + 20 + COL_X[0], y + 40);

            g.setColor(Color.WHITE);
            cell(g, 1, y, score(rec.bestScore(name, "classic")));
            cell(g, 2, y, score(rec.bestScore(name, "ultra")));
            int ms = rec.bestTimeMs(name, Records.sprintKey(20));
            cell(g, 3, y, ms > 0 ? Records.formatTime(ms) : "-");
            int topOutMs = rec.bestTimeMs(name, "topout");
            cell(g, 4, y, topOutMs > 0 ? Records.formatTime(topOutMs) : "-");

            StringBuilder wins = new StringBuilder();
            for (BotBrain.Difficulty d : levels) {
                if (wins.length() > 0) wins.append("  ");
                wins.append(d.label.charAt(0)).append(' ').append(rec.cpuWins(name, d.name()));
            }
            g.setFont(pixel.deriveFont(13f));
            g.drawString(wins.toString(), TABLE_X + 20 + COL_X[5], y + 38);
        }

        g.setColor(new Color(170, 175, 190));
        center(g, kr.deriveFont(16f), "기록이 없으면 '-' · vs CPU 승리: E=EASY N=NORMAL H=HARD M=MASTER (매치 승리 수)"
                + (players.size() > VISIBLE_ROWS ? " · 마우스 휠로 스크롤" : ""), 640, 695);
        g.dispose();
    }

    /** 표의 col번째 열, rowY 행 위치에 값을 그린다. */
    private void cell(Graphics2D g, int col, int rowY, String text) {
        g.setFont(pixel.deriveFont(15f));
        g.drawString(text, TABLE_X + 20 + COL_X[col], rowY + 38);
    }

    /** 점수를 천 단위 쉼표 형식으로 바꾼다. 기록이 없으면 "-". */
    private static String score(int v) { return v > 0 ? String.format("%,d", v) : "-"; }

    /** 문자열이 maxW 픽셀보다 길면 뒤를 잘라내고 "…"을 붙인다. */
    private static String shorten(Graphics2D g, String s, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** 지정한 폰트로 문자열을 가운데 정렬해서 그린다. */
    private static void center(Graphics2D g, Font f, String text, int cx, int baseline) {
        g.setFont(f);
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
