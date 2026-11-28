package ui;

import game.Game;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import audio.AudioManager;
import settings.Records;

/** 게임 오버 결과 창(이번 기록/최고 기록)과 다시 시작/메인화면 버튼을 담당하는 클래스. */
final class GameOverUI {
    private static final int BOARD_X = 517, BOARD_Y = 111;
    private final Game game;
    private final String gameName;
    private final Font interBlack;
    private final Font sansKRBlack;
    private final JButton restartButton = new JButton("다시 시작");
    private final JButton menuButton = new JButton("메인화면");
    private int selection;
    private final Runnable restartAction;
    private final Runnable homeAction;
    private final BooleanSupplier visible;

    /** 게임 정보, 폰트, 표시 조건, 버튼 동작을 받아 버튼을 만들고 부모 패널에 추가한다. */
    GameOverUI(Game game, String gameName, Fonts fonts, JPanel parent,
               BooleanSupplier visible, Runnable restartAction, Runnable homeAction) {
        this.game = game;
        this.gameName = gameName;
        this.interBlack = fonts.interBlack();
        this.sansKRBlack = fonts.sansKRBlack();
        this.visible = visible;
        this.restartAction = restartAction;
        this.homeAction = homeAction;
        configureButtons(parent);
    }

    /** 표시 조건에 따라 다시 시작/메인화면 버튼을 보이거나 숨긴다. */
    void updateVisibility() {
        boolean show = visible.getAsBoolean();
        restartButton.setVisible(show);
        menuButton.setVisible(show);
    }

    /** 키보드 좌/우 입력으로 선택된 버튼을 바꾸고 효과음을 재생한다. */
    void selectOption(int direction) {
        selection = Math.floorMod(selection + direction, 2);
        updateButtonStyle();
        AudioManager.get().playMenuSelect();
    }

    /** 현재 선택된 버튼을 누른 것처럼 실행한다. */
    void confirmSelection() {
        if (selection == 0) restartButton.doClick(0);
        else menuButton.doClick(0);
    }

    /** 버튼의 위치, 스타일, 클릭 동작을 설정하고 부모 패널에 추가한다. */
    private void configureButtons(JPanel parent) {
        restartButton.setBounds(BOARD_X + 10, BOARD_Y + 361, 108, 34);
        menuButton.setBounds(BOARD_X + 126, BOARD_Y + 361, 108, 34);
        for (JButton button : new JButton[] { restartButton, menuButton }) {
            button.setFont(sansKRBlack.deriveFont(13f));
            button.setBackground(new Color(30, 36, 51));
            button.setForeground(Color.WHITE);
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorderPainted(true);
            button.setFocusable(false);
        }
        restartButton.addActionListener(event -> {
            AudioManager.get().playMenuSelect();
            selection = 0;
            updateButtonStyle();
            restartAction.run();
        });
        menuButton.addActionListener(event -> {
            AudioManager.get().playMenuSelect();
            homeAction.run();
        });
        parent.add(restartButton);
        parent.add(menuButton);
        updateButtonStyle();
        updateVisibility();
    }

    /** 선택된 버튼은 노란 테두리와 밝은 배경으로, 나머지는 기본 스타일로 표시한다. */
    private void updateButtonStyle() {
        JButton[] buttons = { restartButton, menuButton };
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = i == selection;
            buttons[i].setBorder(BorderFactory.createLineBorder(
                    selected ? new Color(255, 220, 110) : new Color(100, 110, 130), selected ? 2 : 1));
            buttons[i].setBackground(selected ? new Color(55, 68, 91) : new Color(30, 36, 51));
        }
    }

    /** 결과 창을 그린다. 시간 기록 모드(스프린트, 탑 아웃 런)는 시간을, 그 외 모드는 점수를 이번 기록/최고 기록으로 보여준다. */
    void draw(Graphics2D g) {
        int boxX = BOARD_X + 5, boxY = BOARD_Y + 136, boxW = 240, boxH = 300;
        int centerX = boxX + boxW / 2;
        g.setColor(new Color(12, 16, 25, 238));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g.setColor(new Color(115, 135, 170));
        g.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(interBlack.deriveFont(24f));
        drawCentered(g, "GAME OVER", centerX, boxY + 42);

        boolean timedMode = game.targetLines() > 0 || "TOP OUT RUN".equals(gameName);
        String currentValue, bestValue;
        if (timedMode) {
            String mode = game.targetLines() > 0 ? Records.sprintKey(game.targetLines()) : "topout";
            currentValue = Records.formatTime(game.elapsedTimeMs());
            int bestMs = Records.get().bestTimeMs(mode);
            bestValue = bestMs > 0 ? Records.formatTime(bestMs) : "-";
        } else {
            String mode = Records.scoreKeyForGameName(gameName);
            currentValue = String.format("%,d", game.scoring.score);
            int bestScore = mode == null ? -1 : Records.get().bestScore(mode);
            bestValue = bestScore > 0 ? String.format("%,d", bestScore) : "-";
        }
        g.setColor(new Color(190, 200, 220));
        g.setFont(sansKRBlack.deriveFont(14f));
        drawCentered(g, "이번 기록", centerX, boxY + 88);
        g.setColor(Color.WHITE);
        g.setFont(interBlack.deriveFont(23f));
        drawCentered(g, currentValue, centerX, boxY + 120);
        g.setColor(new Color(190, 200, 220));
        g.setFont(sansKRBlack.deriveFont(14f));
        drawCentered(g, "BEST 기록", centerX, boxY + 165);
        g.setColor(new Color(255, 220, 110));
        g.setFont(interBlack.deriveFont(21f));
        drawCentered(g, bestValue, centerX, boxY + 198);
    }

    /** 문자열을 centerX 기준 가운데 정렬로 그린다. */
    private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baselineY);
    }
}
