package settings;

import game.Tetromino;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;


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

    public static final String DEFAULT_PROFILE = KeyBindingProfiles.DEFAULT_PROFILE;
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
    private final KeyBindingProfiles keyBindings = new KeyBindingProfiles();

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final SettingsStore store = new SettingsStore();

    private Settings() {
        load();
    }

    // 리스너//

    /** 설정이 바뀔 때마다 호출된다. (사운드 볼륨 즉시 반영, 색상 재로드 등에 사용) */
    public void addListener(Runnable r) { if (r != null) listeners.add(r); }
    public void removeListener(Runnable r) { listeners.remove(r); }

    private void changed() {
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

    public int sdf() {
        return sdf;
    }
    public void setSdf(int value) { sdf = Math.max(1, Math.min(40, value)); changed(); }

    public int spawnDelayMs() { return spawnDelayMs; }
    public void setSpawnDelayMs(int value) { spawnDelayMs = Math.max(0, Math.min(100, value)); changed(); }
    public boolean ihs() { return ihs; }
    public void setIhs(boolean value) { ihs = value; changed(); }
    public boolean irs() { return irs; }
    public void setIrs(boolean value) { irs = value; changed(); }

    public int startLevel() { return difficulty.startLevel; }

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

    public String currentProfile() { return keyBindings.currentProfile(); }
    public List<String> profileNames() { return keyBindings.profileNames(); }

    //현재 프로필에서 해당 동작의 키코드 (KeyEvent.VK_*)//
    public int keyOf(Action a) { return keyBindings.keyOf(a); }

    //Swing KeyBinding 문자열용. 예: KeyStroke.getKeyStroke(Settings.get().keyOf(a), 0) //
    public javax.swing.KeyStroke pressed(Action a) { return javax.swing.KeyStroke.getKeyStroke(keyOf(a), 0, false); }
    public javax.swing.KeyStroke released(Action a) { return javax.swing.KeyStroke.getKeyStroke(keyOf(a), 0, true); }

    public String keyText(Action a) { return KeyEvent.getKeyText(keyOf(a)); }

    public static boolean isReserved(int keyCode) {
        return KeyBindingProfiles.isReserved(keyCode);
    }

    /** 키를 변경하고, 같은 키를 쓰는 다른 동작이 있으면 기존 키와 교환한다. */
    public boolean setKey(Action action, int keyCode) {
        if (!keyBindings.setKey(action, keyCode)) return false;
        changed();
        return true;
    }

    public void resetKeys() {
        keyBindings.resetKeys();
        changed();
    }

    public boolean createProfile(String name) {
        if (!keyBindings.createProfile(name)) return false;
        changed();
        return true;
    }

    public boolean switchProfile(String name) {
        if (!keyBindings.switchProfile(name)) return false;
        changed();
        return true;
    }

    public boolean deleteProfile(String name) {
        if (!keyBindings.deleteProfile(name)) return false;
        changed();
        return true;
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
        store.save(settingsData());
    }

    private void load() {
        SettingsStore.Data data = store.load(settingsData());
        colorMode = data.colorMode();
        difficulty = data.difficulty();
        masterVolume = data.masterVolume();
        bgmVolume = data.bgmVolume();
        sfxVolume = data.sfxVolume();
        muted = data.muted();
        muteWhenUnfocused = data.muteWhenUnfocused();
        ghostPiece = data.ghostPiece();
        showGrid = data.showGrid();
        dasMs = data.dasMs();
        arrMs = data.arrMs();
        sdf = data.sdf();
        spawnDelayMs = data.spawnDelayMs();
        ihs = data.ihs();
        irs = data.irs();
        keyBindings.restore(data.profiles(), data.currentProfile());
    }

    private SettingsStore.Data settingsData() {
        return new SettingsStore.Data(
                colorMode, difficulty, masterVolume, bgmVolume, sfxVolume,
                muted, muteWhenUnfocused, ghostPiece, showGrid,
                dasMs, arrMs, sdf, spawnDelayMs, ihs, irs,
                keyBindings.snapshot(), keyBindings.currentProfile());
    }

    // 유틸 //

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
