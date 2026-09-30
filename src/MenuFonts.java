import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;

/** 메인 메뉴에서 사용하는 커스텀 폰트 로딩 유틸리티. */
final class MenuFonts {
    private MenuFonts() { }

    static Font loadPressStart2P() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT,
                    Path.of("Fonts/PressStart2P-Regular.ttf").toFile());
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException e) {
            return new Font(Font.MONOSPACED, Font.PLAIN, 16);
        }
    }
}
