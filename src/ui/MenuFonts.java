package ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;








/** 메뉴와 게임 화면에서 사용하는 커스텀 폰트 로딩 유틸리티. */
public final class MenuFonts {
    private MenuFonts() { }

    public static Font loadPressStart2P() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT,
                    Path.of("Fonts/PressStart2P-Regular.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            return new Font(Font.MONOSPACED, Font.PLAIN, 16);
        }
    }

    /// 폰트를 가져옵니다. Black(가장 굵은)
    public static Font loadBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Inter_18pt-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Inter_18pt-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    public static Font loadBold() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Inter-Bold.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Inter-Bold 폰트를 불러올 수 없습니다.", e);
        }
    }

    /// 폰트를 가져옵니다. Medium(보통)
    public static Font loadMedium() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Inter_18pt-Medium.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Inter_18pt-Medium 폰트를 불러올 수 없습니다.", e);
        }
    }

    /// 폰트를 가져옵니다. 한글 전용
    public static Font loadKRBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/NotoSansKR-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("NotoSansKR-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    public static Font loadOrbitBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Orbitron-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Orbitron-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    public static Font loadOrbitBold() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Orbitron-Bold.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Orbitron-Bold 폰트를 불러올 수 없습니다.", e);
        }
    }
}
