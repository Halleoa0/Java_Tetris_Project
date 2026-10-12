package shop;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** 미션 달성 같은 알림을 화면 위쪽에 잠깐 보여준다. GamePanel/BattlePanel이 하나씩 가진다. */
public final class ToastOverlay implements PlayerData.Toast {
    private static final long SHOW_MS = 3500;
    private static final int MAX_LINES = 3;

    private static final class Entry {
        final String text; final long expireAt;
        Entry(String text, long expireAt) { this.text = text; this.expireAt = expireAt; }
    }

    private final List<Entry> entries = new ArrayList<>();

    /** 이 화면이 열려 있는 동안 알림을 받는다. */
    public void attach() { PlayerData.get().addToastListener(this); }

    /** 화면을 닫을 때 반드시 호출. */
    public void detach() { PlayerData.get().removeToastListener(this); }

    @Override public synchronized void show(String message) {
        entries.add(new Entry(message, System.currentTimeMillis() + SHOW_MS));
    }

    public synchronized void draw(Graphics2D g, int panelWidth, Font font) {
        long now = System.currentTimeMillis();
        entries.removeIf(e -> e.expireAt <= now);
        int start = Math.max(0, entries.size() - MAX_LINES);
        g.setFont(font.deriveFont(Font.BOLD, 20f));
        FontMetrics fm = g.getFontMetrics();
        int y = 24;
        for (int i = start; i < entries.size(); i++) {
            Entry e = entries.get(i);
            long left = e.expireAt - now;
            float alpha = left < 500 ? left / 500f : 1f;
            int w = fm.stringWidth(e.text) + 40, h = 40, x = (panelWidth - w) / 2;
            g.setColor(new Color(20, 24, 36, (int) (230 * alpha)));
            g.fillRoundRect(x, y, w, h, 14, 14);
            g.setColor(new Color(255, 205, 60, (int) (255 * alpha)));
            g.drawRoundRect(x, y, w, h, 14, 14);
            g.setColor(new Color(255, 255, 255, (int) (255 * alpha)));
            g.drawString(e.text, x + 20, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            y += h + 8;
        }
    }
}
