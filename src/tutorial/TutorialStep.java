package tutorial;

import game.FixedPieceGenerator;
import game.PieceGenerator;
import game.Tetromino;
import java.util.Set;





/** 튜토리얼 한 단계의 표시 내용, 입력 정책, 게임 설정, 완료 조건을 보관한다. */
public final class TutorialStep {
    public enum Goal { MOVE_LEFT_AND_RIGHT, ROTATE_BOTH_DIRECTIONS, CLEAR_AT_LEAST_ONE_LINE }

    public final String message;
    public final Set<String> allowedActions;
    public final boolean gravityEnabled;
    public final boolean spawnVisible;
    public final PieceGenerator pieceGenerator;
    private final Goal goal;
    private boolean movedLeft, movedRight, rotatedClockwise, rotatedCounterClockwise, clearedLine;

    public TutorialStep(String message, Set<String> allowedActions, boolean gravityEnabled, boolean spawnVisible,
                 PieceGenerator pieceGenerator, Goal goal) {
        this.message = message;
        this.allowedActions = Set.copyOf(allowedActions);
        this.gravityEnabled = gravityEnabled;
        this.spawnVisible = spawnVisible;
        this.pieceGenerator = pieceGenerator;
        this.goal = goal;
    }

    public static TutorialStep move() {
        return new TutorialStep(
                "← 또는 → 키를 눌러 블록을 양쪽으로 움직여 보세요.",
                Set.of("left", "right"), false, true, new FixedPieceGenerator(Tetromino.Tmino),
                Goal.MOVE_LEFT_AND_RIGHT);
    }

    public static TutorialStep rotate() {
        return new TutorialStep(
                "↑ 또는 X 키로 시계 방향, Z 키로 반시계 방향 회전을 해보세요.",
                Set.of("rotateCW", "rotateCCW"), false, true, new FixedPieceGenerator(Tetromino.Tmino),
                Goal.ROTATE_BOTH_DIRECTIONS);
    }

    public static TutorialStep clearLine() {
        return new TutorialStep(
                "블록을 쌓아 한 줄을 지워 보세요.",
                Set.of("left", "right", "softDrop", "rotateCW", "rotateCCW", "hardDrop", "hold", "restart"),
                true, false, null, Goal.CLEAR_AT_LEAST_ONE_LINE);
    }

    public void onMove(int dx, int dy) {
        if (goal != Goal.MOVE_LEFT_AND_RIGHT || dy != 0) return;
        if (dx < 0) movedLeft = true;
        if (dx > 0) movedRight = true;
    }

    public void onRotate(int direction) {
        if (goal != Goal.ROTATE_BOTH_DIRECTIONS) return;
        if (direction > 0) rotatedClockwise = true;
        if (direction < 0) rotatedCounterClockwise = true;
    }

    public void onLinesCleared(int lines) {
        if (goal == Goal.CLEAR_AT_LEAST_ONE_LINE && lines > 0) clearedLine = true;
    }

    public boolean isComplete() {
        return switch (goal) {
            case MOVE_LEFT_AND_RIGHT -> movedLeft && movedRight;
            case ROTATE_BOTH_DIRECTIONS -> rotatedClockwise && rotatedCounterClockwise;
            case CLEAR_AT_LEAST_ONE_LINE -> clearedLine;
        };
    }
}
