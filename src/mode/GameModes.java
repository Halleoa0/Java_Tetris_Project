package mode;

import java.util.List;

public final class GameModes {
    private GameModes() { }

    public static List<GameMode> all() {
        return List.of(
                // CLASSIC mode
                new ClassicMode(),

                // ULTRA mode
                new UltraMode(),

                // 20L SPRINT mode
                new ComingSoonMode("20L SPRINT", "20줄을 가장 빠르게 지우기"),

                // TOP-OUT RUN mode
                new TopOutRunMode(),

                // vs CPU mode
                new ComingSoonMode("vs CPU", "CPU와 대결")
        );
    }
}
