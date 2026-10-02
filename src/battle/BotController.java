package battle;

import game.Game;
import javax.swing.Timer;

/**
 * CPU 봇의 손. BotBrain이 고른 자리(Plan)로 블록을 실제로 움직인다.
 * 사람처럼 보이도록 회전 -> 좌우 이동 -> 하드 드롭을 한 번에 하지 않고 타이머로 한 동작씩 한다.
 */
final class BotController {

    private final Game game;       // 봇이 플레이하는 게임
    private final BotBrain brain;  // 놓을 자리를 정해주는 두뇌
    private final Timer timer;     // stepDelayMs마다 onTick()을 부름

    private BotBrain.Plan plan;    // 지금 블록을 놓을 자리. null이면 아직 안 정함.
    private long pieceStartMs;     // 지금 블록을 잡기 시작한 시각(ms). PPS를 맞추는 데 씀.

    BotController(Game game, BotBrain brain) {
        this.game = game;
        this.brain = brain;
        this.timer = new Timer(brain.getDifficulty().stepDelayMs(), e -> onTick());
    }

    /// 봇을 움직이기 시작함 (게임 시작, 일시정지 해제 때)
    void start() {
        pieceStartMs = System.currentTimeMillis(); // 일시정지했던 시간은 빼고 다시 잼
        timer.start();
    }

    /// 봇을 멈춤 (일시정지, 라운드 끝날 때)
    void stop() { timer.stop(); }

    /// 멈추고 정해둔 자리도 지움 (새 라운드 시작할 때)
    void reset() {
        stop();
        plan = null;
    }

    /// 타이머가 부를 때마다 한 동작씩 함.
    private void onTick() {
        if (game.gameOver || game.isSpawnDelayed()) return; // 스폰 딜레이 중에는 다음 블록이 아직 없음

        // 0. 자리를 아직 안 정했으면 두뇌에게 물어봄
        if (plan == null) {
            plan = brain.findBestPlan(game);
            if (plan.hold) {
                game.hold();                     // 홀드하면 블록이 바뀌니까
                plan = brain.findBestPlan(game); // 바뀐 블록으로 다시 자리를 정함
            }
        }

        // 1. 회전이 다르면 시계 방향으로 한 번 회전
        if (game.rotation != plan.rotation) {
            if (!game.rotate(1)) drop(); // 막혀서 못 돌리면 그냥 놓음 (멈춰 있지 않도록)
            return;
        }

        // 2. 가로 위치가 다르면 한 칸 이동
        if (game.x < plan.x) {
            if (!game.move(1, 0)) drop(); // 막혀서 못 가면 그냥 놓음
            return;
        }
        if (game.x > plan.x) {
            if (!game.move(-1, 0)) drop();
            return;
        }

        // 3. 자리에 도착함. 난이도의 블록 하나 시간이 지났으면 하드 드롭 (PPS보다 빨라지지 않도록)
        if (System.currentTimeMillis() - pieceStartMs >= brain.getDifficulty().msPerPiece()) {
            drop();
        }
    }

    /// 하드 드롭하고 다음 블록 준비
    private void drop() {
        game.hardDrop();
        plan = null;                               // 다음 블록은 자리를 새로 정해야 함
        pieceStartMs = System.currentTimeMillis(); // 다음 블록 시간을 지금부터 잼
    }
}
