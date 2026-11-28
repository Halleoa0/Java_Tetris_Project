package ui;

import game.Game;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import settings.Settings;

/** 게임 키 바인딩을 등록하고, 한 번만 적용되는 IRS/IHS(스폰 시 회전/홀드) 입력 상태를 관리하는 클래스. */
final class GameInputController {
    private final JComponent component;
    private final Game game;
    private final HeldKeyController heldKeys;
    private final BooleanSupplier paused, gameOver;
    private final Consumer<Settings.Action> pauseInput;
    private final Runnable confirmGameOver;
    private GamePanel.InputFilter inputFilter;
    private final Set<Settings.Action> initialKeys = new HashSet<>();
    private int lastInitialRotation = 1;
    private boolean ignoreSpaceUntilRelease;

    /** 키를 등록할 컴포넌트, 게임, 누르고 있는 키 관리자와 일시정지/게임오버 상태 및 콜백을 받아 생성한다. */
    GameInputController(JComponent component, Game game, HeldKeyController heldKeys,
                        BooleanSupplier paused, BooleanSupplier gameOver,
                        Consumer<Settings.Action> pauseInput, Runnable confirmGameOver) {
        this.component = component;
        this.game = game;
        this.heldKeys = heldKeys;
        this.paused = paused;
        this.gameOver = gameOver;
        this.pauseInput = pauseInput;
        this.confirmGameOver = confirmGameOver;
    }

    /** 허용할 입력을 고르는 필터를 지정하고(예: 튜토리얼), IRS/IHS 상태를 다시 계산한다. */
    void setInputFilter(GamePanel.InputFilter filter) {
        inputFilter = filter;
        updateInitialInputs();
    }

    // 한 번 누르면 한 번 실행되는 일반 키를 등록한다.
    // 하드 드롭은 키를 뗄 때까지 반복 입력을 막고, 게임 오버/일시정지 중에는 해당 화면의 입력으로 전달한다.
    void bind(Settings.Action action, String name, Runnable run) {
        String binding = name + "_" + action.name();
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), binding);
        component.getActionMap().put(binding, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (action == Settings.Action.HARD_DROP) {
                    if (ignoreSpaceUntilRelease) return;
                    ignoreSpaceUntilRelease = true;
                }
                if (gameOver.getAsBoolean() && action == Settings.Action.HARD_DROP) confirmGameOver.run();
                else if (paused.getAsBoolean()) pauseInput.accept(action);
                else if (allowed(name)) run.run();
                component.repaint();
            }
        });
    }

    /** 회전/홀드처럼 IRS/IHS 대상이 되는 키를 등록한다. 누르고 있는 동안의 상태를 기록해 다음 블록 스폰에 반영한다. */
    void bindInitial(Settings.Action action, String name, Runnable run) {
        String binding = name + "_" + action.name();
        String released = binding + "Released";
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), binding);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), released);
        component.getActionMap().put(binding, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused.getAsBoolean()) pauseInput.accept(action);
                else if (allowed(name) && initialKeys.add(action)) {
                    if (action == Settings.Action.ROTATE_CW) lastInitialRotation = 1;
                    else if (action == Settings.Action.ROTATE_CCW) lastInitialRotation = -1;
                    updateInitialInputs();
                    run.run();
                }
                component.repaint();
            }
        });
        component.getActionMap().put(released, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                initialKeys.remove(action);
                updateInitialInputs();
            }
        });
    }

    /** 이동/소프트 드롭처럼 누르고 있으면 반복되는(DAS/ARR) 키를 등록하고 HeldKeyController에 전달한다. */
    void bindHeld(Settings.Action action, Runnable run) {
        String key = action.name(), pressed = key + "HeldPressed", released = key + "HeldReleased";
        heldKeys.register(action, run);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), pressed);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), released);
        component.getActionMap().put(pressed, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (paused.getAsBoolean()) { pauseInput.accept(action); component.repaint(); }
                else heldKeys.press(action);
            }
        });
        component.getActionMap().put(released, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { heldKeys.releaseHeldKey(action); }
        });
    }

    /** 일시정지 중에만 동작하는 메뉴 이동 키를 등록한다. 일시정지가 아니면 원래 키 동작을 실행한다. */
    void bindPauseNavigation(String key, int direction, Runnable moveSelection) {
        bindNavigation(key, "pause", () -> paused.getAsBoolean(), () -> { moveSelection.run(); component.repaint(); });
    }

    /** 게임 오버 화면에서만 동작하는 버튼 선택 키를 등록한다. 그 외에는 원래 키 동작을 실행한다. */
    void bindGameOverNavigation(String key, int direction, Runnable selectOption) {
        bindNavigation(key, "gameOver", () -> gameOver.getAsBoolean() && !paused.getAsBoolean(),
                () -> { selectOption.run(); component.repaint(); });
    }

    /** 입력 필터나 일시정지 상태와 상관없이 항상 동작하는 키(ESC, M 등)를 등록한다. */
    void bindUnfiltered(String key, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); component.repaint(); }
        });
    }

    /** 기록된 IRS/IHS 입력과 하드 드롭 잠금 상태를 모두 초기화한다. */
    void clear() {
        initialKeys.clear();
        ignoreSpaceUntilRelease = false;
        updateInitialInputs();
    }

    /** 하드 드롭 키를 뗐을 때 호출되어 다음 하드 드롭 입력을 다시 허용한다. */
    void spaceReleased() { ignoreSpaceUntilRelease = false; }

    /** 조건(active)이 참일 때만 새 동작을 실행하고, 아니면 같은 키에 원래 등록되어 있던 동작을 실행하도록 키를 덮어쓴다. */
    private void bindNavigation(String key, String name, BooleanSupplier active, Runnable action) {
        KeyStroke stroke = KeyStroke.getKeyStroke("pressed " + key);
        Object priorKey = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
        Action prior = priorKey == null ? null : component.getActionMap().get(priorKey);
        String binding = name + key;
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, binding);
        component.getActionMap().put(binding, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (active.getAsBoolean()) action.run();
                else if (prior != null) prior.actionPerformed(e);
            }
        });
    }

    /** 입력 필터가 없거나 필터가 해당 동작을 허용하면 true를 반환한다. */
    private boolean allowed(String name) { return inputFilter == null || inputFilter.allow(name); }

    /** 현재 누르고 있는 회전/홀드 키를 바탕으로 IRS 회전 방향과 IHS 여부를 계산해 게임에 전달한다. */
    private void updateInitialInputs() {
        boolean cw = Settings.get().irs() && initialAllowed(Settings.Action.ROTATE_CW, "rotateCW");
        boolean ccw = Settings.get().irs() && initialAllowed(Settings.Action.ROTATE_CCW, "rotateCCW");
        int direction = cw && ccw ? lastInitialRotation : cw ? 1 : ccw ? -1 : 0;
        game.setInitialInputs(initialAllowed(Settings.Action.HOLD, "hold"), direction);
    }

    /** 해당 키를 누르고 있고 입력 필터도 허용하는지 확인한다. */
    private boolean initialAllowed(Settings.Action action, String name) {
        return initialKeys.contains(action) && allowed(name);
    }
}
