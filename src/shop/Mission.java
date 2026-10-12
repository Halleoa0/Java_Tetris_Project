package shop;

/** 위클리 미션 한 종류. 이번 주 미션은 WeeklyMissions가 주차마다 다르게 뽑는다. */
public enum Mission {
    LINES_20   ("줄 20개 삭제하기",           Metric.LINES,   20),
    LINES_50   ("줄 50개 삭제하기",           Metric.LINES,   50),
    LINES_100  ("줄 100개 삭제하기",          Metric.LINES,   100),
    COMBO_5    ("5콤보 이상 달성하기",        Metric.COMBO,   5),
    COMBO_7    ("7콤보 이상 달성하기",        Metric.COMBO,   7),
    COMBO_10   ("10콤보 이상 달성하기",       Metric.COMBO,   10),
    TETRIS_3   ("테트리스(4줄 동시 삭제) 3회", Metric.TETRIS,  3),
    TETRIS_10  ("테트리스(4줄 동시 삭제) 10회", Metric.TETRIS, 10),
    PIECES_300 ("블록 300개 쌓기",            Metric.PIECES,  300),
    HARDDROP_100("하드 드롭 100번 하기",      Metric.HARDDROP, 100),
    GAMES_5    ("게임 5판 끝내기",            Metric.GAMES,   5),
    SCORE_10000("한 판에서 10,000점 달성하기", Metric.SCORE,   10_000),
    SPRINT_1   ("20L 스프린트 1회 완주하기",  Metric.SPRINT,  1);

    /** 진행도 집계 방식 */
    public enum Metric {
        LINES(false), COMBO(true), TETRIS(false), PIECES(false), HARDDROP(false),
        GAMES(false), SCORE(true), SPRINT(false);

        /** true면 누적이 아니라 '한 번에 낸 최댓값'을 진행도로 쓴다. */
        public final boolean isMax;
        Metric(boolean isMax) { this.isMax = isMax; }
    }

    public static final int REWARD_COINS = 10;

    public final String label;
    public final Metric metric;
    public final int target;

    Mission(String label, Metric metric, int target) {
        this.label = label; this.metric = metric; this.target = target;
    }
}
