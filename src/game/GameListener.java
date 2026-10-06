package game;


/** Game에서 발생하는 동작 이벤트를 관찰한다. 필요한 이벤트만 구현하면 된다. */
public interface GameListener {
    /** 게임이 진행된 업데이트마다 경과 시간(ms)과 함께 호출된다. */
    public default void onTick(long dtMs) { }

    /** 블록 고정 처리가 끝난 뒤 다음 블록을 스폰하기 직전에 호출된다. */
    public default void onBeforeSpawn() { }

    /** 게임 상태가 재시작되었을 때 호출된다. */
    public default void onRestart() { }

    /** 블록이 좌우로 이동했을 때 호출된다. */
    public default void onMove(int dx, int dy) { }

    /** 블록이 성공적으로 회전했을 때 호출된다. (1: 시계, -1: 반시계) */
    public default void onRotate(int direction) { }

    /** 소프트 드롭으로 1칸 내려갔을 때 호출된다. */
    public default void onSoftDrop() { }

    /** 하드 드롭으로 즉시 바닥까지 낙하했을 때 호출된다. */
    public default void onHardDrop() { }

    /** 블록을 Hold 칸과 교체했을 때 호출된다. */
    public default void onHold() { }

    /** 블록이 바닥에 닿아 보드에 고정되었을 때 호출된다. */
    public default void onLock(Tetromino type) { }

    /** 라인을 지웠을 때 지운 줄 수(1~4)와 함께 호출된다. */
    public default void onLinesCleared(int lines) { }

    /** 라인 삭제로 레벨이 상승했을 때 호출된다. */
    public default void onLevelUp(int newLevel) { }

    /** 스프린트처럼 목표를 달성해 게임이 끝났을 때 호출된다. (이때는 onGameOver가 호출되지 않는다) */
    public default void onGoalReached() { }

    /** 게임 오버 시 호출된다. */
    public default void onGameOver() { }
}

