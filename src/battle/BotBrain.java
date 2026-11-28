package battle;

import game.Board;
import game.Game;
import game.Tetromino;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CPU 봇의 두뇌.
 * 지금 블록을 놓을 수 있는 모든 자리(회전 4가지 x 가로 위치)에 하나씩 놓아보고,
 * 놓은 뒤의 보드에 점수를 매겨서 점수가 가장 높은 자리를 고른다.
 */
public final class BotBrain {

    /// 난이도. pps : 1초에 놓는 블록 수 / mistakeChance : 일부러 아무 데나 놓는 확률 (0.1 = 10%)
    public enum Difficulty {
        EASY("EASY", 0.5f, 0.10),
        NORMAL("NORMAL", 1.0f, 0.04),
        HARD("HARD", 1.6f, 0.01),
        MASTER("MASTER", 4.5f, 0.0);

        public final String label;
        public final float pps;
        public final double mistakeChance;

        Difficulty(String label, float pps, double mistakeChance) {
            this.label = label;
            this.pps = pps;
            this.mistakeChance = mistakeChance;
        }

        /// 블록 하나에 쓰는 시간(ms). 예) 0.5 PPS => 2000ms
        int msPerPiece() { return Math.round(MS_PER_SECOND / pps); }

        /// 회전/이동을 한 번 할 때마다 기다리는 시간(ms). 블록 하나 시간을 STEPS_PER_PIECE번의 동작으로 나눔.
        int stepDelayMs() { return Math.max(MIN_STEP_DELAY_MS, msPerPiece() / STEPS_PER_PIECE); }
    }

    // ---------- 봇의 동작 속도 ----------
    private static final float MS_PER_SECOND = 1000f;
    private static final int STEPS_PER_PIECE = 10;    // 블록 하나 시간을 몇 번의 동작으로 나눌지
    private static final int MIN_STEP_DELAY_MS = 10;  // 동작 사이의 최소 대기 시간(ms)

    // ---------- 자리 탐색 범위 ----------
    private static final int ROTATION_COUNT = 4;      // 회전 상태의 수 (0, 90, 180, 270도)
    private static final int MIN_SEARCH_X = -2;       // 모양 왼쪽의 빈 칸 때문에 x는 음수일 수 있음
    private static final int FALLBACK_X = 3;          // 놓을 곳이 없을 때 쓰는 x (블록이 처음 생기는 위치)
    private static final double NO_PLACE_SCORE = -99_999; // 놓을 곳이 없는 자리의 점수 (어떤 자리보다도 나쁨)

    // ---------- 보드 평가 가중치 (값이 클수록 점수에 크게 반영됨) ----------
    private static final double LANDING_HEIGHT_PENALTY = 3.5;  // 놓는 높이 1칸당 감점
    private static final double HOLE_PENALTY = 35;             // 구멍 1개당 감점. 가장 나쁨
    private static final double ROW_TRANSITION_PENALTY = 3;    // 가로 바뀜 1번당 감점
    private static final double COLUMN_TRANSITION_PENALTY = 9; // 세로 바뀜 1번당 감점
    private static final double TETRIS_WELL_BONUS = 18;        // 테트리스용 우물(오른쪽 끝 열)을 비워두면 가점

    // ---------- 테트리스 우물 조건 ----------
    private static final int SAFE_HEIGHT_LIMIT = 12;  // 가장 높은 열이 이 높이 미만이어야 안전하다고 봄
    private static final int MIN_WELL_DEPTH = 2;      // 우물 옆 열이 이 높이 이상이어야 우물로 인정

    /// 봇이 고른 자리. "홀드를 할지, 몇 번 회전할지, 가로 어디에 놓을지"와 그 자리의 점수.
    static final class Plan {
        final boolean hold;
        final int rotation; // 0 ~ 3
        final int x;        // 블록을 놓을 가로 위치 (Game.x와 같은 기준)
        final double score;

        Plan(boolean hold, int rotation, int x, double score) {
            this.hold = hold;
            this.rotation = rotation;
            this.x = x;
            this.score = score;
        }
    }

    private final Difficulty difficulty;
    private final Random random = new Random();

    BotBrain(Difficulty difficulty) { this.difficulty = difficulty; }

    Difficulty getDifficulty() { return difficulty; }

    /// 지금 블록과 홀드했을 때의 블록을 둘 다 놓아보고 더 좋은 쪽을 고름.
    Plan findBestPlan(Game game) {
        Plan best = bestPlanFor(game.board, game.active, game.y, false);

        // 이번 블록에서 홀드를 아직 안 썼다면, 홀드했을 때도 계산해서 비교
        if (!game.holdUsed) {
            // 홀드 칸이 비어 있으면 홀드했을 때 다음 블록이 나옴
            Tetromino holdPiece = game.held != null ? game.held : game.preview().get(0);
            Plan holdPlan = bestPlanFor(game.board, holdPiece, game.y, true);
            if (holdPlan.score > best.score) best = holdPlan;
        }
        return best;
    }

    /// piece를 놓을 수 있는 모든 자리를 점수 매겨서 가장 좋은 자리를 반환함.
    /// startY : 새 블록이 생긴 높이 (Game.y)
    private Plan bestPlanFor(Board board, Tetromino piece, int startY, boolean hold) {
        List<Plan> plans = new ArrayList<>(); // 놓을 수 있는 자리 목록

        for (int rotation = 0; rotation < ROTATION_COUNT; rotation++) {
            // 미노 모양에 따라 왼쪽에 빈 칸이 있어서 x가 음수일 수도 있음. 그래서 MIN_SEARCH_X부터 검사.
            for (int x = MIN_SEARCH_X; x < Board.WIDTH; x++) {
                if (!board.canPlace(piece, x, startY, rotation)) continue; // 벽 밖이거나 막혀 있으면 패스

                // 하드 드롭처럼 더 못 내려갈 때까지 내림
                int y = startY;
                while (board.canPlace(piece, x, y + 1, rotation)) y++;

                plans.add(new Plan(hold, rotation, x, evaluate(board, piece, x, y, rotation)));
            }
        }

        if (plans.isEmpty()) return new Plan(hold, 0, FALLBACK_X, NO_PLACE_SCORE); // 놓을 곳이 없음 (곧 게임 오버)

        // 난이도에 따라 가끔 일부러 아무 자리나 고름 (사람 같은 실수)
        if (random.nextDouble() < difficulty.mistakeChance) {
            return plans.get(random.nextInt(plans.size()));
        }

        // 점수가 가장 높은 자리를 찾음
        Plan best = plans.get(0);
        for (Plan plan : plans) {
            if (plan.score > best.score) best = plan;
        }
        return best;
    }

    /// piece를 (x, y)에 놓았다고 치고, 그 뒤의 보드가 얼마나 좋은지 점수를 매김. 높을수록 좋음.
    private double evaluate(Board board, Tetromino piece, int x, int y, int rotation) {
        Board after = board.copy();      // 복사본에 놓아봄 (진짜 보드는 그대로)
        after.lock(piece, x, y, rotation);
        after.clearLines();              // 꽉 찬 줄은 지워진 상태로 평가

        // 나쁜 것은 뺌(-). 가중치가 클수록 더 싫어한다는 뜻.
        double score = 0;
        int holes = after.holeCount();
        score -= LANDING_HEIGHT_PENALTY * (Board.HEIGHT - y); // 놓는 높이: 높은 곳에 놓을수록 나쁨 (y가 클수록 아래쪽)
        score -= HOLE_PENALTY * holes;                        // 구멍: 위가 막혀서 채우기 어려움
        score -= ROW_TRANSITION_PENALTY * after.rowTransitions();       // 가로 바뀜: 줄 사이사이에 빈칸이 많으면 나쁨
        score -= COLUMN_TRANSITION_PENALTY * after.columnTransitions(); // 세로 바뀜: 블록 밑에 빈칸이 끼면 나쁨

        // 좋은 것은 더함(+). 오른쪽 끝 열을 비워두면 나중에 I 블록으로 테트리스(4줄)를 할 수 있음.
        // 단, 구멍이 없고 아직 안전한 높이일 때만.
        boolean safe = holes == 0 && maxHeight(after) < SAFE_HEIGHT_LIMIT;
        int rightHeight = after.columnHeight(Board.WIDTH - 1);      // 오른쪽 끝 열 높이
        int besideHeight = after.columnHeight(Board.WIDTH - 2);     // 그 바로 옆 열 높이
        if (safe && rightHeight == 0 && besideHeight >= MIN_WELL_DEPTH) score += TETRIS_WELL_BONUS;

        return score;
    }

    /// 가장 높은 열의 높이
    private int maxHeight(Board board) {
        int max = 0;
        for (int x = 0; x < Board.WIDTH; x++) max = Math.max(max, board.columnHeight(x));
        return max;
    }
}