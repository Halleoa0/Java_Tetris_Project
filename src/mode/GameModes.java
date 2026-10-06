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
                new SprintMode(),

                // TOP-OUT RUN mode
                new ComingSoonMode("TOP-OUT RUN", "계속 올라오는 줄을 버티며 최대한 오래 생존"),

                // vs CPU mode
                new ComingSoonMode("vs CPU", "CPU와 대결")
        );
    }
}
