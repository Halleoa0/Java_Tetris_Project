package mode;

import app.ScreenManager;
import game.Game;

public final class UltraMode implements GameMode {
    private static final int TIME_LIMIT_MS = 120_000;

    @Override public String getName() { return "ULTRA"; }
    @Override public String getDescription() { return "2분 동안 최고 점수에 도전"; }
    @Override public boolean isAvailable() { return true; }
    @Override public void start(ScreenManager screens) {
        screens.startGame(new Game(TIME_LIMIT_MS), "울트라");
    }
}
