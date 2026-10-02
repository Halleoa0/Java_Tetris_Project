package game;


/** Game에 다음 블록을 공급한다. 본 게임에서 PieceGenerator == null이며, next()를 실행하지 않는다.
 * 튜토리얼에서만 next() 연산이 재정의 되고 PieceGenerator가 not null이다. */
@FunctionalInterface
public interface PieceGenerator {
    public Tetromino next();
}
