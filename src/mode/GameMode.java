package mode;

import app.ScreenManager;

public interface GameMode {
    String getName();
    String getDescription();
    boolean isAvailable();
    void start(ScreenManager screens);
}
