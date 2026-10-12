package mode;

import app.ScreenManager;
import game.Game;
import settings.Records;

public final class ClassicMode implements GameMode {
    @Override public String getName() { return "CLASSIC"; }
    @Override public String getDescription() {
        int best = Records.get().bestScore("classic");
        String text = "기본 테트리스";
        return best > 0 ? text + "  |  BEST " + String.format("%,d", best) : text;
    }
    @Override public boolean isAvailable() { return true; }
    @Override public void start(ScreenManager screens) {
        screens.startGame(new Game(), "클래식");
    }
}


