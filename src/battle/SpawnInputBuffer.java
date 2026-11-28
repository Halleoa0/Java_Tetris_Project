package battle;

import game.Game;
import java.util.function.Predicate;
import settings.Settings;

/**
 * 스폰 대기(블록을 쌓고 다음 블록이 나오기 전) 중에 누른 키를 저장해 두었다가, 새 블록이 나오면 적용한다.
 * 블록을 쌓으면 직전에 누른 입력이 무시되는 문제를 막기 위한 버퍼이다.
 */
final class SpawnInputBuffer {
    private int horizontal;     // 좌우 이동 (-1 좌, 0 안 함, 1 우)
    private int rotation;       // 회전 방향 (-1 반시계, 0 안 함, 1 시계) - IRS
    private boolean hold;       // 홀드를 눌렀는지 - IHS
    private boolean softDrop;
    private boolean hardDrop;

    /** 누른 동작을 저장한다. 버퍼 대상이 아닌 동작(재시작 등)은 무시한다. */
    void store(Settings.Action action) {
        switch (action) {
            case MOVE_LEFT -> horizontal = -1;
            case MOVE_RIGHT -> horizontal = 1;
            case ROTATE_CW -> rotation = 1;
            case ROTATE_CCW -> rotation = -1;
            case HOLD -> hold = true;
            case SOFT_DROP -> softDrop = true;
            case HARD_DROP -> hardDrop = true;
            default -> { }
        }
    }

    /**
     * 저장된 입력을 게임에 적용하고 버퍼를 비운다.
     * isHeld : 지금 그 회전/홀드 키를 누르고 있는지 알려주는 함수.
     */
    void applyTo(Game game, Predicate<Settings.Action> isHeld) {
        if (game.active == null || game.gameOver) {
            clear();
            return;
        }

        int bufferedHorizontal = horizontal;
        int bufferedRotation = rotation;
        boolean bufferedHold = hold;
        boolean bufferedSoftDrop = softDrop;
        boolean bufferedHardDrop = hardDrop;
        clear(); // 적용 중에 다음 블록이 나올 수 있으므로 먼저 비움

        // 키를 아직 누르고 있으면 Game이 IHS/IRS로 이미 처리했으므로 버퍼는 건너뜀 (두 번 홀드/회전 방지)
        if (Settings.get().ihs() && isHeld.test(Settings.Action.HOLD)) bufferedHold = false;
        if (Settings.get().irs() && (isHeld.test(Settings.Action.ROTATE_CW)
                || isHeld.test(Settings.Action.ROTATE_CCW))) bufferedRotation = 0;

        if (bufferedHold) game.hold();
        if (bufferedRotation != 0) game.rotate(bufferedRotation);
        if (bufferedHorizontal != 0) game.move(bufferedHorizontal, 0);
        if (bufferedSoftDrop) game.softDrop();
        if (bufferedHardDrop) game.hardDrop();
    }

    /** 버퍼를 비운다. (게임을 다시 시작하거나 라운드가 끝났을 때) */
    void clear() {
        horizontal = 0;
        rotation = 0;
        hold = false;
        softDrop = false;
        hardDrop = false;
    }
}