import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 테트리스 배경음악(BGM)과 효과음(SFX)을 총괄 관리한다.
 * 자바 표준 javax.sound.sampled.Clip 기반으로 동작하며 외부 라이브러리 없이 실행된다.
 */
final class AudioManager {

    // 음원 파일명 정의 //
    // BGM 후보 2곡
    private static final String BGM_A = "bgm-a-grid-pulse";   // A: 신스 펄스 (128 BPM)
    private static final String BGM_B = "bgm-b-block-groove"; // B: 칩 그루브 (128 BPM)

    // SFX 12종
    private static final String[] SFX_NAMES = {
            "menu-select",    // 메뉴 버튼 및 일시정지 옵션 선택
            "move",           // 좌우 블록 이동
            "rotate",         // 블록 회전
            "softdrop",       // 소프트 드롭 (아래 이동)
            "lock",           // 블록 바닥 안착/고정
            "harddrop",       // 하드 드롭 (스페이스바 즉시 낙하)
            "hold",           // 블록 홀드 교체
            "lineclear",      // 일반 1~3줄 라인 삭제
            "tetris",         // 4줄 동시 삭제 (테트리스)
            "levelup",        // 레벨 상승
            "gameover",       // 게임 오버
            "tutorial-clear"  // 튜토리얼 완료
    };

    // 싱글톤 인스턴스 (상수 선언 후 초기화)
    private static final AudioManager INSTANCE = new AudioManager();

    static AudioManager get() { return INSTANCE; }

    // 오디오 클립 저장소 및 재생 상태 //
    private final Map<String, Clip> bgmClips = new HashMap<>();
    private final Map<String, Clip> sfxClips = new HashMap<>();
    // 이동/드롭 연타 시 소리 중첩 완화용 시간 기록표 (ms)
    private final Map<String, Long> lastSoundTime = new HashMap<>();
    private final Random random = new Random();

    private Clip currentBgmClip;
    private String currentBgmName;
    private String lastPlayedGameBgm; // 직전 인게임 BGM (중복 방지용)
    private String currentMode = "menu"; // "menu", "tutorial", "game"
    private boolean bgmPaused = false;
    private boolean windowActive = true;
    private Timer levelUpTimer;

    // 생성자: BGM 2곡과 SFX 12종을 메모리에 미리 로드 //
    private AudioManager() {
        loadBgm(BGM_A);
        loadBgm(BGM_B);

        for (String sfxName : SFX_NAMES) {
            Clip clip = loadClip("sfx", "sfx-" + sfxName + ".wav");
            if (clip != null) sfxClips.put(sfxName, clip);
        }

        // Settings 볼륨 슬라이더 변경 시 즉시 반영
        Settings.get().addListener(this::refreshVolume);
    }

    /// 오디오 파일을 찾아 Clip 객체로 로딩
    private Clip loadClip(String subFolder, String fileName) {
        // 1순위: 로컬 전용 audio-candidates/Audio/, 2순위: 루트 Audio/
        File file = new File("audio-candidates/Audio/" + subFolder + "/" + fileName);
        if (!file.exists()) {
            file = new File("Audio/" + subFolder + "/" + fileName);
        }
        if (!file.exists()) return null; // 파일이 없어도 게임은 계속 실행

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            System.err.println("사운드 파일 로딩 실패: " + file.getPath() + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /// BGM 파일 로딩 및 종료 이벤트 리스너 등록
    private void loadBgm(String bgmName) {
        Clip clip = loadClip("bgm", bgmName + ".wav");
        if (clip == null) return;

        bgmClips.put(bgmName, clip);

        // 곡이 끝까지 재생되었을 때 다음 곡으로 자동 전환
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                SwingUtilities.invokeLater(() -> {
                    if (!bgmPaused && clip == currentBgmClip && clip.getFramePosition() >= clip.getFrameLength()) {
                        onBgmFinished();
                    }
                });
            }
        });
    }

    // BGM 재생 및 상태 제어 //

    /// 메인 메뉴, 설정, 상점 BGM 재생 (Block Groove)
    void playMenuBgm() {
        currentMode = "menu";
        playBgmInternal(BGM_B, false);
    }

    /// 튜토리얼 BGM 재생 (Grid Pulse)
    void playTutorialBgm() {
        currentMode = "tutorial";
        playBgmInternal(BGM_A, true);
    }

    /// 일반 게임 플레이 BGM 재생 (A/B 중 무작위 선택)
    void startGameBgm() {
        currentMode = "game";
        playBgmInternal(chooseRandomGameBgm(), true);
    }

    /// 인게임 BGM 무작위 선택. 직전에 나온 곡은 연속으로 나오지 않게 제외
    private String chooseRandomGameBgm() {
        String[] options = { BGM_A, BGM_B };

        if (lastPlayedGameBgm != null && lastPlayedGameBgm.equals(BGM_A)) {
            lastPlayedGameBgm = BGM_B;
            return BGM_B;
        } else if (lastPlayedGameBgm != null && lastPlayedGameBgm.equals(BGM_B)) {
            lastPlayedGameBgm = BGM_A;
            return BGM_A;
        }

        String selected = options[random.nextInt(options.length)];
        lastPlayedGameBgm = selected;
        return selected;
    }

    /// 한 곡 재생이 끝났을 때 호출. 인게임은 다른 곡으로 자동 전환
    private void onBgmFinished() {
        if ("game".equals(currentMode)) {
            playBgmInternal(chooseRandomGameBgm(), true);
        } else if ("tutorial".equals(currentMode)) {
            playBgmInternal(BGM_A, true);
        } else {
            playBgmInternal(BGM_B, true);
        }
    }

    /// BGM 재생 내부 함수
    private void playBgmInternal(String bgmName, boolean restart) {
        Clip nextClip = bgmClips.get(bgmName);
        if (nextClip == null) return;

        // 이미 같은 곡이 재생 중이고 처음부터 재생이 아니면 유지
        if (!restart && nextClip == currentBgmClip && nextClip.isRunning()) return;

        if (currentBgmClip != null && currentBgmClip.isRunning()) {
            currentBgmClip.stop();
        }

        currentBgmClip = nextClip;
        currentBgmName = bgmName;
        bgmPaused = false;
        currentBgmClip.setFramePosition(0);

        applyVolumeToClip(currentBgmClip, calculateBgmGain());
        if (canOutputMusic()) {
            currentBgmClip.start();
        }
    }

    /// BGM 일시정지 (재생 위치 보존)
    void pauseBgm() {
        bgmPaused = true;
        if (currentBgmClip != null && currentBgmClip.isRunning()) {
            currentBgmClip.stop();
        }
    }

    /// BGM 재개 (멈췄던 위치부터 이어 재생)
    void resumeBgm() {
        bgmPaused = false;
        if (currentBgmClip != null && canOutputMusic()) {
            currentBgmClip.start();
        }
    }

    /// BGM 완전 정지
    void stopBgm() {
        bgmPaused = false;
        if (currentBgmClip != null) {
            currentBgmClip.stop();
            currentBgmClip.setFramePosition(0);
        }
    }

    // 효과음(SFX) 재생 //

    /// 메뉴 버튼 클릭 / 항목 변경음
    void playMenuSelect() { playSound("menu-select"); }

    /// 좌우 이동음 (연타 시 80ms 간격 제한)
    void playMove() { playSoundWithInterval("move", 80); }

    /// 블록 회전음
    void playRotate() { playSound("rotate"); }

    /// 소프트 드롭음 (100ms 간격 제한)
    void playSoftDrop() { playSoundWithInterval("softdrop", 100); }

    /// 블록 바닥 안착/고정음
    void playLock() { playSound("lock"); }

    /// 하드 드롭 즉시 낙하음
    void playHardDrop() { playSound("harddrop"); }

    /// 블록 홀드 교체음
    void playHold() { playSound("hold"); }

    /// 라인 삭제음 (4줄은 테트리스 전용 효과음)
    void playLineClear(int lines) {
        if (lines >= 4) playSound("tetris");
        else if (lines > 0) playSound("lineclear");
    }

    /// 레벨업 효과음 (줄 삭제음과 겹치지 않게 300ms 후 재생)
    void playLevelUpDelayed() {
        if (levelUpTimer != null) levelUpTimer.stop();
        levelUpTimer = new Timer(300, e -> playSound("levelup"));
        levelUpTimer.setRepeats(false);
        levelUpTimer.start();
    }

    /// 게임 오버 효과음 (BGM 정지 후 재생)
    void playGameOver() {
        stopBgm();
        playSound("gameover");
    }

    /// 튜토리얼 단계/전체 완료음
    void playTutorialClear() { playSound("tutorial-clear"); }

    /// 단발성 효과음 재생
    private void playSound(String sfxName) {
        if (!canOutputSounds()) return;
        Clip clip = sfxClips.get(sfxName);
        if (clip == null) return;

        clip.stop();
        clip.setFramePosition(0);
        applyVolumeToClip(clip, Settings.get().sfxGain());
        clip.start();
    }

    /// 연타 조작(이동, 소프트 드롭)의 효과음 중첩 완화
    private void playSoundWithInterval(String sfxName, int minIntervalMs) {
        long now = System.currentTimeMillis();
        long last = lastSoundTime.getOrDefault(sfxName, 0L);
        if (now - last < minIntervalMs) return;

        lastSoundTime.put(sfxName, now);
        playSound(sfxName);
    }

    // 볼륨 및 사운드 출력 제어 //

    /// 창 활성화/비활성화 상태 설정 (창 포커스 아웃 음소거 연동)
    void setWindowActive(boolean active) {
        this.windowActive = active;
        refreshVolume();
    }

    private boolean canOutputSounds() {
        boolean allowed = windowActive || !Settings.get().muteWhenUnfocused();
        return allowed && Settings.get().sfxGain() > 0.0001f;
    }

    private boolean canOutputMusic() {
        boolean allowed = windowActive || !Settings.get().muteWhenUnfocused();
        return allowed && calculateBgmGain() > 0.0001f;
    }

    /// 모드별 BGM 게인 계산 (튜토리얼은 설명 집중을 위해 60% 볼륨)
    private float calculateBgmGain() {
        float scale = "tutorial".equals(currentMode) ? 0.60f : 1.00f;
        return Settings.get().bgmGain() * scale;
    }

    /// Settings 볼륨 변경 시 실시간 반영
    private void refreshVolume() {
        if (currentBgmClip != null) {
            applyVolumeToClip(currentBgmClip, calculateBgmGain());
            if (!canOutputMusic() || bgmPaused) {
                if (currentBgmClip.isRunning()) currentBgmClip.stop();
            } else {
                if (!currentBgmClip.isRunning()) currentBgmClip.start();
            }
        }

        for (Clip clip : sfxClips.values()) {
            applyVolumeToClip(clip, Settings.get().sfxGain());
        }
    }

    /// 0.0~1.0 배율을 데시벨(dB)로 변환해 Clip에 적용
    private void applyVolumeToClip(Clip clip, float gain) {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;

        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float db = Settings.toDecibel(gain);
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
    }
}
