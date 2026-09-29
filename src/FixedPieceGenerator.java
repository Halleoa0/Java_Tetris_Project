/** 튜토리얼에서 같은 종류의 블록을 반복 공급한다. */
final class FixedPieceGenerator implements PieceGenerator {
    private final Tetromino piece;

    FixedPieceGenerator(Tetromino piece) { this.piece = piece; }

    @Override public Tetromino next() { return piece; }
}
