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
    private static final int LABEL_SHOW_MS = 2000, LABEL_FADE_MS = 500;
    private int labelLeft;

    int score, lines, level = 1;
    /** 연속 줄 삭제 횟수. -1이면 콤보 없음, 0이면 첫 삭제(보너스 없음) */
    int combo = -1;
    /** 직전 삭제가 '어려운 삭제'(테트리스/T-스핀 삭제)였는지 */
    boolean backToBack;
    /** 마지막으로 획득한 점수와 설명 (화면에 표시하고 싶을 때 사용) */
    int lastGain;
    String lastLabel = "";

    void reset() {
        score = lines = 0; level = 1; combo = -1; backToBack = false;
        lastGain = 0; lastLabel = ""; labelLeft = 0;
    }

    /** 문구 표시 시간 줄이기. GamePanel 타이머에서 프레임 호출 */
    void tick(int elapsedMs) {
        if (labelLeft <=0) return;
        labelLeft -= elapsedMs;
        if (labelLeft <= 0) {labelLeft = 0; lastLabel = ""; lastGain = 0;}
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
        String text = label(cleared, spin, b2bApplied, perfect);
        if (!text.isEmpty()) {            // 줄을 안 지운 블록은 이전 문구 유지
            lastGain = gain; lastLabel = text; labelLeft = LABEL_SHOW_MS;
        }

        // 줄 수 & 레벨 (점수 계산 후에 올려야 '지운 시점의 레벨'로 점수가 계산)
        if (cleared > 0) { lines += cleared; level = lines / LINES_PER_LEVEL + 1; }
        return gain;
    }

    private String label(int cleared, Spin spin, boolean b2b, boolean perfect) {
        if (cleared == 0 && spin == Spin.NONE) return "";
        StringBuilder sb = new StringBuilder();
        if (b2b) sb.append("B2B ");
        if (spin == Spin.MINI) sb.append("Mini T-Spin ");
        else if (spin == Spin.FULL) sb.append("T-Spin ");
        sb.append(switch (cleared) {
            case 1 -> "Single"; case 2 -> "Double"; case 3 -> "Triple"; case 4 -> "Tetris"; default -> "";
        });
        if (combo > 0) sb.append("  ").append(combo).append(" Combo");
        if (perfect) sb.append("  PERFECT CLEAR");
        return sb.toString().trim();
    }
}