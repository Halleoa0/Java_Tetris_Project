package ui;

import game.Tetromino;
import graphics.MinoSprites;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import settings.Settings;

/** 게임 화면에서 쓰는 이미지들을 불러오고, 미노 타일에 색상/스킨을 적용해 준비하는 클래스. */
public final class MinoTile {
    private final Image matrixImage = loadImage("Images/Board/Matrix.png");
    private final Image guideImage = loadImage("Images/Board/Guide.png");
    private final Image pauseGuideImage = loadImage("Images/PauseGuide.png");
    private final Image gameNamePanelImage = loadImage("Images/Board/GameNamePanel.png");
    private final BufferedImage[] minoTiles = loadMinoTiles();

    /** 보드 배경(매트릭스) 이미지를 반환한다. */
    public Image matrixImage() { return matrixImage; }
    /** 조작 안내 이미지를 반환한다. */
    public Image guideImage() { return guideImage; }
    /** 일시정지 화면의 조작 안내 이미지를 반환한다. */
    public Image pauseGuideImage() { return pauseGuideImage; }
    /** 모드 이름 패널 이미지를 반환한다. */
    public Image gameNamePanelImage() { return gameNamePanelImage; }
    /** 블록 종류별 미노 타일 8개(마지막은 고스트)를 반환한다. */
    public BufferedImage[] minoTiles() { return minoTiles; }

    /** 경로의 이미지 파일을 불러온다 */
    private static Image loadImage(String path) {
        try {
            return javax.imageio.ImageIO.read(Path.of(path).toFile());
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 불러올 수 없음. " + path, e);
        }
    }

    /** 미노 타일을 불러온 뒤, 색맹 모드가 켜져 있으면 설정 색상으로 변경하고 상점에서 장착한 미노 스킨을 적용한다. */
    private static BufferedImage[] loadMinoTiles() {
        BufferedImage[] tiles = MinoSprites.loadTiles();
        if (Settings.get().colorMode() != Settings.ColorMode.OFF) {
            for (Tetromino type : Tetromino.values()) {
                int index = MinoSprites.indexOf(type);
                tiles[index] = MinoSprites.tint(tiles[index], Settings.get().minoColor(type));
            }
        }
        shop.ThemeManager.applyMinoSkin(tiles);
        return tiles;
    }
}