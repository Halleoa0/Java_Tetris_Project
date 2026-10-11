package mode;

import app.ScreenManager;

/// CPU 봇과 1:1 대전. 바로 시작하지 않고 CPU 난이도 선택 화면(CpuSelectPanel)을 먼저 보여준다.
public final class VsCpuMode implements GameMode {
    @Override public String getName() { return "vs CPU"; }
    @Override public String getDescription() { return "CPU와 대결 (3판 2선승)"; }
    @Override public boolean isAvailable() { return true; }
    @Override public void start(ScreenManager screens) {
        screens.showCpuSelect();
    }
}
