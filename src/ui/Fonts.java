package ui;

import java.awt.Font;

// 게임 화면에서 사용하는 폰트들을 한 번에 묶어서 전달하기 위한 레코드.
// 레코드 타입: 불변 객체를 간결하게 정의할 수 있음. Fonts 클래스 명 뒤에 붙은 데이터들은 전부 private final로 취급됨
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
