package battle;

import static ui.BoardLayout.BOARD_X;
import static ui.BoardLayout.BOARD_Y;
import static ui.BoardLayout.CELL;
import static ui.BoardLayout.MATRIX_H;
import static ui.BoardLayout.MATRIX_W;
import static ui.BoardLayout.MATRIX_X;
import static ui.BoardLayout.MATRIX_Y;

import game.Board;
import game.Game;
import graphics.ScreenStyle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.ImageObserver;
import java.util.Locale;
import ui.BoardRenderer;
import ui.Fonts;
import ui.LabelUI;
import ui.MinoTile;

/**
 * 대전 화면의 본 화면(양쪽 보드와 가운데 정보)을 그린다.
 * 1인용 화면 좌표로 그린 뒤 g.translate로 왼쪽(플레이어) / 오른쪽(CPU)으로 옮겨서 두 번 그리는 방식이다.
 */
final class BattleRenderer {

    // 대전에서 Matrix.png를 놓을 x 위치. 왼쪽(플레이어), 오른쪽(CPU)
    private static final int P1_MATRIX_X = 30, BOT_MATRIX_X = 722;

    // 방해 줄 대기열 게이지
    private static final int GARBAGE_BAR_OFFSET_X = 14; // 보드 왼쪽에서 떨어진 거리
    private static final int GARBAGE_BAR_W = 8;
    private static final int GARBAGE_DANGER_LINES = 4;  // 이 줄 수 이상이면 빨간색

    // 가운데 정보의 세로 위치
    private static final int VS_Y = 170, ROUND_Y = 250, WINS_Y = 330, TIME_Y = 466, TARGET_Y = 539;

    private static final Color DANGER_COLOR = new Color(255, 80, 80);
    private static final Color GARBAGE_BAR_BACKGROUND = new Color(25, 28, 36);

    private final BattleGame battle;
    private final ImageObserver observer;
    private final MinoTile assets;
    private final BoardRenderer boardRenderer;
    private final Font interBlack, interMedium;

    // 1인용과 같은 글씨(PPS, LINES, SCORE, 모드 이름)를 그리는 LabelUI. 플레이어용, CPU용 하나씩.
    private final LabelUI playerLabelUI;
    private final LabelUI botLabelUI;

    BattleRenderer(BattleGame battle, MinoTile assets, Fonts fonts, String botName, ImageObserver observer) {
        this.battle = battle;
        this.assets = assets;
        this.observer = observer;
        this.interBlack = fonts.interBlack();
        this.interMedium = fonts.interMedium();
        this.boardRenderer = new BoardRenderer(assets.minoTiles(), observer, BoardRenderer.GarbageStyle.GHOST_TILE);
        this.playerLabelUI = new LabelUI(battle.playerGame, fonts, assets.gameNamePanelImage(), "PLAYER 1");
        this.botLabelUI = new LabelUI(battle.botGame, fonts, assets.gameNamePanelImage(), botName);
    }

    /** 왼쪽(플레이어 1), 오른쪽(CPU), 가운데 정보를 그린다. */
    void draw(Graphics2D g) {
        drawSide(g, P1_MATRIX_X, playerLabelUI, battle.playerGame, battle.playerGarbage, battle.getPlayerTotalAttack());
        drawSide(g, BOT_MATRIX_X, botLabelUI, battle.botGame, battle.botGarbage, battle.getBotTotalAttack());
        drawCenterInfo(g);
    }

    /// 한 사람의 화면(Matrix.png, 홀드, 넥스트, 보드, 방해 줄 게이지, 글씨)을 그린다.
    /// 1인용 GamePanel과 똑같은 좌표로 그리고, g.translate로 matrixX 위치까지 옮긴다.
    private void drawSide(Graphics2D g, int matrixX, LabelUI labelUI, Game game, GarbageQueue garbage, int attack) {
        int shift = matrixX - MATRIX_X; // 1인용 화면보다 얼마나 옮길지 (왼쪽이면 음수)
        g.translate(shift, 0); // 지금부터 그리는 것은 전부 shift만큼 옮겨져서 그려짐

        g.drawImage(assets.matrixImage(), MATRIX_X, MATRIX_Y, MATRIX_W, MATRIX_H, observer);
        boardRenderer.draw(g, game);
        drawGarbageBar(g, BOARD_X - GARBAGE_BAR_OFFSET_X, BOARD_Y, garbage.getPendingCount());

        // 1인용과 같은 글씨들 (LabelUI)
        labelUI.drawGameNamePanel(g);
        labelUI.drawPpsLabel(g);
        labelUI.drawLineLabel(g);
        labelUI.drawScoreValueLabel(g);
        labelUI.drawAttackLabel(g, attack); // 1인용의 TIME 자리. 대전은 시간을 가운데에 따로 보여줘서 공격량을 대신 보여줌
        labelUI.drawScoreLabel(g);          // T-SPIN, COMBO 같은 점수 글씨

        g.translate(-shift, 0); // 원래 위치로 되돌림
    }

    /// 방해 줄 대기열 게이지. 기다리는 방해 줄 수만큼 보드 왼쪽에 노란색(4줄 이상이면 빨간색)으로 채운다.
    private void drawGarbageBar(Graphics2D g, int x, int y, int pendingLines) {
        int barH = (Board.HEIGHT - Board.HIDDEN_ROWS) * CELL;
        g.setColor(GARBAGE_BAR_BACKGROUND);
        g.fillRect(x, y, GARBAGE_BAR_W, barH);

        if (pendingLines > 0) {
            int fillH = Math.min(barH, pendingLines * CELL);
            g.setColor(pendingLines >= GARBAGE_DANGER_LINES ? DANGER_COLOR : Color.YELLOW);
            g.fillRect(x + 1, y + barH - fillH, GARBAGE_BAR_W - 2, fillH);
        }
    }

    /// 가운데 정보(라운드, 승리 수, 남은 시간, 목표 점수). LabelUI와 같은 글씨 모양(작은 제목 + 큰 숫자)으로 그린다.
    private void drawCenterInfo(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(interBlack.deriveFont(36f));
        drawCentered(g, "VS", VS_Y);

        drawCenterLabel(g, "ROUND", battle.getCurrentRound() + " / 3", ROUND_Y, Color.WHITE);
        drawCenterLabel(g, "WINS", battle.getPlayerWins() + " : " + battle.getBotWins(), WINS_Y, Color.WHITE);

        // 남은 시간. 마지막 45초(HURRY UP)에는 공격이 1.5배가 되고, 글씨가 빨간색으로 바뀜
        int timeSec = (int) Math.ceil(battle.getRoundTimer());
        String timeText = String.format(Locale.ROOT, "%02d:%02d", timeSec / 60, timeSec % 60);
        if (battle.isHurryUp()) drawCenterLabel(g, "ATTACK x1.5", timeText, TIME_Y, DANGER_COLOR);
        else drawCenterLabel(g, "TIME", timeText, TIME_Y, Color.WHITE);

        String target = String.format(Locale.US, "%,d", BattleGame.TARGET_SCORE);
        drawCenterLabel(g, "TARGET", target, TARGET_Y, Color.WHITE);
    }

    /// 작은 제목(16) 아래에 큰 값(24)을 화면 가운데 정렬로 그린다. (LabelUI와 같은 글씨 크기)
    private void drawCenterLabel(Graphics2D g, String title, String value, int y, Color valueColor) {
        g.setColor(Color.WHITE);
        g.setFont(interMedium.deriveFont(16f));
        drawCentered(g, title, y);
        g.setColor(valueColor);
        g.setFont(interBlack.deriveFont(24f));
        drawCentered(g, value, y + 28);
    }

    /// 글씨를 화면 가로 가운데에 그린다.
    static void drawCentered(Graphics2D g, String text, int y) {
        g.drawString(text, (ScreenStyle.WIDTH - g.getFontMetrics().stringWidth(text)) / 2, y);
    }
}