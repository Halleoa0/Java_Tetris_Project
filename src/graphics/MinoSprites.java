package graphics;

import game.Tetromino;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * 미노 타일 시트(Mino.png)를 다루는 공통 기능. (불러와 자르기, 블록별 타일 번호, 색 입히기)
 * 게임 화면과 상점 테마가 함께 쓴다.
 */
public final class MinoSprites {
    /** 시트에서 타일 한 칸의 크기(px). */
    private static final int TILE_SIZE = 25;
    /** 시트에 들어 있는 타일 수. (미노 7개 + 고스트 1개) */
    private static final int TILE_COUNT = 8;
    /** 고스트(예상 착지 위치) 타일 번호. */
    public static final int GHOST_TILE = 7;
    private static final String SHEET_PATH = "Images/Mino.png";

    // 밝기를 계산할 때 쓰는 RGB 가중치
    private static final float RED_LUMA = 0.299f, GREEN_LUMA = 0.587f, BLUE_LUMA = 0.114f;

    private MinoSprites() { }

    /** 테트로미노 종류를 타일 시트에서의 번호로 변환한다. (순서: Z, L, O, S, I, J, T) */
    public static int indexOf(Tetromino type) {
        return switch (type) {
            case Zmino -> 0;
            case Lmino -> 1;
            case Omino -> 2;
            case Smino -> 3;
            case Imino -> 4;
            case Jmino -> 5;
            case Tmino -> 6;
        };
    }

    /** 타일 시트를 불러와 타일 8개로 자른다. */
    public static BufferedImage[] loadTiles() {
        try {
            BufferedImage sheet = ImageIO.read(Path.of(SHEET_PATH).toFile());
            if (sheet.getWidth() != TILE_SIZE * TILE_COUNT || sheet.getHeight() != TILE_SIZE)
                throw new IllegalStateException("이미지 사이즈가 200x25가 아님. " + SHEET_PATH);
            BufferedImage[] tiles = new BufferedImage[TILE_COUNT];
            for (int i = 0; i < tiles.length; i++) tiles[i] = sheet.getSubimage(i * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE);
            return tiles;
        } catch (IOException e) {
            throw new IllegalStateException("미노 이미지를 불러올 수 없음. " + SHEET_PATH, e);
        }
    }

    /** 원본 타일의 밝기(음영)는 유지하고 색만 target으로 바꾼 새 이미지를 만든다. */
    public static BufferedImage tint(BufferedImage src, Color target) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float base = luminance(target.getRed(), target.getGreen(), target.getBlue());
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int pixel = src.getRGB(x, y), alpha = pixel >>> 24;
                float lum = luminance((pixel >> 16) & 255, (pixel >> 8) & 255, pixel & 255);
                float scale = base == 0 ? 1f : lum / base;
                int red = Math.min(255, Math.round(target.getRed() * scale));
                int green = Math.min(255, Math.round(target.getGreen() * scale));
                int blue = Math.min(255, Math.round(target.getBlue() * scale));
                out.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return out;
    }

    /** RGB 값의 밝기(0 ~ 1). */
    private static float luminance(int red, int green, int blue) {
        return (red * RED_LUMA + green * GREEN_LUMA + blue * BLUE_LUMA) / 255f;
    }
}