package settings;

import settings.Settings.Action;
import settings.Settings.ColorMode;
import settings.Settings.Difficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


/// 기존 Settings에 있던 load()와 save()를 클래스 분리를 통해 여기서 작동시킵니다.
final class SettingsStore {
    private final Path file = Path.of(System.getProperty("user.home"), ".tetris2026", "settings.properties");

    record Data(
            ColorMode colorMode, Difficulty difficulty,
            int masterVolume, int bgmVolume, int sfxVolume,
            boolean muted, boolean muteWhenUnfocused, boolean ghostPiece, boolean showGrid,
            int dasMs, int arrMs, int sdf, int spawnDelayMs,
            boolean ihs, boolean irs,
            Map<String, EnumMap<Action, Integer>> profiles, String currentProfile) {
    }

    void save(Data data) {
        Properties p = new Properties();
        p.setProperty("colorMode", data.colorMode().name());
        p.setProperty("difficulty", data.difficulty().name());
        p.setProperty("volume.master", String.valueOf(data.masterVolume()));
        p.setProperty("volume.bgm", String.valueOf(data.bgmVolume()));
        p.setProperty("volume.sfx", String.valueOf(data.sfxVolume()));
        p.setProperty("muted", String.valueOf(data.muted()));
        p.setProperty("muteWhenUnfocused", String.valueOf(data.muteWhenUnfocused()));
        p.setProperty("ghostPiece", String.valueOf(data.ghostPiece()));
        p.setProperty("showGrid", String.valueOf(data.showGrid()));
        p.setProperty("das", String.valueOf(data.dasMs()));
        p.setProperty("arr", String.valueOf(data.arrMs()));
        p.setProperty("sdf", String.valueOf(data.sdf()));
        p.setProperty("spawnDelayMs", String.valueOf(data.spawnDelayMs()));
        p.setProperty("ihs", String.valueOf(data.ihs()));
        p.setProperty("irs", String.valueOf(data.irs()));
        p.setProperty("profile.current", data.currentProfile());
        p.setProperty("profile.names", String.join(",", data.profiles().keySet()));
        for (var profile : data.profiles().entrySet()) {
            for (var key : profile.getValue().entrySet()) {
                p.setProperty(keyProperty(profile.getKey(), key.getKey()), String.valueOf(key.getValue()));
            }
        }
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "Tetris settings");
            }
        } catch (IOException e) {
            System.err.println("설정 저장 실패: " + e.getMessage());
        }
    }

    Data load(Data defaults) {
        if (!Files.exists(file)) return defaults;

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("설정 불러오기 실패: " + e.getMessage());
            return defaults;
        }

        Map<String, EnumMap<Action, Integer>> profiles = new LinkedHashMap<>();
        String names = p.getProperty("profile.names", defaults.currentProfile());
        for (String name : names.split(",")) {
            if (name.isBlank()) continue;
            EnumMap<Action, Integer> keys = new EnumMap<>(Action.class);
            for (Action action : Action.values()) {
                keys.put(action, parseInt(p.getProperty(keyProperty(name, action)), action.defaultKey));
            }
            profiles.put(name, keys);
        }
        if (profiles.isEmpty()) profiles.putAll(defaults.profiles());

        String currentProfile = p.getProperty("profile.current", "");
        if (!profiles.containsKey(currentProfile)) currentProfile = profiles.keySet().iterator().next();

        return new Data(
                parseEnum(p.getProperty("colorMode"), defaults.colorMode()),
                parseEnum(p.getProperty("difficulty"), defaults.difficulty()),
                readInt(p, "volume.master", defaults.masterVolume(), 0, 100),
                readInt(p, "volume.bgm", defaults.bgmVolume(), 0, 100),
                readInt(p, "volume.sfx", defaults.sfxVolume(), 0, 100),
                readBoolean(p, "muted", defaults.muted()),
                readBoolean(p, "muteWhenUnfocused", defaults.muteWhenUnfocused()),
                readBoolean(p, "ghostPiece", defaults.ghostPiece()),
                readBoolean(p, "showGrid", defaults.showGrid()),
                readInt(p, "das", defaults.dasMs(), 50, 400),
                readInt(p, "arr", defaults.arrMs(), 0, 120),
                readInt(p, "sdf", defaults.sdf(), 1, 40),
                readInt(p, "spawnDelayMs", defaults.spawnDelayMs(), 0, 100),
                readBoolean(p, "ihs", defaults.ihs()),
                readBoolean(p, "irs", defaults.irs()),
                profiles, currentProfile);
    }

    private static String keyProperty(String profile, Action action) {
        return "key." + profile + "." + action.name();
    }

    private static boolean readBoolean(Properties p, String key, boolean fallback) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(fallback)));
    }

    private static int readInt(Properties p, String key, int fallback, int min, int max) {
        return Math.max(min, Math.min(max, parseInt(p.getProperty(key), fallback)));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E parseEnum(String value, E fallback) {
        try {
            return value == null ? fallback : Enum.valueOf(fallback.getDeclaringClass(), value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
