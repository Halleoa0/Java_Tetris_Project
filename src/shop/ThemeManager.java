package shop;

import graphics.MinoSprites;
import graphics.ScreenStyle;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import settings.Settings;

/** 장착한 테마(미노 스킨, 배경화면)를 게임 화면에 적용한다. */
public final class ThemeManager {
    private static final int W = ScreenStyle.WIDTH, H = ScreenStyle.HEIGHT;
    private static final Map<ShopItem, BufferedImage> BG_CACHE = new EnumMap<>(ShopItem.class);
    /** 배경별로 그리는 메서드를 모아 둔 표. 표에 없는 배경은 paintDefault로 그린다. */
    private static final Map<ShopItem, BiConsumer<Graphics2D, Random>> PAINTERS = new EnumMap<>(ShopItem.class);
    static {
        PAINTERS.put(ShopItem.BG_NIGHT, ThemeManager::paintNight);
        PAINTERS.put(ShopItem.BG_SUNSET, ThemeManager::paintSunset);
        PAINTERS.put(ShopItem.BG_GRID, (g, rnd) -> paintGrid(g));      // 난수를 쓰지 않는 배경
        PAINTERS.put(ShopItem.BG_OCEAN, ThemeManager::paintOcean);
        PAINTERS.put(ShopItem.BG_CHECKER, (g, rnd) -> paintChecker(g)); // 난수를 쓰지 않는 배경
    }

    private ThemeManager() { }

    /** 장착한 미노 스킨을 타일(0~6번)에 입힌다. 색각 보정 모드가 켜져 있으면 그쪽이 우선이라 건드리지 않는다. */
    public static void applyMinoSkin(BufferedImage[] tiles) {
        if (Settings.get().colorMode() != Settings.ColorMode.OFF) return;
        applyPalette(tiles, PlayerData.get().equipped(ShopItem.Type.MINO));
    }

    /** 상점 미리보기용: 지정한 스킨이 입혀진 타일 7개(Z, L, O, S, I, J, T 순서). */
    public static BufferedImage[] previewTiles(ShopItem skin) {
        BufferedImage[] tiles = Arrays.copyOf(MinoSprites.loadTiles(), MinoSprites.GHOST_TILE); // 고스트 타일은 제외
        applyPalette(tiles, skin);
        return tiles;
    }

    /** 스킨 팔레트를 타일에 입힌다. 팔레트 순서(Z, L, O, S, I, J, T)는 타일 번호 순서와 같다. */
    private static void applyPalette(BufferedImage[] tiles, ShopItem skin) {
        if (skin.palette == null) return;
        for (int i = 0; i < skin.palette.length; i++) tiles[i] = MinoSprites.tint(tiles[i], skin.palette[i]);
    }

    /** 장착한 배경화면을 화면 전체에 그린다. 패널 크기에 맞춰 늘려 그린다. */
    public static void paintBackground(Graphics2D g, int width, int height) {
        ShopItem bg = PlayerData.get().equipped(ShopItem.Type.BACKGROUND);
        if (bg == ShopItem.BG_DEFAULT) return;   // 기본 배경은 패널 배경색 그대로
        g.drawImage(background(bg), 0, 0, width, height, null);
    }

    /** 상점 미리보기용: 지정한 영역에 배경을 그린다. */
    public static void paintPreview(Graphics2D g, ShopItem bg, int x, int y, int w, int h) {
        if (bg == ShopItem.BG_DEFAULT) {
            g.setColor(ScreenStyle.BACKGROUND);
            g.fillRect(x, y, w, h);
        } else {
            g.drawImage(background(bg), x, y, w, h, null);
        }
    }

    private static synchronized BufferedImage background(ShopItem bg) {
        return BG_CACHE.computeIfAbsent(bg, ThemeManager::render);
    }

    /** 배경 하나를 화면 크기의 이미지로 그린다. 그리는 방법은 PAINTERS 표에서 찾는다. */
    private static BufferedImage render(ShopItem bg) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 배경마다 시드가 고정된 난수를 쓰므로 매번 같은 그림이 나온다.
        Random rnd = new Random(bg.ordinal() * 31L + 7);
        BiConsumer<Graphics2D, Random> painter = PAINTERS.getOrDefault(bg, (canvas, unused) -> paintDefault(canvas));
        painter.accept(g, rnd);
        g.dispose();
        return img;
    }

    /** 화면 전체를 위(top)에서 아래(bottom)로 바뀌는 세로 그라데이션으로 채운다. */
    private static void fillVerticalGradient(Graphics2D g, Color top, Color bottom) {
        g.setPaint(new GradientPaint(0, 0, top, 0, H, bottom));
        g.fillRect(0, 0, W, H);
    }

    /** 별이 빛나는 밤: 남색 그라데이션 + 별 160개 + 초승달. */
    private static void paintNight(Graphics2D g, Random rnd) {
        fillVerticalGradient(g, new Color(8, 10, 32), new Color(32, 24, 70));
        for (int i = 0; i < 160; i++) {
            int a = 90 + rnd.nextInt(165), s = rnd.nextInt(10) == 0 ? 3 : 2;
            g.setColor(new Color(255, 255, 255, a));
            g.fillOval(rnd.nextInt(W), rnd.nextInt(H), s, s);
        }
        g.setColor(new Color(245, 240, 210));
        g.fillOval(1060, 70, 70, 70);
        g.setColor(new Color(24, 20, 60));
        g.fillOval(1082, 60, 66, 66);
    }

    /** 노을: 보라-주황 그라데이션 + 해 + 건물 실루엣 24개. */
    private static void paintSunset(Graphics2D g, Random rnd) {
        fillVerticalGradient(g, new Color(40, 20, 80), new Color(255, 120, 60));
        g.setColor(new Color(255, 210, 120, 230));
        g.fillOval(W / 2 - 110, H - 230, 220, 220);
        g.setColor(new Color(60, 25, 55));
        g.fillRect(0, H - 90, W, 90);
        for (int i = 0; i < 24; i++) {
            int bw = 30 + rnd.nextInt(50), bh = 30 + rnd.nextInt(110), bx = i * 56 + rnd.nextInt(14);
            g.setColor(new Color(45, 18, 45));
            g.fillRect(bx, H - 90 - bh, bw, bh);
        }
    }

    /** 사이버 그리드: 어두운 그라데이션 + 40px 청록 격자 + 200px 분홍 격자. */
    private static void paintGrid(Graphics2D g) {
        fillVerticalGradient(g, new Color(8, 6, 24), new Color(35, 8, 60));
        drawGridLines(g, 40, new Color(0, 230, 255, 70));
        drawGridLines(g, 200, new Color(255, 40, 200, 60));
    }

    /** spacing 간격의 가로/세로 격자선을 그린다. */
    private static void drawGridLines(Graphics2D g, int spacing, Color color) {
        g.setColor(color);
        for (int x = 0; x <= W; x += spacing) g.drawLine(x, 0, x, H);
        for (int y = 0; y <= H; y += spacing) g.drawLine(0, y, W, y);
    }

    /** 깊은 바다: 파란 그라데이션 + 물방울 70개 + 빛줄기 6개. */
    private static void paintOcean(Graphics2D g, Random rnd) {
        fillVerticalGradient(g, new Color(10, 70, 120), new Color(2, 12, 40));
        for (int i = 0; i < 70; i++) {
            int s = 4 + rnd.nextInt(18), x = rnd.nextInt(W), y = rnd.nextInt(H);
            g.setColor(new Color(180, 230, 255, 25 + rnd.nextInt(45)));
            g.drawOval(x, y, s, s);
        }
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(120, 200, 255, 14));
            int x = rnd.nextInt(W);
            g.fillPolygon(new int[]{x, x + 70, x + 260, x + 120}, new int[]{0, 0, H, H}, 4);
        }
    }

    /** 픽셀 체커: 40px 칸을 두 가지 회색으로 번갈아 칠한다. */
    private static void paintChecker(Graphics2D g) {
        for (int y = 0; y < H; y += 40)
            for (int x = 0; x < W; x += 40) {
                boolean dark = ((x / 40) + (y / 40)) % 2 == 0;
                g.setColor(dark ? new Color(34, 34, 44) : new Color(48, 48, 62));
                g.fillRect(x, y, 40, 40);
            }
    }

    /** 그 밖의 배경: 단색 회색. */
    private static void paintDefault(Graphics2D g) {
        g.setColor(ScreenStyle.BACKGROUND);
        g.fillRect(0, 0, W, H);
    }
}