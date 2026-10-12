package shop;

import java.awt.Color;

/** 상점에서 파는 테마. 미노 스킨(MINO)과 배경화면(BACKGROUND) 두 종류. */
public enum ShopItem {
    // ---- 미노 스킨 (Mino.png의 음영은 유지하고 색만 바꾼다) ----
    // 색 순서: Z, L, O, S, I, J, T
    MINO_CLASSIC("기본 미노", Type.MINO, 0, null),
    MINO_NEON("네온 미노", Type.MINO, 30, new Color[]{
            new Color(255, 40, 100), new Color(255, 150, 0), new Color(255, 245, 0), new Color(60, 255, 90),
            new Color(0, 240, 255), new Color(70, 90, 255), new Color(220, 60, 255)}),
    MINO_PASTEL("파스텔 미노", Type.MINO, 30, new Color[]{
            new Color(255, 160, 170), new Color(255, 200, 150), new Color(255, 240, 160), new Color(180, 235, 180),
            new Color(160, 230, 240), new Color(165, 180, 250), new Color(215, 180, 240)}),
    MINO_MONO("모노 미노", Type.MINO, 40, new Color[]{
            new Color(235, 235, 235), new Color(205, 205, 205), new Color(255, 255, 255), new Color(175, 175, 175),
            new Color(150, 150, 150), new Color(120, 120, 120), new Color(190, 190, 190)}),
    MINO_SAKURA("벚꽃 미노", Type.MINO, 50, new Color[]{
            new Color(255, 105, 150), new Color(255, 140, 170), new Color(255, 190, 205), new Color(240, 90, 140),
            new Color(255, 215, 225), new Color(220, 110, 160), new Color(250, 160, 190)}),
    MINO_GOLD("골드 미노", Type.MINO, 80, new Color[]{
            new Color(205, 140, 40), new Color(235, 175, 50), new Color(255, 215, 60), new Color(185, 125, 35),
            new Color(250, 225, 120), new Color(165, 110, 30), new Color(225, 160, 45)}),

    // ---- 배경화면 (ThemeManager가 코드로 그린다) ----
    BG_DEFAULT("기본 배경", Type.BACKGROUND, 0, null),
    BG_NIGHT("별이 빛나는 밤", Type.BACKGROUND, 40, null),
    BG_SUNSET("노을", Type.BACKGROUND, 40, null),
    BG_GRID("사이버 그리드", Type.BACKGROUND, 60, null),
    BG_OCEAN("깊은 바다", Type.BACKGROUND, 60, null),
    BG_CHECKER("픽셀 체커", Type.BACKGROUND, 30, null);

    public enum Type { MINO, BACKGROUND }

    public final String label;
    public final Type type;
    public final int price;
    /** 미노 스킨 팔레트(Z, L, O, S, I, J, T 순). 배경이나 기본 스킨은 null. */
    public final Color[] palette;

    ShopItem(String label, Type type, int price, Color[] palette) {
        this.label = label; this.type = type; this.price = price; this.palette = palette;
    }

    /** 가격이 0원이면 처음부터 가지고 있다. */
    public boolean isFree() { return price == 0; }

    public static ShopItem defaultOf(Type type) { return type == Type.MINO ? MINO_CLASSIC : BG_DEFAULT; }

    public static ShopItem byName(String name, Type type) {
        try {
            ShopItem it = valueOf(name);
            if (it.type == type) return it;
        } catch (RuntimeException ignored) { }
        return defaultOf(type);
    }
}
