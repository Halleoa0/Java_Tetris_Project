package settings;

import game.Tetromino;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * 환경설정 데이터 모델 (싱글톤), UI(SettingsPanel)와 게임/사운드 쪽은 이 클래스만
 * 값이 바뀌면 자동 저장 + 등록된 리스너 호출.
 */
public final class Settings {

    // 열거형 //

    //키 바인딩 대상 동작//
    public enum Action {
        MOVE_LEFT("왼쪽 이동", KeyEvent.VK_LEFT),
        MOVE_RIGHT("오른쪽 이동", KeyEvent.VK_RIGHT),
        SOFT_DROP("소프트 드롭", KeyEvent.VK_DOWN),
        ROTATE_CW("시계 회전", KeyEvent.VK_X),
        ROTATE_CCW("반시계 회전", KeyEvent.VK_Z),
        HARD_DROP("하드 드롭", KeyEvent.VK_SPACE),
        HOLD("홀드", KeyEvent.VK_C),
        RESTART("재시작", KeyEvent.VK_R);

        public final String label;
        public final int defaultKey;
        Action(String label, int defaultKey) { this.label = label; this.defaultKey = defaultKey; }
    }

    //난이도: 중력 배율(작을수록 빠름)과 시작 레벨//
    public enum Difficulty {
        EASY("쉬움", 1.4, 1),
        NORMAL("보통", 1.0, 1),
        HARD("어려움", 0.7, 3);

        public final String label;
        public final double gravityScale;
        public final int startLevel;
        Difficulty(String label, double gravityScale, int startLevel) {
            this.label = label; this.gravityScale = gravityScale; this.startLevel = startLevel;
        }
    }

    //색각 보정 모드. 모드별로 7종 미노 색 팔레트(순서: Tetromino.values())를 가짐//
    public enum ColorMode {
        OFF("끄기 (기본 색)", new int[] {0x4BCDEB, 0xF5D237, 0xAA55CD, 0x55D26E, 0xE64646, 0x4169E1, 0xF09137}),
        PROTANOPIA("적색약 (Protanopia)", OKABE_ITO),
        DEUTERANOPIA("녹색약 (Deuteranopia)", OKABE_ITO),
        TRITANOPIA("청황색약 (Tritanopia)", new int[] {0xE6194B, 0xF0F0F0, 0xB5179E, 0x3CB44B, 0x7A1F1F, 0x1B1B8F, 0xFF8C00}),
        HIGH_CONTRAST("고대비", new int[] {0x00FFFF, 0xFFFF00, 0xFF00FF, 0x00FF00, 0xFF0000, 0x3B5BFF, 0xFFA500});

        public final String label;
        public final int[] rgb; // Tetromino 선언 순서: I, O, T, S, Z, J, L
        ColorMode(String label, int[] rgb) { this.label = label; this.rgb = rgb; }
    }
    // Okabe-Ito 팔레트 기반 (적/녹색약 공용)
    private static final int[] OKABE_ITO = {0x56B4E9, 0xF0E442, 0xCC79A7, 0x009E73, 0xD55E00, 0x0072B2, 0xE69F00};

    //상수//

    public static final String DEFAULT_PROFILE = "Player1";
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".tetris2026", "settings.properties");
    private static final Settings INSTANCE = new Settings();

    public static Settings get() { return INSTANCE; }

    //상태//

    // 공통 설정
    private ColorMode colorMode = ColorMode.OFF;
    private Difficulty difficulty = Difficulty.NORMAL;
    private int masterVolume = 80, bgmVolume = 70, sfxVolume = 80; // 0~100
    private boolean muted = false;
    private boolean ghostPiece = true, showGrid = true;
    private int dasMs = 180, arrMs = 55; // GamePanel.tickHeldKeys 의 기본값과 동일
    // SDF
    private int sdf = 20;
    // 스폰 딜레이(ms)와 IHS/IRS 사용 여부
    private int spawnDelayMs = 0;
    private boolean ihs = true, irs = true;
    private boolean muteWhenUnfocused = true;

    // 사용자(프로필)별 키 세팅
    private final Map<String, EnumMap<Action, Integer>> profiles = new LinkedHashMap<>();
    private String currentProfile = DEFAULT_PROFILE;

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private boolean loading;

    private Settings() {
        profiles.put(DEFAULT_PROFILE, defaultKeys());
        load();
    }

    // 리스너//

    /** 설정이 바뀔 때마다 호출된다. (사운드 볼륨 즉시 반영, 색상 재로드 등에 사용) */
    public void addListener(Runnable r) { if (r != null) listeners.add(r); }
    public void removeListener(Runnable r) { listeners.remove(r); }

    private void changed() {
        if (loading) return;
        save();
        for (Runnable r : listeners) r.run();
    }

    //색맹 모드//

    public ColorMode colorMode() { return colorMode; }
    public void setColorMode(ColorMode m) { colorMode = m; changed(); }

    /** 현재 색맹 모드가 적용된 미노 색. Mino.png 스프라이트 대신 색으로 그릴 때 사용. */
    public Color minoColor(Tetromino t) { return new Color(colorMode.rgb[t.ordinal()]); }

    //난이도//

    public Difficulty difficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { difficulty = d; changed(); }

    //Game.tick 의 중력 간격(ms). 기존 식에 난이도 배율만 곱//
    public int gravityMs(int level) {
        //레벨이 올라갈수록 낙하 간격을 줄이고, 난이도가 높을수록 더 빠르게 낙하
        int base = 800 - (level - 1) * 60;
        return Math.max(40, (int) Math.round(base * difficulty.gravityScale));
    }

    /** 소프트 드롭 중 사용하는 중력 간격. SDF가 클수록 빠르게 내려감 */
    public int softDropGravityMs(int level) {
        return Math.max(16, gravityMs(level) / Math.max(1, sdf));
    }

    /** 레벨/난이도에 따라 락 딜레이도 조금씩 짧아지도록 함 */
    public int lockDelayMs(int level) {
        int base = (int) Math.round(500 * difficulty.gravityScale);
        int levelReduction = Math.max(0, level -1) * 10;
        return Math.max(100, base - levelReduction);
    }

    public int sdf() {
        return sdf;
    }
    public void setSdf(int value) { sdf = Math.max(1, Math.min(40, value)); changed(); }

    int spawnDelayMs() { return spawnDelayMs; }
    void setSpawnDelayMs(int value) { spawnDelayMs = Math.max(0, Math.min(100, value)); changed(); }
    boolean ihs() { return ihs; }
    void setIhs(boolean value) { ihs = value; changed(); }
    boolean irs() { return irs; }
    void setIrs(boolean value) { irs = value; changed(); }

    int startLevel() { return difficulty.startLevel; }

    //사운드 (0~100)//

    public int masterVolume() { return masterVolume; }
    public int bgmVolume() { return bgmVolume; }
    public int sfxVolume() { return sfxVolume; }
    public boolean muted() { return muted; }
    public boolean muteWhenUnfocused() { return muteWhenUnfocused; }

    public void setMasterVolume(int v) { masterVolume = clamp(v, 0, 100); changed(); }
    public void setBgmVolume(int v) { bgmVolume = clamp(v, 0, 100); changed(); }
    public void setSfxVolume(int v) { sfxVolume = clamp(v, 0, 100); changed(); }
    public void setMuted(boolean m) { muted = m; changed(); }
    public void setMuteWhenUnfocused(boolean b) { muteWhenUnfocused = b; changed(); }

    //사운드 구현 시 사용할 최종 배율 0.0~1.0 (마스터 × 배경음, 음소거 반영)//
    public float bgmGain() { return muted ? 0f : masterVolume * bgmVolume / 10000f; }
    public float sfxGain() { return muted ? 0f : masterVolume * sfxVolume / 10000f; }

    /** dB 변환이 필요한 Clip/FloatControl(MASTER_GAIN)용. gain 0 이면 -80dB. */
    public static float toDecibel(float gain) { return gain <= 0.0001f ? -80f : (float) (20.0 * Math.log10(gain)); }

    //게임플레이 옵션//

    public boolean ghostPiece() { return ghostPiece; }
    public boolean showGrid() { return showGrid; }
    public int dasMs() { return dasMs; }
    public int arrMs() { return arrMs; }

    public void setGhostPiece(boolean b) { ghostPiece = b; changed(); }
    public void setShowGrid(boolean b) { showGrid = b; changed(); }
    public void setDasMs(int ms) { dasMs = clamp(ms, 50, 400); changed(); }
    public void setArrMs(int ms) { arrMs = clamp(ms, 0, 120); changed(); }

    //사용자 프로필 / 키 세팅//

    public String currentProfile() { return currentProfile; }
    public List<String> profileNames() { return new ArrayList<>(profiles.keySet()); }

    //현재 프로필에서 해당 동작의 키코드 (KeyEvent.VK_*)//
    public int keyOf(Action a) { return profiles.get(currentProfile).get(a); }

    //Swing KeyBinding 문자열용. 예: KeyStroke.getKeyStroke(Settings.get().keyOf(a), 0) //
    public javax.swing.KeyStroke pressed(Action a) { return javax.swing.KeyStroke.getKeyStroke(keyOf(a), 0, false); }
    public javax.swing.KeyStroke released(Action a) { return javax.swing.KeyStroke.getKeyStroke(keyOf(a), 0, true); }

    public String keyText(Action a) { return KeyEvent.getKeyText(keyOf(a)); }

    //예약 키(일시정지/메뉴)는 바인딩 불가//
    public static boolean isReserved(int keyCode) {
        return keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_M
                || keyCode == KeyEvent.VK_UNDEFINED;
    }

    /**
     * 키 변경. 이미 다른 동작이 쓰는 키면 두 동작의 키를 서로 교환
     * @return 바인딩 성공 여부 (예약 키면 false)
     */
    public boolean setKey(Action a, int keyCode) {
        if (isReserved(keyCode)) return false;
        EnumMap<Action, Integer> map = profiles.get(currentProfile);
        int old = map.get(a);
        for (Action other : Action.values()) {
            if (other != a && map.get(other) == keyCode) { map.put(other, old); break; }
        }
        map.put(a, keyCode);
        changed();
        return true;
    }

    public void resetKeys() { profiles.put(currentProfile, defaultKeys()); changed(); }

    // 새 프로필 생성 (현재 프로필 키 복사) 후 전환. 이름이 비었거나 중복이면 false//
    public boolean createProfile(String name) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty() || name.length() > 16 || name.matches(".*[=:\\s#!\\\\].*") || profiles.containsKey(name)) return false;
        profiles.put(name, new EnumMap<>(profiles.get(currentProfile)));
        currentProfile = name;
        changed();
        return true;
    }

    public boolean switchProfile(String name) {
        if (!profiles.containsKey(name)) return false;
        currentProfile = name;
        changed();
        return true;
    }

    // 마지막 1개는 삭제 불가//
    public boolean deleteProfile(String name) {
        if (profiles.size() <= 1 || !profiles.containsKey(name)) return false;
        profiles.remove(name);
        if (currentProfile.equals(name)) currentProfile = profiles.keySet().iterator().next();
        changed();
        return true;
    }

    private static EnumMap<Action, Integer> defaultKeys() {
        EnumMap<Action, Integer> m = new EnumMap<>(Action.class);
        for (Action a : Action.values()) m.put(a, a.defaultKey);
        return m;
    }

    //전체 초기화//

    //키 세팅(프로필)은 유지하고 나머지 설정만 기본값으로//
    public void resetAllExceptKeys() {
        colorMode = ColorMode.OFF; difficulty = Difficulty.NORMAL;
        masterVolume = 80; bgmVolume = 70; sfxVolume = 80; muted = false;
        ghostPiece = true; showGrid = true; dasMs = 180; arrMs = 55; muteWhenUnfocused = true;
        changed();
    }

    // 저장 / 불러오기//

    public void save() {
        Properties p = new Properties();
        p.setProperty("colorMode", colorMode.name());
        p.setProperty("difficulty", difficulty.name());
        p.setProperty("volume.master", String.valueOf(masterVolume));
        p.setProperty("volume.bgm", String.valueOf(bgmVolume));
        p.setProperty("volume.sfx", String.valueOf(sfxVolume));
        p.setProperty("muted", String.valueOf(muted));
        p.setProperty("muteWhenUnfocused", String.valueOf(muteWhenUnfocused));
        p.setProperty("ghostPiece", String.valueOf(ghostPiece));
        p.setProperty("showGrid", String.valueOf(showGrid));
        p.setProperty("das", String.valueOf(dasMs));
        p.setProperty("arr", String.valueOf(arrMs));
        p.setProperty("sdf", String.valueOf(sdf));
        p.setProperty("spawnDelayMs", String.valueOf(spawnDelayMs));
        p.setProperty("ihs", String.valueOf(ihs));
        p.setProperty("irs", String.valueOf(irs));
        p.setProperty("profile.current", currentProfile);
        p.setProperty("profile.names", String.join(",", profiles.keySet()));
        for (var e : profiles.entrySet())
            for (var k : e.getValue().entrySet())
                p.setProperty("key." + e.getKey() + "." + k.getKey().name(), String.valueOf(k.getValue()));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Tetris settings"); }
        } catch (IOException e) {
            System.err.println("설정 저장 실패: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) { p.load(in); }
        catch (IOException e) { System.err.println("설정 불러오기 실패: " + e.getMessage()); return; }

        loading = true;
        try {
            colorMode = parseEnum(ColorMode.class, p.getProperty("colorMode"), ColorMode.OFF);
            difficulty = parseEnum(Difficulty.class, p.getProperty("difficulty"), Difficulty.NORMAL);
            masterVolume = clamp(parseInt(p.getProperty("volume.master"), 80), 0, 100);
            bgmVolume = clamp(parseInt(p.getProperty("volume.bgm"), 70), 0, 100);
            sfxVolume = clamp(parseInt(p.getProperty("volume.sfx"), 80), 0, 100);
            muted = Boolean.parseBoolean(p.getProperty("muted", "false"));
            muteWhenUnfocused = Boolean.parseBoolean(p.getProperty("muteWhenUnfocused", "true"));
            ghostPiece = Boolean.parseBoolean(p.getProperty("ghostPiece", "true"));
            showGrid = Boolean.parseBoolean(p.getProperty("showGrid", "true"));
            dasMs = clamp(parseInt(p.getProperty("das"), 180), 50, 400);
            arrMs = clamp(parseInt(p.getProperty("arr"), 55), 0, 120);
            sdf = clamp(parseInt(p.getProperty("sdf"), 20), 1, 40);
            spawnDelayMs = clamp(parseInt(p.getProperty("spawnDelayMs"), 0), 0, 100);
            ihs = Boolean.parseBoolean(p.getProperty("ihs", "true"));
            irs = Boolean.parseBoolean(p.getProperty("irs", "true"));


            String names = p.getProperty("profile.names", DEFAULT_PROFILE);
            profiles.clear();
            for (String name : names.split(",")) {
                if (name.isBlank()) continue;
                EnumMap<Action, Integer> m = defaultKeys();
                for (Action a : Action.values())
                    m.put(a, parseInt(p.getProperty("key." + name + "." + a.name()), a.defaultKey));
                profiles.put(name, m);
            }
            if (profiles.isEmpty()) profiles.put(DEFAULT_PROFILE, defaultKeys());
            currentProfile = p.getProperty("profile.current", "");
            if (!profiles.containsKey(currentProfile)) currentProfile = profiles.keySet().iterator().next();
        } finally {
            loading = false;
        }
    }

    // 유틸 //

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int parseInt(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    private static <E extends Enum<E>> E parseEnum(Class<E> c, String s, E def) {
        try { return s == null ? def : Enum.valueOf(c, s); } catch (IllegalArgumentException e) { return def; }
    }
}
