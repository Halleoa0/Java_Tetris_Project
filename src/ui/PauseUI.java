package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.JPanel;

/** 일시정지 메뉴(RESUME/RESTART/QUIT)를 그리고 현재 선택된 항목을 관리하는 클래스. */
final class PauseUI {
    private static final String[] OPTIONS = { "RESUME", "RESTART", "QUIT" };
    private final JPanel panel;
    private final Image guideImage;
    private final Font orbitBlack, interBlack, interMedium;
    private int selection;

    /** 그릴 패널, 조작 안내 이미지, 폰트를 받아 생성한다. */
    PauseUI(JPanel panel, Image guideImage, Fonts fonts) {
        this.panel = panel;
        this.guideImage = guideImage;
        this.orbitBlack = fonts.orbitBlack();
        this.interBlack = fonts.interBlack();
        this.interMedium = fonts.interMedium();
    }

    /** 선택을 첫 번째 항목(RESUME)으로 되돌린다. */
    void resetSelection() { selection = 0; }
    /** 현재 선택된 항목의 번호를 반환한다. (0: RESUME, 1: RESTART, 2: QUIT) */
    int selection() { return selection; }
    /** 선택을 위/아래로 옮긴다. 끝에 도달하면 반대쪽 끝으로 돌아간다 */
    void moveSelection(int direction) { selection = Math.floorMod(selection + direction, OPTIONS.length); }

    /** 화면을 검게 덮고 PAUSE 제목, 메뉴 항목(선택 항목은 강조), 조작 안내 이미지를 그린다. */
    void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 255));
        g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        Map<TextAttribute, Float> attributes = Map.of(TextAttribute.TRACKING, 0.3f);
        g.setColor(Color.WHITE);
        g.setFont(orbitBlack.deriveFont(30f).deriveFont(attributes));
        g.drawString("PAUSE", 40, 70);
        int spacing = 80;
        int firstCenterY = panel.getHeight() / 2 - (OPTIONS.length - 1) * spacing / 2;
        for (int i = 0; i < OPTIONS.length; i++) {
            g.setColor(i == selection ? Color.WHITE : new Color(70, 70, 70));
            g.setFont((i == selection ? interBlack : interMedium).deriveFont(32f).deriveFont(attributes));
            FontMetrics metrics = g.getFontMetrics();
            int x = (panel.getWidth() - metrics.stringWidth(OPTIONS[i])) / 2;
            int y = firstCenterY + i * spacing + (metrics.getAscent() - metrics.getDescent()) / 2;
            g.drawString(OPTIONS[i], x, y);
        }
        int guideX = (panel.getWidth() - guideImage.getWidth(panel)) / 2;
        int guideY = panel.getHeight() - guideImage.getHeight(panel) - 50;
        g.drawImage(guideImage, guideX, guideY, panel);
    }
}
