package mode;

import app.ScreenManager;
/** 각 모드 별 클래스가 생성되면(GameMode 인터페이스 implements 해야함) 사라질 임시 클래스 */
public final class ComingSoonMode implements GameMode {
    private final String name;
    private final String description;

    public ComingSoonMode(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public boolean isAvailable() { return false; }
    @Override public void start(ScreenManager screens) { }
}
