package game;


/** Game에 다음 블록을 공급한다. */
@FunctionalInterface
public interface PieceGenerator {
    public Tetromino next();
}
