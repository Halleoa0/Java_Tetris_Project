package settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 모드별 최고 기록(걸린 시간)을 저장한다. (싱글톤: Records.get())
 * - 사용자(Settings의 현재 프로필)마다 따로 저장된다.
 * - 파일: 사용자 홈 폴더의 .tetris2026/records.properties (Settings와 같은 폴더)
 * - 시간은 ms 단위이고, 짧을수록 좋은 기록이다.
 */
public final class Records {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".tetris2026", "records.properties");
    private static final Records INSTANCE = new Records();

    public static Records get() { return INSTANCE; }

    private final Properties props = new Properties();

    private Records() { load(); }

    /** 20L 스프린트처럼 목표 줄 수가 있는 모드의 기록 이름. 예: sprint20 */
    public static String sprintKey(int lines) { return "sprint" + lines; }

    private static String key(String mode) { return "time." + Settings.get().currentProfile() + "." + mode; }

    /** 현재 사용자의 최고 기록(ms). 기록이 없으면 -1. */
    public synchronized int bestTimeMs(String mode) {
        try {
            int ms = Integer.parseInt(props.getProperty(key(mode), "").trim());
            return ms > 0 ? ms : -1;
        } catch (NumberFormatException e) {
            return -1;   // 값이 없거나 파일이 손상된 경우
        }
    }

    /**
     * 이번 기록을 제출한다. 최고 기록(더 짧은 시간)이면 저장하고 true를 돌려준다.
     * 같은 시간은 갱신으로 보지 않는다.
     */
    public synchronized boolean submitTime(String mode, int timeMs) {
        if (timeMs <= 0) return false;
        int best = bestTimeMs(mode);
        if (best > 0 && timeMs >= best) return false;
        props.setProperty(key(mode), String.valueOf(timeMs));
        save();
        return true;
    }

    // ===== 점수 기록 (Classic / Ultra / Top-Out Run) =====

    /** 모드 화면 이름(GamePanel에 넘기는 이름) -> 점수 기록 키. 점수 기록이 없는 모드는 null. */
    public static String scoreKeyForGameName(String gameName) {
        if (gameName == null) return null;
        switch (gameName) {
            case "클래식": return "classic";
            case "울트라": return "ultra";
            case "탑-아웃 런": return "topout";
            default: return null;
        }
    }

    private static String scoreKey(String profile, String mode) { return "score." + profile + "." + mode; }

    /** 현재 사용자의 최고 점수. 기록이 없으면 -1. */
    public synchronized int bestScore(String mode) { return bestScore(Settings.get().currentProfile(), mode); }

    /** 지정한 사용자의 최고 점수. 기록이 없으면 -1. */
    public synchronized int bestScore(String profile, String mode) {
        try {
            int v = Integer.parseInt(props.getProperty(scoreKey(profile, mode), "").trim());
            return v > 0 ? v : -1;
        } catch (NumberFormatException e) { return -1; }
    }

    /** 점수를 제출. 최고 점수면 저장하고 true. */
    public synchronized boolean submitScore(String mode, int score) {
        if (mode == null || score <= 0) return false;
        if (score <= bestScore(mode)) return false;
        props.setProperty(scoreKey(Settings.get().currentProfile(), mode), String.valueOf(score));
        save();
        return true;
    }

    /** 지정한 사용자의 스프린트 최고 시간(ms). 없으면 -1. */
    public synchronized int bestTimeMs(String profile, String mode) {
        try {
            int ms = Integer.parseInt(props.getProperty("time." + profile + "." + mode, "").trim());
            return ms > 0 ? ms : -1;
        } catch (NumberFormatException e) { return -1; }
    }

    // ===== vs CPU 매치 승리 수 (난이도별) =====

    public synchronized int cpuWins(String profile, String difficulty) {
        try { return Math.max(0, Integer.parseInt(props.getProperty("cpuwin." + profile + "." + difficulty, "0").trim())); }
        catch (NumberFormatException e) { return 0; }
    }

    public synchronized void addCpuWin(String difficulty) {
        String p = Settings.get().currentProfile();
        props.setProperty("cpuwin." + p + "." + difficulty, String.valueOf(cpuWins(p, difficulty) + 1));
        save();
    }


    /** 1234567 ms -> "20:34.56" (분:초.1/100초). 1/100초 아래는 버린다. */
    public static String formatTime(int ms) {
        int total = Math.max(0, ms);
        return String.format("%02d:%02d.%02d", total / 60000, total / 1000 % 60, total / 10 % 100);
    }

    private void load() {
        if (!Files.exists(FILE)) return;
        try (InputStream in = Files.newInputStream(FILE)) { props.load(in); }
        catch (IOException e) { System.err.println("기록 불러오기 실패: " + e.getMessage()); }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) { props.store(out, "Tetris records"); }
        } catch (IOException e) {
            System.err.println("기록 저장 실패: " + e.getMessage());
        }
    }
}