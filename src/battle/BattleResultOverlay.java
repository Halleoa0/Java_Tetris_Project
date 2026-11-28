package battle;

import graphics.ScreenStyle;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Locale;
import ui.Fonts;

/** 라운드/매치 결과 상자를 그리고, 상자 안의 누를 수 있는 글씨 위치를 관리한다. */
final class BattleResultOverlay {
    private static final int BOX_W = 440, BOX_H = 130;
    private static final int CHOICE_GAP = 40; // "NEXT ROUND"와 "MENU" 글씨 사이 간격

    private final BattleGame battle;
    private final Font interBlack, interMedium;

    // 마우스로 누를 수 있는 글씨들. 화면에 그릴 때 글씨가 있는 사각형을 기억해 두고, 클릭하면 그 안인지 확인한다.
    private Rectangle nextBounds; // "NEXT ROUND" 또는 "REMATCH"
    private Rectangle menuBounds; // "MENU"

    BattleResultOverlay(BattleGame battle, Fonts fonts) {
        this.battle = battle;
        this.interBlack = fonts.interBlack();
        this.interMedium = fonts.interMedium();
    }

    /** p가 "다음 라운드 / 다시 대결" 글씨 위인지 확인한다. */
    boolean isNextAt(Point p) { return nextBounds != null && nextBounds.contains(p); }

    /** p가 "메뉴" 글씨 위인지 확인한다. */
    boolean isMenuAt(Point p) { return menuBounds != null && menuBounds.contains(p); }

    /** 라운드나 매치가 끝난 상태면 결과 상자를 그린다. mouse는 지금 마우스 위치(없으면 null). */
    void draw(Graphics2D g, Point mouse) {
        if (battle.getState() == BattleGame.State.ROUND_OVER) drawRoundOver(g, mouse);
        else if (battle.getState() == BattleGame.State.MATCH_OVER) drawMatchOver(g, mouse);
    }

    /// 라운드 종료 화면
    private void drawRoundOver(Graphics2D g, Point mouse) {
        boolean playerWon = battle.getRoundWinner() == BattleGame.Winner.PLAYER;
        String title = playerWon ? "PLAYER 1 WINS ROUND" : "CPU WINS ROUND";
        String reason = switch (battle.getWinReason()) {
            case KO -> playerWon ? "KO - CPU TOPPED OUT" : "KO - YOU TOPPED OUT";
            case TARGET_SCORE -> String.format(Locale.US, "REACHED %,d POINTS FIRST", BattleGame.TARGET_SCORE);
            case TIME_OUT -> "TIME UP - MORE ATTACK / SCORE WINS";
            default -> "";
        };
        drawBox(g, title, reason, "[SPACE] NEXT ROUND", mouse);
    }

    /// 매치(3판 2선승) 종료 화면
    private void drawMatchOver(Graphics2D g, Point mouse) {
        String title = battle.getMatchWinner() == BattleGame.Winner.PLAYER ? "YOU WIN!" : "CPU WINS!";
        String score = "FINAL " + battle.getPlayerWins() + " : " + battle.getBotWins();
        drawBox(g, title, score, "[SPACE] REMATCH", mouse);
    }

    /// 결과 상자. GamePanel의 GAME OVER 상자와 같은 모양(어두운 둥근 상자 + 흰 글씨)이다.
    /// nextText : 누를 수 있는 글씨 (다음 라운드 또는 다시 대결). 옆에 "(M) MENU"도 같이 그린다.
    private void drawBox(Graphics2D g, String title, String line1, String nextText, Point mouse) {
        int x = (ScreenStyle.WIDTH - BOX_W) / 2, y = (ScreenStyle.HEIGHT - BOX_H) / 2;
        g.setColor(new Color(12, 16, 25, 220));
        g.fillRoundRect(x, y, BOX_W, BOX_H, 12, 12);

        g.setColor(Color.WHITE);
        g.setFont(interBlack.deriveFont(24f));
        BattleRenderer.drawCentered(g, title, y + 45);
        g.setFont(interMedium.deriveFont(14f));
        BattleRenderer.drawCentered(g, line1, y + 78);

        // 아래 줄 : 누를 수 있는 글씨 2개를 가운데에 나란히 그림
        g.setFont(interBlack.deriveFont(15f));
        String menuText = "[M] MENU";
        FontMetrics fm = g.getFontMetrics();
        int startX = (ScreenStyle.WIDTH - fm.stringWidth(nextText) - CHOICE_GAP - fm.stringWidth(menuText)) / 2;
        nextBounds = drawClickableText(g, nextText, startX, y + 108, mouse);
        menuBounds = drawClickableText(g, menuText, startX + fm.stringWidth(nextText) + CHOICE_GAP, y + 108, mouse);
    }

    /// 누를 수 있는 글씨를 그리고, 글씨가 있는 사각형을 돌려준다. 마우스를 올리면 흰색, 아니면 회색.
    private Rectangle drawClickableText(Graphics2D g, String text, int x, int y, Point mouse) {
        FontMetrics fm = g.getFontMetrics();
        Rectangle bounds = new Rectangle(x, y - fm.getAscent(), fm.stringWidth(text), fm.getHeight());
        bounds.grow(8, 4); // 글씨보다 조금 넓게 눌러도 되도록
        g.setColor(mouse != null && bounds.contains(mouse) ? Color.WHITE : new Color(150, 150, 150));
        g.drawString(text, x, y);
        return bounds;
    }
}