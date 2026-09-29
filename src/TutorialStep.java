import java.util.Set;

/** 튜토리얼 한 단계의 표시 내용, 입력 정책, 게임 설정, 완료 조건을 보관한다. */
final class TutorialStep {
    enum Goal { MOVE_LEFT_AND_RIGHT, ROTATE_BOTH_DIRECTIONS, CLEAR_AT_LEAST_ONE_LINE }

    final String message;
    final Set<String> allowedActions;
    final boolean gravityEnabled;
    final PieceGenerator pieceGenerator;
    private final Goal goal;
    private boolean movedLeft, movedRight, rotatedClockwise, rotatedCounterClockwise, clearedLine;

    TutorialStep(String message, Set<String> allowedActions, boolean gravityEnabled,
                 PieceGenerator pieceGenerator, Goal goal) {
        this.message = message;
        this.allowedActions = Set.copyOf(allowedActions);
        this.gravityEnabled = gravityEnabled;
        this.pieceGenerator = pieceGenerator;
        this.goal = goal;
    }

    static TutorialStep move() {
        return new TutorialStep(
                "← 또는 → 키를 눌러 T 블록을 양쪽으로 각각 한 번 이상 움직여 보세요.",
                Set.of("left", "right"), false, new FixedPieceGenerator(Tetromino.Tmino),
                Goal.MOVE_LEFT_AND_RIGHT);
    }

    static TutorialStep rotate() {
        return new TutorialStep(
                "↑ 또는 X 키로 시계 방향, Z 키로 반시계 방향 회전을 각각 해보세요.",
                Set.of("rotateCW", "rotateCCW"), false, new FixedPieceGenerator(Tetromino.Tmino),
                Goal.ROTATE_BOTH_DIRECTIONS);
    }

    static TutorialStep clearLine() {
        return new TutorialStep(
                "블록을 쌓은 뒤 Space 키로 한 줄 이상 지워 보세요.",
                Set.of("left", "right", "softDrop", "rotateCW", "rotateCCW", "hardDrop", "hold", "restart"),
                true, null, Goal.CLEAR_AT_LEAST_ONE_LINE);
    }

    void onMove(int dx, int dy) {
        if (goal != Goal.MOVE_LEFT_AND_RIGHT || dy != 0) return;
        if (dx < 0) movedLeft = true;
        if (dx > 0) movedRight = true;
    }

    void onRotate(int direction) {
        if (goal != Goal.ROTATE_BOTH_DIRECTIONS) return;
        if (direction > 0) rotatedClockwise = true;
        if (direction < 0) rotatedCounterClockwise = true;
    }

    void onLinesCleared(int lines) {
        if (goal == Goal.CLEAR_AT_LEAST_ONE_LINE && lines > 0) clearedLine = true;
    }

    boolean isComplete() {
        return switch (goal) {
            case MOVE_LEFT_AND_RIGHT -> movedLeft && movedRight;
            case ROTATE_BOTH_DIRECTIONS -> rotatedClockwise && rotatedCounterClockwise;
            case CLEAR_AT_LEAST_ONE_LINE -> clearedLine;
        };
    }
}
