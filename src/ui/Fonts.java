package ui;

import java.awt.Font;

public record Fonts(Font interBlack, Font interMedium, Font sansKRBlack, Font orbitBlack, Font orbitBold) {

    // 기본 폰트들을 ui/MenuFonts에서 불러온다
    public static Fonts load() {
        return new Fonts(
                MenuFonts.loadBlack(),
                MenuFonts.loadMedium(),
                MenuFonts.loadKRBlack(),
                MenuFonts.loadOrbitBlack(),
                MenuFonts.loadOrbitBold());
    }
}
