package ui;

import game.Game;
import game.GameListener;
import settings.Records;

/** 게임이 끝났을 때 스프린트 기록 시간과 모드별 점수를 저장하는 리스너. */
final class GameRecordListener implements GameListener {
    private final Game game;
    private final String gameName;

    /** 기록할 게임과 현재 모드 이름을 받아 생성한다. */
    GameRecordListener(Game game, String gameName) {
        this.game = game;
        this.gameName = gameName;
    }

    /** 스프린트 목표 줄 수를 달성하면 걸린 시간을 기록으로 제출한다. */
    @Override public void onGoalReached() {
        String mode = Records.sprintKey(game.targetLines());
        Records.get().submitTime(mode, game.elapsedTimeMs());
    }

    /** 게임 오버 시 탑 아웃 런은 버틴 시간을, 이외 다른 모드들은 점수를 기록으로 제출한다. */
    @Override public void onGameOver() {
        if ("TOP OUT RUN".equals(gameName)) {
            Records.get().submitTime("topout", game.elapsedTimeMs());
        } else if (game.targetLines() <= 0) {
            String scoreKey = Records.scoreKeyForGameName(gameName);
            if (scoreKey != null) Records.get().submitScore(scoreKey, game.scoring.score);
        }
    }
}
