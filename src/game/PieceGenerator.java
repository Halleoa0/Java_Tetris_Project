package game;

/// next 방식을 바꾸기 위하여 지정한 인터페이스
/// 일반 게임 - 7beg 시스템 / 튜토리얼 - T미노만 시스템
@FunctionalInterface
public interface PieceGenerator {
    public Tetromino next();
}
