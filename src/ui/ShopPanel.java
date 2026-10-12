package ui;

import app.ScreenManager;
import audio.AudioManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import shop.Mission;
import shop.PlayerData;
import shop.ShopItem;
import shop.ThemeManager;

/** 상점 화면: 위클리 미션 / 미노 스킨 / 배경화면 세 탭. 마우스로 탭 이동, 구매, 장착을 한다. (ESC = 뒤로가기) */
public final class ShopPanel extends JPanel {
    private static final Color BACKGROUND = new Color(41, 41, 41);
    private static final Color CARD = new Color(30, 36, 51);
    private static final Color CARD_HOVER = new Color(42, 50, 72);
    private static final Color GOLD = new Color(255, 205, 60);
    private static final Color GREEN = new Color(85, 210, 110);
    private static final Color GRAY = new Color(100, 104, 116);

    private enum Tab {
        MISSION("위클리 미션"), MINO("미노 스킨"), BACKGROUND("배경화면");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private static final int LEFT = 140, TOP = 195, CARD_W = 310, CARD_H = 235, GAP_X = 35, GAP_Y = 20;

    private final ScreenManager screens;
    private final PlayerData data = PlayerData.get();
    private final Font pixel = MenuFonts.loadPressStart2P();
    private final Font kr = MenuFonts.loadKRBlack();
    private final TetrisBlockButton backButton;
    private final Map<ShopItem, BufferedImage[]> previewCache = new EnumMap<>(ShopItem.class);

    private Tab tab = Tab.MISSION;
    private boolean backHover, backPressed;
    private int hoverTab = -1, hoverCard = -1;
    private String message = "";
    private Color messageColor = Color.WHITE;
    private final Timer messageTimer = new Timer(2500, e -> { message = ""; repaint(); });

    public ShopPanel(ScreenManager screens) {
        this.screens = screens;
        setPreferredSize(new Dimension(1280, 720));
        setBackground(BACKGROUND);
        setOpaque(true);
        setFocusable(true);
        messageTimer.setRepeats(false);
        backButton = new TetrisBlockButton("BACK", new String[]{"####"}, new Color(110, 190, 120), 35, 35, 28, this::goBack, true);

        bind("ESCAPE", this::goBack);
        bind("LEFT", () -> switchTab(-1));
        bind("RIGHT", () -> switchTab(1));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(e.getPoint()); }
            @Override public void mouseDragged(MouseEvent e) { updateHover(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e) { backHover = false; hoverTab = hoverCard = -1; repaint(); }
            @Override public void mousePressed(MouseEvent e) {
                backPressed = backButton.contains(e.getX(), e.getY());
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                boolean back = backPressed && backButton.contains(e.getX(), e.getY());
                backPressed = false;
                if (back) { AudioManager.get().playMenuSelect(); goBack(); return; }
                handleClick(e.getPoint());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void bind(String key, Runnable action) {
        String name = "shop." + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) action.run(); }
        });
    }

    private void goBack() { messageTimer.stop(); screens.showHome(); }

    private void switchTab(int delta) {
        Tab[] tabs = Tab.values();
        tab = tabs[(tab.ordinal() + delta + tabs.length) % tabs.length];
        hoverCard = -1;
        repaint();
    }

    // ===== 좌표 =====

    private Rectangle tabRect(int i) { return new Rectangle(LEFT + i * 216, 115, 200, 46); }
    private Rectangle cardRect(int i) {
        return new Rectangle(LEFT + (i % 3) * (CARD_W + GAP_X), TOP + (i / 3) * (CARD_H + GAP_Y), CARD_W, CARD_H);
    }
    private Rectangle buyRect(int i) {
        Rectangle c = cardRect(i);
        return new Rectangle(c.x + 15, c.y + c.height - 55, c.width - 30, 40);
    }

    private List<ShopItem> itemsOfTab() {
        List<ShopItem> list = new ArrayList<>();
        ShopItem.Type type = tab == Tab.MINO ? ShopItem.Type.MINO : ShopItem.Type.BACKGROUND;
        for (ShopItem it : ShopItem.values()) if (it.type == type) list.add(it);
        return list;
    }

    // ===== 입력 =====

    private void updateHover(Point p) {
        boolean newBack = backButton.contains(p.x, p.y);
        int newTab = -1, newCard = -1;
        for (int i = 0; i < Tab.values().length; i++) if (tabRect(i).contains(p)) newTab = i;
        if (tab != Tab.MISSION) {
            int n = itemsOfTab().size();
            for (int i = 0; i < n; i++) if (cardRect(i).contains(p)) newCard = i;
        }
        if (newBack != backHover || newTab != hoverTab || newCard != hoverCard) {
            backHover = newBack; hoverTab = newTab; hoverCard = newCard;
            repaint();
        }
    }

    private void handleClick(Point p) {
        for (int i = 0; i < Tab.values().length; i++) {
            if (tabRect(i).contains(p)) {
                tab = Tab.values()[i];
                hoverCard = -1;
                AudioManager.get().playMenuSelect();
                repaint();
                return;
            }
        }
        if (tab == Tab.MISSION) return;
        List<ShopItem> items = itemsOfTab();
        for (int i = 0; i < items.size(); i++) {
            if (!buyRect(i).contains(p)) continue;
            ShopItem item = items.get(i);
            if (data.equipped(item.type) == item) return;               // 이미 장착 중
            if (data.owns(item)) {
                data.equip(item);
                notice(item.label + " 장착!", GREEN);
            } else if (data.buy(item)) {
                data.equip(item);                                       // 사자마자 바로 장착
                notice(item.label + " 구매 및 장착 완료!", GOLD);
            } else {
                notice("코인이 부족합니다. (필요 " + item.price + ", 보유 " + data.coins() + ")", new Color(235, 90, 90));
                repaint();
                return;
            }
            AudioManager.get().playMenuSelect();
            repaint();
            return;
        }
    }

    private void notice(String text, Color color) {
        message = text;
        messageColor = color;
        messageTimer.restart();
    }

    // ===== 그리기 =====

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        backButton.draw(g, backHover, backPressed && backHover, pixel, 14);

        g.setFont(pixel.deriveFont(30f));
        g.setColor(Color.WHITE);
        g.drawString("SHOP", 640 - g.getFontMetrics().stringWidth("SHOP") / 2, 75);

        // 코인
        String coin = "COIN  " + data.coins();
        g.setFont(pixel.deriveFont(20f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(GOLD);
        g.drawString(coin, 1245 - fm.stringWidth(coin), 70);
        g.setColor(GOLD);
        g.fillOval(1245 - fm.stringWidth(coin) - 34, 50, 24, 24);
        g.setColor(new Color(150, 110, 20));
        g.drawOval(1245 - fm.stringWidth(coin) - 34, 50, 24, 24);

        // 탭
        for (int i = 0; i < Tab.values().length; i++) {
            Rectangle r = tabRect(i);
            boolean on = tab == Tab.values()[i];
            g.setColor(on ? new Color(245, 210, 55) : hoverTab == i ? CARD_HOVER : CARD);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g.setColor(on ? Color.BLACK : Color.WHITE);
            centerText(g, kr.deriveFont(20f), Tab.values()[i].label, r.x + r.width / 2, r.y + r.height / 2);
        }

        if (tab == Tab.MISSION) drawMissions(g); else drawItems(g);

        if (!message.isEmpty()) {
            g.setColor(messageColor);
            centerText(g, kr.deriveFont(20f), message, 640, 700);
        }
        g.dispose();
    }

    private void drawMissions(Graphics2D g) {
        Map<Mission, int[]> status = data.weeklyStatus();
        int i = 0;
        for (Map.Entry<Mission, int[]> e : status.entrySet()) {
            Mission m = e.getKey();
            int progress = e.getValue()[0];
            boolean done = e.getValue()[1] == 1;
            int x = LEFT, y = TOP + i * 140, w = 1000, h = 120;
            g.setColor(CARD);
            g.fillRoundRect(x, y, w, h, 16, 16);
            if (done) { g.setColor(GREEN); g.setStroke(new BasicStroke(2)); g.drawRoundRect(x, y, w, h, 16, 16); }

            g.setColor(Color.WHITE);
            g.setFont(kr.deriveFont(26f));
            g.drawString(m.label, x + 30, y + 45);

            // 진행 막대
            int bx = x + 30, by = y + 70, bw = 600, bh = 24;
            g.setColor(new Color(60, 66, 84));
            g.fillRoundRect(bx, by, bw, bh, 12, 12);
            g.setColor(done ? GREEN : new Color(245, 210, 55));
            int fill = (int) (bw * (progress / (double) m.target));
            if (fill > 0) g.fillRoundRect(bx, by, Math.max(fill, 12), bh, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(pixel.deriveFont(14f));
            g.drawString(progress + " / " + m.target, bx + bw + 20, by + 19);

            // 보상
            g.setColor(done ? GREEN : GOLD);
            centerText(g, kr.deriveFont(22f), done ? "완료!" : "+" + Mission.REWARD_COINS + " 코인", x + w - 110, y + h / 2);
            i++;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)));
        g.setColor(new Color(170, 175, 190));
        centerText(g, kr.deriveFont(18f), "미션은 매주 월요일에 새로 바뀝니다 (" + days + "일 남음) · 미션 하나를 달성할 때마다 "
                + Mission.REWARD_COINS + "코인", 640, 640);
    }

    private void drawItems(Graphics2D g) {
        List<ShopItem> items = itemsOfTab();
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            Rectangle c = cardRect(i);
            g.setColor(hoverCard == i ? CARD_HOVER : CARD);
            g.fillRoundRect(c.x, c.y, c.width, c.height, 16, 16);

            // 미리보기
            int px = c.x + 15, py = c.y + 15, pw = c.width - 30, ph = 105;
            if (item.type == ShopItem.Type.BACKGROUND) {
                ThemeManager.paintPreview(g, item, px, py, pw, ph);
            } else {
                g.setColor(new Color(41, 41, 41));
                g.fillRect(px, py, pw, ph);
                BufferedImage[] tiles = previewCache.computeIfAbsent(item, ThemeManager::previewTiles);
                int size = 34, total = 7 * size + 6 * 4, sx = px + (pw - total) / 2, sy = py + (ph - size) / 2;
                for (int t = 0; t < 7; t++) g.drawImage(tiles[t], sx + t * (size + 4), sy, size, size, null);
            }

            g.setColor(Color.WHITE);
            centerText(g, kr.deriveFont(20f), item.label, c.x + c.width / 2, c.y + 140);

            // 구매 / 장착 버튼
            Rectangle b = buyRect(i);
            boolean equipped = data.equipped(item.type) == item;
            boolean owned = data.owns(item);
            String text;
            Color face, fg = Color.BLACK;
            if (equipped) { text = "장착 중"; face = GRAY; fg = Color.WHITE; }
            else if (owned) { text = "장착하기"; face = GREEN; }
            else if (data.coins() >= item.price) { text = "구매  " + item.price + " 코인"; face = GOLD; }
            else { text = "구매  " + item.price + " 코인"; face = new Color(70, 74, 88); fg = new Color(170, 170, 170); }
            g.setColor(face);
            g.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);
            g.setColor(fg);
            centerText(g, kr.deriveFont(18f), text, b.x + b.width / 2, b.y + b.height / 2);
        }
    }

    private static void centerText(Graphics2D g, Font font, String text, int cx, int cy) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, cy - fm.getHeight() / 2 + fm.getAscent());
    }
}
