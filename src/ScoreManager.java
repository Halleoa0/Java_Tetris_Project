/** 점수·레벨·줄 수·콤보·백투백 */
final class ScoreManager {

    /** 회전으로 놓은 T미노의 판정 종류 */
    enum Spin { NONE, MINI, FULL }

    private static final int[] NORMAL    = {0, 100, 300, 500, 800};   // 0~4줄
    private static final int[] MINI_SPIN = {100, 200, 400};           // 0~2줄
    private static final int[] FULL_SPIN = {400, 800, 1200, 1600};    // 0~3줄
    private static final int[] PERFECT   = {0, 800, 1200, 1800, 2000}; // 퍼펙트 클리어 (1~4줄)
    private static final int PERFECT_B2B_TETRIS = 3200;
    private static final int COMBO_BONUS = 50;
    private static final int LINES_PER_LEVEL = 10;
    /** 점수 문구를 화면에 보여주는 시간(ms). */
    private static final int LABEL_SHOW_MS = 1000, LABEL_FADE_MS = 500, LABEL_TRACKING_ANIMATION_MS = 1200;
    private int labelLeft;

    int score, lines, level = 1;
    /** 연속 줄 삭제 횟수. -1이면 콤보 없음, 0이면 첫 삭제(보너스 없음) */
    int combo = -1;
    /** 직전 삭제가 '어려운 삭제'(테트리스/T-스핀 삭제)였는지 */
    boolean backToBack;
    /** 마지막으로 획득한 점수와 설명 (화면에 표시하고 싶을 때 사용) */
    int lastGain;
    String cleardLineString = "";
    String skillString = "";
    String comboString = "";
    String perfectString = "";

    void reset() {
        score = lines = 0; level = 1; combo = -1; backToBack = false;
        lastGain = 0; cleardLineString = ""; labelLeft = 0; skillString = ""; comboString = "";
        perfectString = "";
    }

    /** 문구 표시 시간 줄이기. GamePanel 타이머에서 프레임 호출 */
    void tick(int elapsedMs) {
        if (labelLeft <=0) return;
        labelLeft -= elapsedMs;
        if (labelLeft <= 0) {
            labelLeft = 0;
            skillString = "";
            cleardLineString = "";
            comboString = "";
            perfectString = "";
            lastGain = 0;
        }
    }

    
    /// easeOut 형식의 자간 조절
    float clearLineTracking() {
        float progress = Math.max(0f, Math.min(1f, (LABEL_SHOW_MS - labelLeft) / (float) LABEL_TRACKING_ANIMATION_MS));
        float easedProgress = 1f - (1f - progress) * (1f - progress);
        return -0.30f + 0.55f * easedProgress;
    }

    /** 문구 투명도 0.0~1.0 (마지막 0.5초동안 서서히 사라짐) */
    float labelAlpha() {
        if (labelLeft <= 0) return 0f;
        return Math.min(1f, labelLeft / (float) LABEL_FADE_MS);
    }

    /** 소프트 드롭: 내려간 칸당 1점 */
    void onSoftDrop(int cells) { if (cells > 0) score += cells; }

    /** 하드 드롭: 내려간 칸당 2점 */
    void onHardDrop(int cells) { if (cells > 0) score += cells * 2; }

    /**
     * 블록이 고정될 때 호출. 점수를 계산해 반영하고, 줄 수/레벨도 갱신
     * @param cleared 지운 줄 수 (0~4)
     * @param spin    T-스핀 판정
     * @param perfect 줄 삭제 후 보드가 완전히 비었는지
     * @return 이번에 얻은 점수
     */
    int onLock(int cleared, Spin spin, boolean perfect) {
        cleared = Math.max(0, Math.min(cleared, 4));
        int base;
        if (spin == Spin.FULL)      base = FULL_SPIN[Math.min(cleared, 3)];
        else if (spin == Spin.MINI) base = MINI_SPIN[Math.min(cleared, 2)];
        else                        base = NORMAL[cleared];

        // 어려운 삭제 = 테트리스 또는 줄을 지운 T-스핀
        boolean difficult = cleared > 0 && (cleared == 4 || spin != Spin.NONE);
        boolean b2bApplied = false;
        if (cleared > 0) {
            if (difficult) {
                if (backToBack) { base = base * 3 / 2; b2bApplied = true; } // 백투백 x1.5
                backToBack = true;
            } else {
                backToBack = false; // 일반 삭제가 나오면 백투백 끊김
            }
        }

        int gain = base * level;

        // 콤보: 연속으로 줄을 지울 때 50 x 콤보 x 레벨
        if (cleared > 0) {
            combo++;
            if (combo > 0) gain += COMBO_BONUS * combo * level;
        } else {
            combo = -1;
        }

        // 퍼펙트 클리어 보너스
        if (perfect && cleared > 0) {
            gain += (b2bApplied && cleared == 4 ? PERFECT_B2B_TETRIS : PERFECT[cleared]) * level;
        }

        score += gain;

        // B2B 문구는 현재 연속 상태가 아니라 이번 삭제에 보너스가 실제 적용됐을 때 표시한다.
        label(cleared, spin, b2bApplied, perfect);

        // 줄 수 & 레벨 (점수 계산 후에 올려야 '지운 시점의 레벨'로 점수가 계산)
        if (cleared > 0) { lines += cleared; level = lines / LINES_PER_LEVEL + 1; }
        return gain;
    }

    private void label(int cleared, Spin spin, boolean b2b, boolean perfect) {
        if (cleared == 0 && spin == Spin.NONE) return;

        StringBuilder skillSb = new StringBuilder();
        StringBuilder clearSb = new StringBuilder();
        StringBuilder comboSb = new StringBuilder();
        StringBuilder perfectSb = new StringBuilder();
        if (b2b) skillSb.append("B2B "); else skillSb.append("");
        if (spin == Spin.MINI) skillSb.append("MINI T-SPIN ");
        else if (spin == Spin.FULL) skillSb.append("T-SPIN ");

        clearSb.append(switch (cleared) {
            case 1 -> "SINGLE";
            case 2 -> "DOUBLE";
            case 3 -> "TRIPLE";
            case 4 -> "TETRIS";
            default -> "";
        });

        if (combo > 0) comboSb.append(combo).append(" Combo");
        if (perfect) perfectSb.append("ALL CLEAR");

        skillString = skillSb.toString();
        cleardLineString = clearSb.toString();
        comboString = comboSb.toString();
        perfectString = perfectSb.toString();
        labelLeft = LABEL_SHOW_MS;
    }
}
