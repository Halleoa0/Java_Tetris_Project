package battle;

import game.Game;
import game.GameListener;
import game.Tetromino;

/**
 * 1:1 테트리스 배틀(유저 vs CPU 봇)의 승패 규칙, 라운드 시간, 공격 라인 전송을 총괄 관리한다.
 * 3판 2선승제(Best of 3) 및 4분(240초) 제한 시간(마지막 45초 서든데스), 50,000점 선착 승리 룰을 지원한다.
 */
final class BattleGame {

    // 배틀 진행 상태 (대기, 진행 중, 라운드 종료, 전체 매치 종료)
    enum State { PLAYING, ROUND_OVER, MATCH_OVER }
    // 라운드 승자 (없음, 플레이어, CPU 봇)
    enum Winner { NONE, PLAYER, BOT }
    // 승리 사유 (상대 탑아웃 KO, 목표 점수 50,000점 달성, 제한 시간 종료 판정승)
    enum WinReason { NONE, KO, TARGET_SCORE, TIME_OUT }

    static final float ROUND_TIME_LIMIT = 240.0f; // 한 라운드 제한 시간(초). 4분 동안 진행된다.
    static final float HURRY_UP_TIME = 45.0f;      // 서든데스 시작 시간(초). 마지막 45초에는 공격력이 1.5배가 된다.
    static final int TARGET_SCORE = 50_000;        // 목표 점수. 이 점수에 먼저 도달하면 라운드를 즉시 승리한다.

    final Game playerGame = new Game();            // 플레이어 1의 테트리스 게임 인스턴스
    final Game botGame = new Game();               // CPU 봇의 테트리스 게임 인스턴스
    final GarbageQueue playerGarbage = new GarbageQueue(); // 플레이어에게 날아오는 방해 줄 대기열
    final GarbageQueue botGarbage = new GarbageQueue();    // CPU 봇에게 날아오는 방해 줄 대기열

    final BotBrain botBrain;                       // CPU 봇의 두뇌 (놓을 자리를 정함)
    final BotController botController;             // CPU 봇의 손 (블록을 실제로 움직임)

    private State state = State.PLAYING;           // 현재 배틀 진행 상태 (BattlePanel이 만들자마자 startMatch()를 부름)
    private Winner roundWinner = Winner.NONE;      // 이번 라운드의 승자
    private Winner matchWinner = Winner.NONE;      // 3판 2선승 매치 전체의 최종 승자
    private WinReason winReason = WinReason.NONE;  // 라운드가 종료된 구체적인 원인

    private int playerWins = 0;                    // 플레이어가 승리한 라운드 수
    private int botWins = 0;                       // CPU 봇이 승리한 라운드 수
    private int currentRound = 1;                  // 현재 진행 중인 라운드 번호 (1~3)
    private float roundTimer = ROUND_TIME_LIMIT;   // 이번 라운드에 남은 시간(초)

    private int playerTotalAttack = 0;             // 플레이어가 상대에게 보낸 총 공격 줄 수
    private int botTotalAttack = 0;                // CPU 봇이 상대에게 보낸 총 공격 줄 수
    // 이번에 놓은 블록으로 줄을 지웠는지 여부. onLinesCleared에서 true로 바꾸고, onLock에서 확인한 뒤 false로 되돌린다.
    // (Game은 onLinesCleared를 onLock보다 먼저 알려준다)
    private boolean lastPlayerClear = false;
    private boolean lastBotClear = false;

    BattleGame(BotBrain.Difficulty difficulty) {
        this.botBrain = new BotBrain(difficulty);
        this.botController = new BotController(botGame, botBrain);
        this.botGame.setGravityEnabled(false); // CPU 봇은 BotController가 직접 하드 드롭하므로 중력이 필요 없다.
        setupGameListeners();
    }

    /// 두 플레이어(유저와 CPU)의 게임 이벤트를 감지하여 공격 전송과 방해 줄 상쇄를 처리한다.
    private void setupGameListeners() {
        // 플레이어 1 게임 이벤트 리스너 //
        playerGame.addListener(new GameListener() {
            @Override public void onLinesCleared(int lines) {
                lastPlayerClear = true; // 이번 턴에 줄을 지웠음을 기록한다.
            }

            @Override public void onAttackSent(int attackLines) {
                // 서든데스(마지막 45초) 구간에서는 공격력이 1.5배로 강화된다.
                if (isHurryUp()) {
                    attackLines = (int) Math.round(attackLines * 1.5);
                }
                playerTotalAttack += attackLines;

                // 1단계: 플레이어에게 밀려오고 있던 상대방의 방해 줄을 내 공격으로 먼저 상쇄(Cancel)한다.
                int leftover = playerGarbage.cancel(attackLines);
                // 2단계: 상쇄하고 남은 잉여 공격 줄이 있다면 CPU 봇에게 전송한다.
                if (leftover > 0) {
                    botGarbage.addGarbage(leftover);
                }
            }

            @Override public void onLock(Tetromino type) {
                // 블록을 놓았을 때 줄을 지우지 못했다면, 대기 중이던 방해 줄이 바닥에서 솟아오른다.
                // (방해 줄 때문에 다음 블록이 나올 자리가 막히면 다음 블록이 나올 때 게임 오버가 된다)
                if (!lastPlayerClear && playerGarbage.hasPending()) {
                    int toInject = playerGarbage.popGarbage(4); // 한 번에 최대 4줄씩 안전하게 솟아오른다.
                    if (toInject > 0) {
                        playerGame.board.insertGarbage(toInject, playerGarbage.getHoleCol());
                    }
                }
                lastPlayerClear = false;
            }

            @Override public void onGameOver() {
                // 플레이어의 블록이 천장에 닿아 탑아웃되면 패배 처리된다.
                if (state == State.PLAYING) onPlayerKnockedOut();
            }
        });

        // CPU 봇 게임 이벤트 리스너 //
        botGame.addListener(new GameListener() {
            @Override public void onLinesCleared(int lines) {
                lastBotClear = true; // 이번 턴에 CPU 봇이 줄을 지웠음을 기록한다.
            }

            @Override public void onAttackSent(int attackLines) {
                if (isHurryUp()) {
                    attackLines = (int) Math.round(attackLines * 1.5);
                }
                botTotalAttack += attackLines;

                // 1단계: 봇에게 밀려오던 방해 줄을 상쇄한다.
                int leftover = botGarbage.cancel(attackLines);
                // 2단계: 남은 공격 줄을 플레이어에게 전송한다.
                if (leftover > 0) {
                    playerGarbage.addGarbage(leftover);
                }
            }

            @Override public void onLock(Tetromino type) {
                // 줄을 지우지 못하고 블록이 고정되면 대기 중이던 방해 줄을 봇 보드에 주입한다.
                if (!lastBotClear && botGarbage.hasPending()) {
                    int toInject = botGarbage.popGarbage(4);
                    if (toInject > 0) {
                        botGame.board.insertGarbage(toInject, botGarbage.getHoleCol());
                    }
                }
                lastBotClear = false;
            }

            @Override public void onGameOver() {
                // CPU 봇의 보드가 꽉 차서 탑아웃되면 플레이어 승리로 처리된다.
                if (state == State.PLAYING) onBotKnockedOut();
            }
        });
    }

    /// 새로운 배틀 매치를 1라운드부터 시작한다.
    void startMatch() {
        playerWins = 0;
        botWins = 0;
        currentRound = 1;
        matchWinner = Winner.NONE;
        startRound();
    }

    /// 한 라운드를 초기화하고 게임 플레이를 시작한다.
    void startRound() {
        roundTimer = ROUND_TIME_LIMIT;
        roundWinner = Winner.NONE;
        winReason = WinReason.NONE;
        playerGarbage.clear();
        botGarbage.clear();
        lastPlayerClear = false;
        lastBotClear = false;
        playerTotalAttack = 0; // 타임아웃 판정이 이번 라운드 공격량만 비교하도록 초기화한다.
        botTotalAttack = 0;

        playerGame.restart();
        botGame.restart();
        botGame.setGravityEnabled(false); // 라운드 재시작 시에도 봇의 자체 타이머 제어를 위해 중력 비활성화를 유지한다.
        botController.reset();

        state = State.PLAYING;
        botController.start();
    }

    /// 매 프레임 남은 시간을 줄이고 목표 점수 도달 여부를 확인한다.
    void tick(int elapsedMs) {
        if (state != State.PLAYING) return;

        // 목표 점수(TARGET_SCORE) 도달 즉시 승리 판정 //
        if (playerGame.scoring.score >= TARGET_SCORE) {
            onTargetScoreReached(Winner.PLAYER);
            return;
        } else if (botGame.scoring.score >= TARGET_SCORE) {
            onTargetScoreReached(Winner.BOT);
            return;
        }

        roundTimer -= elapsedMs / 1000f; // ms를 초로 바꿔서 뺌
        if (roundTimer <= 0) {
            roundTimer = 0;
            onTimeOut();
        }
    }

    /// 한쪽이 목표 점수(50,000점)에 도달하여 라운드를 즉시 승리한다.
    private void onTargetScoreReached(Winner winner) {
        if (winner == Winner.PLAYER) {
            playerWins++;
            roundWinner = Winner.PLAYER;
        } else {
            botWins++;
            roundWinner = Winner.BOT;
        }
        winReason = WinReason.TARGET_SCORE;
        finishRound();
    }

    /// 플레이어가 탑아웃(천장 도달)되어 CPU 봇이 라운드를 승리한다.
    private void onPlayerKnockedOut() {
        botWins++;
        roundWinner = Winner.BOT;
        winReason = WinReason.KO;
        finishRound();
    }

    /// CPU 봇이 탑아웃(천장 도달)되어 플레이어가 라운드를 승리한다.
    private void onBotKnockedOut() {
        playerWins++;
        roundWinner = Winner.PLAYER;
        winReason = WinReason.KO;
        finishRound();
    }

    /// 제한 시간(4분)이 지나 누적 공격량 > 점수 > 보드 높이 순으로 판정승을 결정한다.
    private void onTimeOut() {
        winReason = WinReason.TIME_OUT;
        if (playerTotalAttack > botTotalAttack) {
            playerWins++;
            roundWinner = Winner.PLAYER;
        } else if (botTotalAttack > playerTotalAttack) {
            botWins++;
            roundWinner = Winner.BOT;
        } else if (playerGame.scoring.score > botGame.scoring.score) {
            playerWins++;
            roundWinner = Winner.PLAYER;
        } else if (botGame.scoring.score > playerGame.scoring.score) {
            botWins++;
            roundWinner = Winner.BOT;
        } else {
            // 보드 높이가 더 낮은 쪽(더 여유로운 쪽)이 승리한다.
            if (playerGame.board.totalHeight() <= botGame.board.totalHeight()) {
                playerWins++;
                roundWinner = Winner.PLAYER;
            } else {
                botWins++;
                roundWinner = Winner.BOT;
            }
        }
        finishRound();
    }

    /// 라운드를 마감하고 어느 한 쪽이 2선승에 도달했는지 확인한다.
    private void finishRound() {
        botController.stop();
        if (playerWins >= 2) {
            matchWinner = Winner.PLAYER;
            state = State.MATCH_OVER;
        } else if (botWins >= 2) {
            matchWinner = Winner.BOT;
            state = State.MATCH_OVER;
        } else {
            state = State.ROUND_OVER;
        }
    }

    /// 다음 라운드로 진행한다.
    void nextRound() {
        if (state == State.ROUND_OVER) {
            currentRound++;
            startRound();
        }
    }

    /// CPU 봇 제어기를 멈추고 게임을 일시 정지한다.
    void pause() {
        botController.stop();
    }

    /// 일시 정지를 풀고 CPU 봇 제어기를 다시 가동한다.
    void resume() {
        if (state == State.PLAYING) {
            botController.start();
        }
    }

    /// 현재 서든데스(마지막 45초) 구간인지 확인한다.
    boolean isHurryUp() {
        return roundTimer <= HURRY_UP_TIME && state == State.PLAYING;
    }

    /// 게임을 완전히 정지하고 봇 제어기를 종료한다.
    void stop() {
        botController.stop();
    }

    // 상태 게터 //
    State getState() { return state; }
    Winner getRoundWinner() { return roundWinner; }
    Winner getMatchWinner() { return matchWinner; }
    WinReason getWinReason() { return winReason; }
    int getPlayerWins() { return playerWins; }
    int getBotWins() { return botWins; }
    int getCurrentRound() { return currentRound; }
    float getRoundTimer() { return roundTimer; }
    int getPlayerTotalAttack() { return playerTotalAttack; }
    int getBotTotalAttack() { return botTotalAttack; }
}
