/** Game에서 발생하는 동작 이벤트를 관찰한다. 필요한 이벤트만 구현하면 된다. */
interface GameListener {
    default void onMove(int dx, int dy) { }
    default void onRotate(int direction) { }
    default void onLock(Tetromino type) { }
    default void onLinesCleared(int lines) { }
    default void onGameOver() { }
}
