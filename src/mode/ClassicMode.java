package mode;

import app.ScreenManager;

public final class ClassicMode implements GameMode {
    @Override public String getName() { return "CLASSIC"; }
    @Override public String getDescription() { return "기본 테트리스"; }
    @Override public boolean isAvailable() { return true; }
    @Override public void start(ScreenManager screens) { screens.startGame(); }


}


