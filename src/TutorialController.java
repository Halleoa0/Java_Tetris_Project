import java.util.List;

/** 튜토리얼 설정을 게임과 화면에 적용하고, 게임 이벤트로 단계를 진행한다. */
final class TutorialController implements GameListener {
    private static final int STEP_COUNT = 3;

    private final Game game;
    private final GamePanel panel;
    private final List<TutorialStep> steps = List.of(
            TutorialStep.move(), TutorialStep.rotate(), TutorialStep.clearLine());
    private final TutorialOverlayRenderer overlay;
    private Runnable completionCallback = () -> { };
    private int currentStep;
    private boolean started, complete;

    TutorialController(Game game, GamePanel panel) {
        this.game = game;
        this.panel = panel;
        panel.setPauseRestartCallback(this::start);
        overlay = new TutorialOverlayRenderer(panel.sansKRBlack);
        panel.setOverlayRenderer(overlay);
    }

    /** 메뉴나 진입점에서 호출해 튜토리얼을 시작한다. */
    void start() {
        if (!started) {
            game.addListener(this);
            started = true;
        }
        currentStep = 0;
        complete = false;
        applyCurrentStep();
    }

    /** 완료 시 메뉴 복귀 등 외부 동작을 연결하는 지점. */
    void setCompletionCallback(Runnable callback) {
        completionCallback = callback == null ? () -> { } : callback;
    }

    @Override public void onMove(int dx, int dy) {
        if (!complete && currentStep == 0) {
            steps.get(currentStep).onMove(dx, dy);
            checkStepCompletion();
        }
    }

    @Override public void onRotate(int direction) {
        if (!complete && currentStep == 1) {
            steps.get(currentStep).onRotate(direction);
            checkStepCompletion();
        }
    }

    @Override public void onLinesCleared(int lines) {
        if (!complete && currentStep == 2) {
            steps.get(currentStep).onLinesCleared(lines);
            checkStepCompletion();
        }
    }

    @Override public void onGameOver() {
        if (!complete && currentStep == 2) game.restart();
    }

    private void checkStepCompletion() {
        if (steps.get(currentStep).isComplete()) {
            if (currentStep + 1 == STEP_COUNT) finish();
            else {
                currentStep++;
                applyCurrentStep();
            }
        }
    }

    private void applyCurrentStep() {
        TutorialStep step = steps.get(currentStep);
        panel.setInputFilter(step.allowedActions::contains);
        game.setGravityEnabled(step.gravityEnabled);
        game.setSpawnVisible(step.spawnVisible);
        game.setPieceGenerator(step.pieceGenerator);
        game.restart();
        overlay.showStep(currentStep + 1, STEP_COUNT, step.message);
        panel.repaint();
    }

    private void finish() {
        complete = true;
        game.setGravityEnabled(false);
        panel.setInputFilter(action -> false);
        overlay.showComplete();
        panel.repaint();
        completionCallback.run();
    }
}
