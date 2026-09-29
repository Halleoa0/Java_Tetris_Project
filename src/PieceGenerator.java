/** Game에 다음 블록을 공급한다. */
@FunctionalInterface
interface PieceGenerator {
    Tetromino next();
}
