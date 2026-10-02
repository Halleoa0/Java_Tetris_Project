package battle;

import game.Board;
import java.util.Random;

/**
 * 대전 모드에서 나에게 날아오는 방해 줄(가비지)을 잠깐 모아두는 곳.
 * 방해 줄은 바로 올라오지 않고 여기서 기다리다가, 내가 줄을 못 지우고 블록을 놓으면 올라온다.
 * 그 전에 내가 공격하면 기다리던 방해 줄을 먼저 없앨 수 있다. (상쇄)
 */
final class GarbageQueue {

    private final Random random = new Random();
    private int pendingLines = 0; // 올라오기를 기다리는 방해 줄 수
    private int holeCol = random.nextInt(Board.WIDTH); // 방해 줄의 빈칸 x 위치

    /// 상대가 보낸 공격 lines줄을 대기열에 추가함. 공격이 올 때마다 빈칸 위치를 새로 정함.
    void addGarbage(int lines) {
        pendingLines += lines;
        holeCol = random.nextInt(Board.WIDTH);
    }

    /// 내 공격으로 기다리던 방해 줄을 먼저 없앰. 없애고 남은 공격 줄 수를 반환함. (남은 건 상대에게 보냄)
    /// 예) 기다리는 방해 줄 3줄, 내 공격 5줄 => 3줄 없애고 2줄 반환
    int cancel(int attackLines) {
        int canceled = Math.min(attackLines, pendingLines); // 실제로 없앨 수 있는 줄 수
        pendingLines -= canceled;
        return attackLines - canceled;
    }

    /// 보드로 올릴 방해 줄을 최대 maxLines줄까지 꺼냄. 꺼낸 줄 수를 반환함.
    int popGarbage(int maxLines) {
        int lines = Math.min(pendingLines, maxLines);
        pendingLines -= lines;
        return lines;
    }

    int getHoleCol() { return holeCol; }
    int getPendingCount() { return pendingLines; }
    boolean hasPending() { return pendingLines > 0; }

    /// 대기열을 비움 (새 라운드 시작할 때)
    void clear() { pendingLines = 0; }
}
