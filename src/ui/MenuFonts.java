package ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;








/** 메뉴와 게임 화면에서 사용하는 커스텀 폰트 로딩 유틸리티.
 * 외부 폰트를 불러오고, 화면에 안전하게 띄우는 역할을 하는 클래스.
 */
public final class MenuFonts {
    /** 유틸리티 클래스이므로 객체 생성을 막는다. */
    private MenuFonts() { }

    /** 메뉴용 픽셀 폰트(PressStart2P)를 불러온다. 실패하면 기본 고정폭 폰트를 반환한다. */
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

    /// 폰트를 가져옴. Black
    public static Font loadBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Inter_18pt-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Inter_18pt-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    /** Inter Bold(굵은) 폰트를 불러온다 */
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

    /** Orbitron Black(가장 굵은) 폰트를 불러온다 */
    public static Font loadOrbitBlack() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of("Fonts/Orbitron-Black.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Orbitron-Black 폰트를 불러올 수 없습니다.", e);
        }
    }

    /** Orbitron Bold(굵은) 폰트를 불러온다 */
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
