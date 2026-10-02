package audio;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import settings.Settings;


/**
 * 테트리스 배경음악(BGM)과 효과음(SFX)을 총괄 관리한다.
 * 자바 표준 javax.sound.sampled.Clip 기반으로 동작하며 외부 라이브러리 없이 실행된다.
 */
public final class AudioManager {

    // 음원 파일명 정의 //
    private static final String BGM_A = "bgm-a-grid-pulse";   // 그리드 펄스 (신스)
    private static final String BGM_B = "bgm-b-block-groove"; // 블록 그루브 (칩튠)

    // 효과음 파일명 목록 //
    private static final String[] SFX_NAMES = {
            "menu-select",    // 메뉴 선택
            "move",           // 블록 이동
            "rotate",         // 블록 회전
            "softdrop",       // 소프트 드롭
            "lock",           // 블록 안착
            "harddrop",       // 하드 드롭
            "hold",           // 홀드
            "lineclear",      // 라인 삭제 (1~3줄)
            "tetris",         // 테트리스 (4줄 삭제)
            "levelup",        // 레벨 상승
            "gameover",       // 게임 오버
            "tutorial-clear"  // 튜토리얼 완료
    };

    // 싱글톤 인스턴스
    private static final AudioManager INSTANCE = new AudioManager();

    public static AudioManager get() { return INSTANCE; }

    // 오디오 클립 저장소 및 재생 상태 //
    private final Map<String, Clip> bgmClips = new HashMap<>();
    private final Map<String, Clip> sfxClips = new HashMap<>();
    // 이동/소프트 드롭 연타 시 효과음 중첩 완화용 시간 기록표 (ms)
    private final Map<String, Long> lastSoundTime = new HashMap<>();
    private final Random random = new Random();

    private Clip currentBgmClip;
    private String currentBgmName;
    private String lastPlayedGameBgm; // 직전 BGM (연속 재생 방지)
    private String currentMode = "menu"; // "menu", "tutorial", "game"
    private boolean bgmPaused = false;
    private boolean windowActive = true;
    private Timer levelUpTimer;

    // BGM과 SFX 클립을 미리 메모리에 로드한다. //
    private AudioManager() {
        loadBgm(BGM_A);
        loadBgm(BGM_B);

        for (String sfxName : SFX_NAMES) {
            Clip clip = loadClip("sfx", "sfx-" + sfxName + ".wav");
            if (clip != null) sfxClips.put(sfxName, clip);
        }

        // 설정 변경 시 볼륨을 즉시 갱신한다.
        Settings.get().addListener(this::refreshVolume);
    }

    /// 오디오 파일을 Clip 객체로 불러온다.
    private Clip loadClip(String subFolder, String fileName) {
        // 음원 파일을 탐색한다. (로컬 후보 폴더 우선, 그 후 기본 Audio 폴더)
        File file = new File("audio-candidates/Audio/" + subFolder + "/" + fileName);
        if (!file.exists()) {
            file = new File("Audio/" + subFolder + "/" + fileName);
        }
        if (!file.exists()) return null; // 파일이 없어도 정상 진행한다.

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            System.err.println("사운드 파일 로딩 실패: " + file.getPath() + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /// BGM 파일을 불러오고 재생 종료 리스너를 등록한다.
    private void loadBgm(String bgmName) {
        Clip clip = loadClip("bgm", bgmName + ".wav");
        if (clip == null) return;

        bgmClips.put(bgmName, clip);

        // 곡 재생이 끝나면 다음 곡으로 전환한다.
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

    /// 메뉴 화면 BGM을 재생한다.
    public void playMenuBgm() {
        currentMode = "menu";
        playBgmInternal(BGM_B, false);
    }

    /// 튜토리얼 BGM을 재생한다.
    public void playTutorialBgm() {
        currentMode = "tutorial";
        playBgmInternal(BGM_A, true);
    }

    /// 인게임 BGM을 무작위로 재생한다.
    public void startGameBgm() {
        currentMode = "game";
        playBgmInternal(chooseRandomGameBgm(), true);
    }

    /// 직전 곡을 제외하고 인게임 BGM을 무작위로 선택한다.
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

    /// 곡 재생 완료 시 다음 곡으로 전환한다.
    private void onBgmFinished() {
        if ("game".equals(currentMode)) {
            playBgmInternal(chooseRandomGameBgm(), true);
        } else if ("tutorial".equals(currentMode)) {
            playBgmInternal(BGM_A, true);
        } else {
            playBgmInternal(BGM_B, true);
        }
    }

    /// 지정한 BGM을 재생한다.
    private void playBgmInternal(String bgmName, boolean restart) {
        Clip nextClip = bgmClips.get(bgmName);
        if (nextClip == null) return;

        // 같은 곡이 재생 중이면 유지한다.
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

    /// BGM을 일시정지한다. 재생 위치를 보존한다.
    public void pauseBgm() {
        bgmPaused = true;
        if (currentBgmClip != null && currentBgmClip.isRunning()) {
            currentBgmClip.stop();
        }
    }

    /// BGM을 멈춘 위치부터 다시 재생한다.
    public void resumeBgm() {
        bgmPaused = false;
        if (currentBgmClip != null && canOutputMusic()) {
            currentBgmClip.start();
        }
    }

    /// BGM을 정지하고 처음 위치로 되돌린다.
    public void stopBgm() {
        bgmPaused = false;
        if (currentBgmClip != null) {
            currentBgmClip.stop();
            currentBgmClip.setFramePosition(0);
        }
    }

    // 효과음(SFX) 재생 //

    /// 메뉴 선택 효과음을 재생한다.
    public void playMenuSelect() { playSound("menu-select"); }

    /// 블록 이동 효과음을 재생한다. 연타 간격을 제한한다.
    public void playMove() { playSoundWithInterval("move", 80); }

    /// 블록 회전 효과음을 재생한다.
    public void playRotate() { playSound("rotate"); }

    /// 소프트 드롭 효과음을 재생한다. 연타 간격을 제한한다.
    public void playSoftDrop() { playSoundWithInterval("softdrop", 100); }

    /// 블록 안착 효과음을 재생한다.
    public void playLock() { playSound("lock"); }

    /// 하드 드롭 효과음을 재생한다.
    public void playHardDrop() { playSound("harddrop"); }

    /// 홀드 효과음을 재생한다.
    public void playHold() { playSound("hold"); }

    /// 라인 삭제 효과음을 재생한다. 4줄은 전용 효과음을 쓴다.
    public void playLineClear(int lines) {
        if (lines >= 4) playSound("tetris");
        else if (lines > 0) playSound("lineclear");
    }

    /// 라인 삭제음과 겹치지 않도록 지연 후 레벨업 효과음을 재생한다.
    public void playLevelUpDelayed() {
        if (levelUpTimer != null) levelUpTimer.stop();
        levelUpTimer = new Timer(300, e -> playSound("levelup"));
        levelUpTimer.setRepeats(false);
        levelUpTimer.start();
    }

    /// BGM을 멈추고 게임 오버 효과음을 재생한다.
    public void playGameOver() {
        stopBgm();
        playSound("gameover");
    }

    /// 튜토리얼 완료 효과음을 재생한다.
    public void playTutorialClear() { playSound("tutorial-clear"); }

    /// 효과음을 처음부터 재생한다.
    private void playSound(String sfxName) {
        if (!canOutputSounds()) return;
        Clip clip = sfxClips.get(sfxName);
        if (clip == null) return;

        clip.stop();
        clip.setFramePosition(0);
        applyVolumeToClip(clip, Settings.get().sfxGain());
        clip.start();
    }

    /// 지정한 간격 이내의 연속 효과음 재생을 제한한다.
    private void playSoundWithInterval(String sfxName, int minIntervalMs) {
        long now = System.currentTimeMillis();
        long last = lastSoundTime.getOrDefault(sfxName, 0L);
        if (now - last < minIntervalMs) return;

        lastSoundTime.put(sfxName, now);
        playSound(sfxName);
    }

    // 볼륨 및 사운드 출력 제어 //

    /// 창 활성화 상태를 갱신한다.
    public void setWindowActive(boolean active) {
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

    /// 모드에 맞게 BGM 출력 비율을 계산한다.
    private float calculateBgmGain() {
        float scale = "tutorial".equals(currentMode) ? 0.50f : 0.70f;
        return Settings.get().bgmGain() * scale;
    }

    /// 변경된 볼륨 설정을 재생 중인 클립에 반영한다.
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

    /// 볼륨 배율을 데시벨로 변환해 클립에 적용한다.
    private void applyVolumeToClip(Clip clip, float gain) {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;

        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float db = Settings.toDecibel(gain);
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
    }
}
