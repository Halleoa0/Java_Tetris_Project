package mode;

import app.ScreenManager;
import game.Game;
import settings.Records;

/** 목표 줄 수를 가장 빠르게 지우는 모드. 걸린 시간이 최고 기록이 된다. */
public final class SprintMode implements GameMode {
    public static final int TARGET_LINES = 20;

    // 모드 선택 화면이 이 이름으로 설명 문구를 찾으므로 GameModes의 이름과 바꾸지 않는다.
    @Override public String getName() { return "20L SPRINT"; }

    @Override public String getDescription() {
        int best = Records.get().bestTimeMs(Records.sprintKey(TARGET_LINES));
        String text = TARGET_LINES + "줄을 가장 빠르게 지우기";
        return best > 0 ? text + "  |  BEST " + Records.formatTime(best) : text;
    }

    @Override public boolean isAvailable() { return true; }

    @Override public void start(ScreenManager screens) {
        screens.startGame(Game.sprint(TARGET_LINES), TARGET_LINES + "L 스프린트");
    }
}