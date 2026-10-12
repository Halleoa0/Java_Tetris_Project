package shop;

import game.Game;
import game.GameListener;
import game.Tetromino;

/** 게임 이벤트를 보고 위클리 미션 진행도를 올린다. 실제 플레이하는 Game에 addListener로 붙인다. (튜토리얼 제외) */
public final class MissionTracker implements GameListener {
    private final Game game;
    private final PlayerData data = PlayerData.get();

    public MissionTracker(Game game) { this.game = game; }

    @Override public void onLinesCleared(int lines) {
        data.record(Mission.Metric.LINES, lines);
        if (lines == 4) data.record(Mission.Metric.TETRIS, 1);
    }

    @Override public void onLock(Tetromino type) { data.record(Mission.Metric.PIECES, 1); }

    @Override public void onHardDrop() { data.record(Mission.Metric.HARDDROP, 1); }

    /** 콤보/점수는 블록 고정 처리(점수 계산)가 끝난 뒤 값을 읽어야 해서 스폰 직전에 확인한다. */
    @Override public void onBeforeSpawn() { checkPeaks(); }

    @Override public void onGoalReached() {
        checkPeaks();
        data.record(Mission.Metric.GAMES, 1);
        data.record(Mission.Metric.SPRINT, 1);
    }

    @Override public void onGameOver() {
        checkPeaks();
        data.record(Mission.Metric.GAMES, 1);
    }

    private void checkPeaks() {
        data.record(Mission.Metric.COMBO, game.scoring.combo);   // combo는 화면의 "N Combo"와 같은 값, 콤보 없음(-1)은 무시됨
        data.record(Mission.Metric.SCORE, game.scoring.score);
    }
}
