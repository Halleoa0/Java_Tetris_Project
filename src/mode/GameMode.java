package mode;

import app.ScreenManager;

/** 게임 모드 인터페이스. 각 모드에 대한 이름, 설명, available 여부를 표기 */
public interface GameMode {
    String getName();
    String getDescription();
    boolean isAvailable();
    void start(ScreenManager screens);
}
