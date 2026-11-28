package ui;

import game.Tetromino;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import settings.Settings;

/** 게임 화면에서 쓰는 이미지들을 불러오고, 미노 타일 시트를 잘라 색상/스킨을 적용해 준비하는 클래스. */
final class MinoTile {
    private final Image matrixImage = loadImage("Images/Board/Matrix.png");
    private final Image guideImage = loadImage("Images/Board/Guide.png");
    private final Image pauseGuideImage = loadImage("Images/PauseGuide.png");
    private final Image gameNamePanelImage = loadImage("Images/Board/GameNamePanel.png");
    private final BufferedImage[] minoTiles = loadMinoTiles("Images/Mino.png");

    /** 보드 배경(매트릭스) 이미지를 반환한다. */
    Image matrixImage() { return matrixImage; }
    /** 조작 안내 이미지를 반환한다. */
    Image guideImage() { return guideImage; }
    /** 일시정지 화면의 조작 안내 이미지를 반환한다. */
    Image pauseGuideImage() { return pauseGuideImage; }
    /** 모드 이름 패널 이미지를 반환한다. */
    Image gameNamePanelImage() { return gameNamePanelImage; }
    /** 블록 종류별 미노 타일 8개(마지막은 고스트)를 반환한다. */
    BufferedImage[] minoTiles() { return minoTiles; }

    /** 경로의 이미지 파일을 불러온다 */
    private Image loadImage(String path) {
        try {
            return javax.imageio.ImageIO.read(Path.of(path).toFile());
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 불러올 수 없음. " + path, e);
        }
    }

    /**
     * 200x25 크기의 미노 시트를 25x25 타일 8개로 자른다.
     * 색맹 모드가 켜져 있으면 설정 색상으로 변경하고, 상점에서 장착한 미노 스킨을 적용한다.
     */
    private BufferedImage[] loadMinoTiles(String path) {
        try {
            BufferedImage sheet = javax.imageio.ImageIO.read(Path.of(path).toFile());
            if (sheet.getWidth() != 200 || sheet.getHeight() != 25)
                throw new IllegalStateException("이미지 사이즈가 200x25가 아님. " + path);
            BufferedImage[] tiles = new BufferedImage[8];
            for (int i = 0; i < tiles.length; i++) tiles[i] = sheet.getSubimage(i * 25, 0, 25, 25);
            if (Settings.get().colorMode() != Settings.ColorMode.OFF) {
                for (Tetromino type : Tetromino.values()) {
                    int index = BoardRenderer.minoTileIndex(type);
                    tiles[index] = tint(tiles[index], Settings.get().minoColor(type));
                }
            }
            shop.ThemeManager.applyMinoSkin(tiles, BoardRenderer::minoTileIndex);
            return tiles;
        } catch (IOException e) {
            throw new IllegalStateException("미노 이미지에 문제 발생. " + path, e);
        }
    }

    /** 원본 타일의 명암(밝기)은 유지한 채 목표 색상으로 물들인 새 이미지를 만든다. */
    private static BufferedImage tint(BufferedImage src, Color target) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float base = (target.getRed() * 0.299f + target.getGreen() * 0.587f + target.getBlue() * 0.114f) / 255f;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int pixel = src.getRGB(x, y), alpha = pixel >>> 24;
                float lum = (((pixel >> 16) & 255) * 0.299f + ((pixel >> 8) & 255) * 0.587f + (pixel & 255) * 0.114f) / 255f;
                float scale = base == 0 ? 1f : lum / base;
                int red = Math.min(255, Math.round(target.getRed() * scale));
                int green = Math.min(255, Math.round(target.getGreen() * scale));
                int blue = Math.min(255, Math.round(target.getBlue() * scale));
                out.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return out;
    }
}
