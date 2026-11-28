package graphics;

import java.awt.Color;

/** 여러 화면이 함께 쓰는 화면 크기와 기본 배경색. */
public final class ScreenStyle {
    public static final int WIDTH = 1280, HEIGHT = 720;
    /** 기본 다크 그레이 배경색. */
    public static final Color BACKGROUND = new Color(41, 41, 41);

    private ScreenStyle() { }
}