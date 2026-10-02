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
        int msPerPiece() { return Math.round(1000f / pps); }

        /// 회전/이동을 한 번 할 때마다 기다리는 시간(ms). 블록 하나 시간을 10번의 동작으로 나눔.
        int stepDelayMs() { return Math.max(10, msPerPiece() / 10); }
    }

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

        for (int rotation = 0; rotation < 4; rotation++) {
            // 미노 모양에 따라 왼쪽에 빈 칸이 있어서 x가 음수일 수도 있음. 그래서 -2부터 검사.
            for (int x = -2; x < Board.WIDTH; x++) {
                if (!board.canPlace(piece, x, startY, rotation)) continue; // 벽 밖이거나 막혀 있으면 패스

                // 하드 드롭처럼 더 못 내려갈 때까지 내림
                int y = startY;
                while (board.canPlace(piece, x, y + 1, rotation)) y++;

                plans.add(new Plan(hold, rotation, x, evaluate(board, piece, x, y, rotation)));
            }
        }

        if (plans.isEmpty()) return new Plan(hold, 0, 3, -99999); // 놓을 곳이 없음 (곧 게임 오버)

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

        // 나쁜 것은 뺌(-). 앞의 숫자가 클수록 더 싫어한다는 뜻.
        double score = 0;
        int holes = after.holeCount();
        score -= 3.5 * (Board.HEIGHT - y);      // 놓는 높이: 높은 곳에 놓을수록 나쁨 (y가 클수록 아래쪽)
        score -= 35 * holes;                    // 구멍: 가장 나쁨. 위가 막혀서 채우기 어려움
        score -= 3 * after.rowTransitions();    // 가로 바뀜: 줄 사이사이에 빈칸이 많으면 나쁨
        score -= 9 * after.columnTransitions(); // 세로 바뀜: 블록 밑에 빈칸이 끼면 나쁨

        // 좋은 것은 더함(+). 오른쪽 끝 열을 비워두면 나중에 I 블록으로 테트리스(4줄)를 할 수 있음.
        // 단, 구멍이 없고 아직 안전한 높이(12칸 미만)일 때만.
        boolean safe = holes == 0 && maxHeight(after) < 12;
        int rightHeight = after.columnHeight(Board.WIDTH - 1);      // 오른쪽 끝 열 높이
        int besideHeight = after.columnHeight(Board.WIDTH - 2);     // 그 바로 옆 열 높이
        if (safe && rightHeight == 0 && besideHeight >= 2) score += 18;

        return score;
    }

    /// 가장 높은 열의 높이
    private int maxHeight(Board board) {
        int max = 0;
        for (int x = 0; x < Board.WIDTH; x++) max = Math.max(max, board.columnHeight(x));
        return max;
    }
}
