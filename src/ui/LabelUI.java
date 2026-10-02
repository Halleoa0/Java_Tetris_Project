package ui;

import game.Board;
import game.Game;
import java.awt.Color;
import java.awt.font.TextAttribute;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Locale;
import java.util.Map;



/// GamePanel에 존재하면 Label을 따로 별개의 클래스로 뺌
public final class LabelUI {
    private static final int CELL = 25;
    private static final int BOARD_X = 517;
    private static final int BOARD_Y = 111;

    private final Game game;
    private final Font interBlack;
    private final Font interMedium;
    private final Font sansKRBlack;
    private final Font orbitBlack;
    private final Font orbitBold;
    private final Image gameNamePanelImage;
    private final String gameName;

    public LabelUI(Game game, Font interBlack, Font interMedium, Font sansKRBlack, Font orbitBlack, Font orbitBold,
            Image gameNamePanelImage, String gameName) {
        this.game = game;
        this.interBlack = interBlack;
        this.interMedium = interMedium;
        this.sansKRBlack = sansKRBlack;
        this.orbitBlack = orbitBlack;
        this.orbitBold = orbitBold;
        this.gameNamePanelImage = gameNamePanelImage;
        this.gameName = gameName;
    }

    public void drawScoreLabel(Graphics2D g) {
        float a = game.scoring.labelAlpha();
        if (a <= 0f) return;
        int alpha = (int) (255 * a);

        g.setFont(orbitBold.deriveFont(Map.of(TextAttribute.SIZE, 16f, TextAttribute.TRACKING, 0.15f)));
        g.setColor(new Color(197, 110, 255, alpha));
        FontMetrics metrics2 = g.getFontMetrics();
        g.drawString(game.scoring.skillString,
                505 - metrics2.stringWidth(game.scoring.skillString), 262);

        float clearTracking = game.scoring.clearLineTracking();
        g.setFont(orbitBlack.deriveFont(Map.of(TextAttribute.SIZE, 36f, TextAttribute.TRACKING, clearTracking)));
        g.setColor(new Color(255, 255, 255, alpha));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(game.scoring.cleardLineString,
                500 - metrics.stringWidth(game.scoring.cleardLineString), 300);

        g.setFont(orbitBold.deriveFont(Map.of(TextAttribute.SIZE, 24f, TextAttribute.TRACKING, 0.15f)));
        g.setColor(new Color(255, 172, 0, alpha));
        FontMetrics metrics3 = g.getFontMetrics();
        g.drawString(game.scoring.comboString,
                500 - metrics3.stringWidth(game.scoring.comboString), 335);

        g.setFont(orbitBold.deriveFont(Map.of(TextAttribute.SIZE, 24f, TextAttribute.TRACKING, 0.15f)));
        g.setColor(new Color(255, 239, 137, alpha));
        FontMetrics metrics4 = g.getFontMetrics();
        g.drawString(game.scoring.perfectString,
                647 - metrics4.stringWidth(game.scoring.perfectString) / 2, 197);
    }

    public void drawPpsLabel(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        g.drawString("PPS", 470, 466);
        g.setFont(interBlack.deriveFont(24f));
        float pps = game.timer > 0f ? game.calPPS() : 0f;
        String ppsText = String.format(Locale.ROOT, "%.2f/s", pps);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(ppsText, 500 - metrics.stringWidth(ppsText), 466 + 28);
    }

    public void drawGameNamePanel(Graphics2D g) {
        int boardWidth = Board.WIDTH * CELL;
        int boardBottom = BOARD_Y + (Board.HEIGHT - Board.HIDDEN_ROWS) * CELL;
        int x = BOARD_X + (boardWidth - gameNamePanelImage.getWidth(null)) / 2;
        int y = boardBottom + 20;
        g.drawImage(gameNamePanelImage, x, y, null);

        g.setColor(Color.WHITE);
        g.setFont(sansKRBlack.deriveFont(15f));
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (gameNamePanelImage.getWidth(null) - metrics.stringWidth(gameName)) / 2;
        int textY = y + (gameNamePanelImage.getHeight(null) - metrics.getHeight()) / 2 + metrics.getAscent() - 1;
        g.drawString(gameName, textX, textY);
    }

    public void drawLineLabel(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        g.drawString("LINES", 456, 539);
        g.setFont(interBlack.deriveFont(24f));
        String lineText = String.format(Locale.ROOT, "%d", game.scoring.lines);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(lineText, 500 - metrics.stringWidth(lineText), 539 + 28);
    }

    public void drawScoreValueLabel(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        g.drawString("SCORE", 783, 466);
        g.setFont(interBlack.deriveFont(24f));
        g.drawString(String.format(Locale.US, "%,d", game.scoring.score), 783, 466 + 28);
    }

    public void drawTimeLabel(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        g.drawString("TIME", 783, 539);
        int totalSeconds = (int) game.timer;
        String timeText = String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        g.setFont(interBlack.deriveFont(24f));
        g.drawString(timeText, 783, 539 + 28);
    }
}
