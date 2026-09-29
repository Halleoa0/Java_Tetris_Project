import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.function.Consumer;

/** 게임 화면 위에 튜토리얼 말풍선과 진행도를 그린다. */
final class TutorialOverlayRenderer implements Consumer<Graphics2D> {
    private static final Color BUBBLE = new Color(30, 36, 51, 238);
    private static final Color BORDER = new Color(95, 190, 220);
    private int stepNumber;
    private int stepCount;
    private String message = "";
    private boolean complete;

    void showStep(int stepNumber, int stepCount, String message) {
        this.stepNumber = stepNumber;
        this.stepCount = stepCount;
        this.message = message;
        complete = false;
    }

    void showComplete() {
        complete = true;
        message = "튜토리얼 완료!";
    }

    @Override public void accept(Graphics2D g) {
        int x = 520, y = 86, width = 700, height = 126;
        g.setColor(BUBBLE);
        g.fillRoundRect(x, y, width, height, 18, 18);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, width, height, 18, 18);
        g.fillPolygon(new int[]{x, x - 14, x + 10}, new int[]{y + 44, y + 56, y + 66}, 3);

        g.setColor(new Color(145, 205, 225));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(complete ? "TUTORIAL COMPLETE" : "TUTORIAL  " + stepNumber + " / " + stepCount,
                x + 24, y + 31);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawWrappedMessage(g, message, x + 24, y + 72, width - 48);
    }

    private void drawWrappedMessage(Graphics2D g, String text, int x, int y, int maxWidth) {
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxWidth && line.length() > 0) {
                g.drawString(line.toString(), x, lineY);
                line.setLength(0);
                lineY += 23;
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) g.drawString(line.toString(), x, lineY);
    }
}
