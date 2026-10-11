package mode;

import app.ScreenManager;
import game.Board;
import game.Game;
import game.GameListener;
import java.util.Random;

public final class TopOutRunMode implements GameMode {
    @Override public String getName() { return "TOP-OUT RUN"; }

    @Override public String getDescription() { return "계속 올라오는 줄을 버티며 최대한 오래 생존"; }

    @Override public boolean isAvailable() { return true; }

    @Override public void start(ScreenManager screens) {
        Game game = new Game();
        game.addListener(new RiseListener(game));
        screens.startGame(game, getName());
    }

    private static final class RiseListener implements GameListener {
        private static final long RISE_INTERVAL_MS = 10_000L;

        private final Game game;
        private final Random random = new Random();
        private long elapsedMs;
        private int pending;

        private RiseListener(Game game) {
            this.game = game;
        }

        @Override public void onTick(long dtMs) {
            elapsedMs += dtMs;
            while (elapsedMs >= RISE_INTERVAL_MS) {
                elapsedMs -= RISE_INTERVAL_MS;
                pending++;
            }
        }

        @Override public void onBeforeSpawn() {
            for (int i = 0; i < pending && !game.gameOver; i++) {
                game.addGarbageRow(random.nextInt(Board.WIDTH));
            }
            pending = 0;
        }

        @Override public void onRestart() {
            elapsedMs = 0;
            pending = 0;
        }
    }
}
