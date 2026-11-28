package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.JPanel;

/** 일시정지 메뉴를 그리고 현재 선택된 항목을 관리하는 클래스. 메뉴 항목은 화면마다 다르게 지정한다. */
public final class PauseUI {
    private final String[] options;
    // 마우스 클릭용: 화면에 그릴 때 각 항목의 글씨가 있는 사각형을 기억해 둔다.
    private final Rectangle[] optionBounds;
    private final JPanel panel;
    private final Image guideImage;
    private final Font orbitBlack, interBlack, interMedium;
    private int selection;

    /** 그릴 패널, 조작 안내 이미지, 폰트, 메뉴 항목들을 받아 생성한다. */
    public PauseUI(JPanel panel, Image guideImage, Fonts fonts, String... options) {
        this.panel = panel;
        this.guideImage = guideImage;
        this.orbitBlack = fonts.orbitBlack();
        this.interBlack = fonts.interBlack();
        this.interMedium = fonts.interMedium();
        this.options = options.clone();
        this.optionBounds = new Rectangle[options.length];
    }

    /** 선택을 첫 번째 항목으로 되돌린다. */
    public void resetSelection() { selection = 0; }
    /** 현재 선택된 항목의 번호를 반환한다. (생성할 때 넘긴 항목 순서) */
    public int selection() { return selection; }
    /** 지정한 번호의 항목을 선택한다. (마우스 선택용) */
    public void select(int index) { selection = index; }
    /** 선택을 위/아래로 옮긴다. 끝에 도달하면 반대쪽 끝으로 돌아간다 */
    public void moveSelection(int direction) { selection = Math.floorMod(selection + direction, options.length); }

    /** p 위치에 있는 메뉴 항목의 번호. 없으면 -1. */
    public int optionAt(Point p) {
        for (int i = 0; i < optionBounds.length; i++) {
            if (optionBounds[i] != null && optionBounds[i].contains(p)) return i;
        }
        return -1;
    }

    /** 화면을 검게 덮고 PAUSE 제목, 메뉴 항목(선택 항목은 강조), 조작 안내 이미지를 그린다. */
    public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 255));
        g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        Map<TextAttribute, Float> attributes = Map.of(TextAttribute.TRACKING, 0.3f);
        g.setColor(Color.WHITE);
        g.setFont(orbitBlack.deriveFont(30f).deriveFont(attributes));
        g.drawString("PAUSE", 40, 70);
        int spacing = 80;
        int firstCenterY = panel.getHeight() / 2 - (options.length - 1) * spacing / 2;
        for (int i = 0; i < options.length; i++) {
            g.setColor(i == selection ? Color.WHITE : new Color(70, 70, 70));
            g.setFont((i == selection ? interBlack : interMedium).deriveFont(32f).deriveFont(attributes));
            FontMetrics metrics = g.getFontMetrics();
            int x = (panel.getWidth() - metrics.stringWidth(options[i])) / 2;
            int y = firstCenterY + i * spacing + (metrics.getAscent() - metrics.getDescent()) / 2;
            g.drawString(options[i], x, y);

            optionBounds[i] = new Rectangle(x, y - metrics.getAscent(), metrics.stringWidth(options[i]), metrics.getHeight());
            optionBounds[i].grow(20, 10); // 글씨보다 조금 넓게 눌러도 되도록
        }
        int guideX = (panel.getWidth() - guideImage.getWidth(panel)) / 2;
        int guideY = panel.getHeight() - guideImage.getHeight(panel) - 50;
        g.drawImage(guideImage, guideX, guideY, panel);
    }
}