package shop;

import game.Tetromino;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;
import settings.Settings;

/** 장착한 테마(미노 스킨, 배경화면)를 게임 화면에 적용한다. */
public final class ThemeManager {
    private static final int W = 1280, H = 720;
    private static final Map<ShopItem, BufferedImage> BG_CACHE = new EnumMap<>(ShopItem.class);

    private ThemeManager() { }

    /** 장착한 미노 스킨을 타일(0~6번)에 입힌다. 색각 보정 모드가 켜져 있으면 그쪽이 우선이라 건드리지 않는다. */
    public static void applyMinoSkin(BufferedImage[] tiles, ToIntFunction<Tetromino> tileIndex) {
        if (Settings.get().colorMode() != Settings.ColorMode.OFF) return;
        ShopItem skin = PlayerData.get().equipped(ShopItem.Type.MINO);
        if (skin.palette == null) return;
        // 팔레트 순서: Z, L, O, S, I, J, T
        Tetromino[] order = {Tetromino.Zmino, Tetromino.Lmino, Tetromino.Omino, Tetromino.Smino,
                Tetromino.Imino, Tetromino.Jmino, Tetromino.Tmino};
        for (int i = 0; i < order.length; i++) {
            int idx = tileIndex.applyAsInt(order[i]);
            tiles[idx] = tint(tiles[idx], skin.palette[i]);
        }
    }

    /** 상점 미리보기용: 지정한 스킨이 입혀진 타일 7개(Z, L, O, S, I, J, T 순서). */
    public static BufferedImage[] previewTiles(ShopItem skin) {
        try {
            BufferedImage sheet = javax.imageio.ImageIO.read(java.nio.file.Path.of("Images/Mino.png").toFile());
            BufferedImage[] tiles = new BufferedImage[7];
            for (int i = 0; i < 7; i++) {
                tiles[i] = sheet.getSubimage(i * 25, 0, 25, 25);
                if (skin.palette != null) tiles[i] = tint(tiles[i], skin.palette[i]);
            }
            return tiles;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Images/Mino.png를 불러올 수 없습니다.", e);
        }
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
            g.setColor(new Color(41, 41, 41));
            g.fillRect(x, y, w, h);
        } else {
            g.drawImage(background(bg), x, y, w, h, null);
        }
    }

    private static synchronized BufferedImage background(ShopItem bg) {
        return BG_CACHE.computeIfAbsent(bg, ThemeManager::render);
    }

    private static BufferedImage render(ShopItem bg) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random rnd = new Random(bg.ordinal() * 31L + 7);
        switch (bg) {
            case BG_NIGHT: {
                g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 32), 0, H, new Color(32, 24, 70)));
                g.fillRect(0, 0, W, H);
                for (int i = 0; i < 160; i++) {
                    int a = 90 + rnd.nextInt(165), s = rnd.nextInt(10) == 0 ? 3 : 2;
                    g.setColor(new Color(255, 255, 255, a));
                    g.fillOval(rnd.nextInt(W), rnd.nextInt(H), s, s);
                }
                g.setColor(new Color(245, 240, 210));
                g.fillOval(1060, 70, 70, 70);
                g.setColor(new Color(24, 20, 60));
                g.fillOval(1082, 60, 66, 66);
                break;
            }
            case BG_SUNSET: {
                g.setPaint(new GradientPaint(0, 0, new Color(40, 20, 80), 0, H, new Color(255, 120, 60)));
                g.fillRect(0, 0, W, H);
                g.setColor(new Color(255, 210, 120, 230));
                g.fillOval(W / 2 - 110, H - 230, 220, 220);
                g.setColor(new Color(60, 25, 55));
                g.fillRect(0, H - 90, W, 90);
                for (int i = 0; i < 24; i++) {
                    int bw = 30 + rnd.nextInt(50), bh = 30 + rnd.nextInt(110), bx = i * 56 + rnd.nextInt(14);
                    g.setColor(new Color(45, 18, 45));
                    g.fillRect(bx, H - 90 - bh, bw, bh);
                }
                break;
            }
            case BG_GRID: {
                g.setPaint(new GradientPaint(0, 0, new Color(8, 6, 24), 0, H, new Color(35, 8, 60)));
                g.fillRect(0, 0, W, H);
                g.setColor(new Color(0, 230, 255, 70));
                for (int x = 0; x <= W; x += 40) g.drawLine(x, 0, x, H);
                for (int y = 0; y <= H; y += 40) g.drawLine(0, y, W, y);
                g.setColor(new Color(255, 40, 200, 60));
                for (int x = 0; x <= W; x += 200) g.drawLine(x, 0, x, H);
                for (int y = 0; y <= H; y += 200) g.drawLine(0, y, W, y);
                break;
            }
            case BG_OCEAN: {
                g.setPaint(new GradientPaint(0, 0, new Color(10, 70, 120), 0, H, new Color(2, 12, 40)));
                g.fillRect(0, 0, W, H);
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
                break;
            }
            case BG_CHECKER: {
                for (int y = 0; y < H; y += 40)
                    for (int x = 0; x < W; x += 40) {
                        boolean dark = ((x / 40) + (y / 40)) % 2 == 0;
                        g.setColor(dark ? new Color(34, 34, 44) : new Color(48, 48, 62));
                        g.fillRect(x, y, 40, 40);
                    }
                break;
            }
            default:
                g.setColor(new Color(41, 41, 41));
                g.fillRect(0, 0, W, H);
        }
        g.dispose();
        return img;
    }

    /** 원본 타일의 밝기(음영)는 유지하고 색만 target으로 바꾼다. */
    public static BufferedImage tint(BufferedImage src, Color target) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float base = (target.getRed() * 0.299f + target.getGreen() * 0.587f + target.getBlue() * 0.114f) / 255f;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                float lum = (((p >> 16) & 255) * 0.299f + ((p >> 8) & 255) * 0.587f + (p & 255) * 0.114f) / 255f;
                float k = base == 0 ? 1f : lum / base;
                int r = Math.min(255, Math.round(target.getRed() * k));
                int gr = Math.min(255, Math.round(target.getGreen() * k));
                int b = Math.min(255, Math.round(target.getBlue() * k));
                out.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
        }
        return out;
    }
}
