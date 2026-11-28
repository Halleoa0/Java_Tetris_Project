package ui;

import game.Game;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import settings.Settings;

/**
 * 게임 키 바인딩을 등록하고, 누르고 있는 키(DAS/ARR)와 IRS/IHS(스폰 시 회전/홀드) 입력 상태를 관리하는 클래스.
 * 1인용 화면과 대전 화면이 함께 쓰며, 화면마다 다른 규칙은 {@link Host}로 전달받는다.
 */
public final class GameInputController {

    /** 키 입력을 받는 화면이 알려줘야 하는 상태와, 화면마다 다르게 처리하는 동작. */
    public interface Host {
        /** 일시정지 중인지. */
        boolean isPaused();
        /** 결과 화면(게임 오버, 라운드 종료 등)인지. 이때 하드 드롭 키는 결과 확인으로 쓴다. */
        boolean isFinished();
        /** 지금 게임 조작 키를 받는 상태인지. 일시정지 여부는 따로 확인한다. */
        default boolean acceptsGameplay() { return true; }
        /** 일시정지 중에 게임 키를 눌렀을 때 호출된다. */
        void onPauseInput(Settings.Action action);
        /** 결과 화면에서 하드 드롭 키를 눌렀을 때 호출된다. */
        void onConfirmResult();
        /** 한 번 누르는 키(회전, 홀드, 하드 드롭, 재시작)의 동작을 실행한다. */
        default void dispatch(Settings.Action action, Runnable run) { run.run(); }
        /** 이동/소프트 드롭 키를 처리하기 직전에 호출된다. alreadyHeld는 이미 누르고 있던 키(OS 반복 입력)인지. */
        default void beforeHeldPress(Settings.Action action, boolean alreadyHeld) { }
    }

    private final JComponent component;
    private final Game game;
    private final Host host;
    private final HeldKeyController heldKeys;
    private GamePanel.InputFilter inputFilter;
    private final Set<Settings.Action> initialKeys = new HashSet<>(); // 누르고 있는 회전/홀드 키 (IRS/IHS)
    private int lastInitialRotation = 1; // 회전 키 두 개를 같이 누르면 마지막에 누른 방향
    private boolean ignoreSpaceUntilRelease;

    /** 키를 등록할 컴포넌트, 조작할 게임, 화면 규칙을 받아 생성한다. */
    public GameInputController(JComponent component, Game game, Host host) {
        this.component = component;
        this.game = game;
        this.host = host;
        this.heldKeys = new HeldKeyController(game, component);
    }

    /** 설정의 키로 게임 조작 키(이동, 드롭, 회전, 홀드, 재시작)를 모두 등록한다. restart는 재시작 키의 동작. */
    public void bindGameplayKeys(Runnable restart) {
        // 좌우 이동, 소프트 드롭 : 누르고 있으면 DAS/ARR(소프트 드롭은 SDF)로 반복
        bindHeld(Settings.Action.MOVE_LEFT, () -> game.move(-1, 0));
        bindHeld(Settings.Action.MOVE_RIGHT, () -> game.move(1, 0));
        bindHeld(Settings.Action.SOFT_DROP, () -> {
            game.setSoftDropHeld(true);
            game.softDrop();
        });
        // 회전, 홀드 : 누르고 있어도 한 번만 실행하고, 누르고 있는 상태는 다음 블록의 IRS/IHS에 사용
        bindInitial(Settings.Action.ROTATE_CW, "rotateCW", () -> game.rotate(1));
        bindInitial(Settings.Action.ROTATE_CCW, "rotateCCW", () -> game.rotate(-1));
        bindInitial(Settings.Action.HOLD, "hold", game::hold);
        bind(Settings.Action.HARD_DROP, "hardDrop", game::hardDrop);
        bindHardDropRelease();
        bind(Settings.Action.RESTART, "restart", restart);
    }

    /** 허용할 입력을 고르는 필터를 지정하고(예: 튜토리얼), IRS/IHS 상태를 다시 계산한다. */
    void setInputFilter(GamePanel.InputFilter filter) {
        inputFilter = filter;
        updateInitialInputs();
    }

    /** 일시정지 중에만 동작하는 메뉴 이동 키를 등록한다. 일시정지가 아니면 원래 키 동작을 실행한다. */
    public void bindPauseNavigation(String key, Runnable moveSelection) {
        bindNavigation(key, "pause", host::isPaused, () -> { moveSelection.run(); component.repaint(); });
    }

    /** 결과 화면에서만 동작하는 버튼 선택 키를 등록한다. 그 외에는 원래 키 동작을 실행한다. */
    void bindGameOverNavigation(String key, Runnable selectOption) {
        bindNavigation(key, "gameOver", () -> host.isFinished() && !host.isPaused(),
                () -> { selectOption.run(); component.repaint(); });
    }

    /** 입력 필터나 일시정지 상태와 상관없이 항상 동작하는 키(ESC, M 등)를 등록한다. */
    public void bindUnfiltered(String key, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); component.repaint(); }
        });
    }

    /** 매 프레임 호출해 누르고 있는 이동 키를 DAS/ARR에 맞춰 반복한다. */
    public void tickHeldKeys(int elapsedMs) { heldKeys.tickHeldKeys(elapsedMs); }

    /** 해당 회전/홀드 키를 누르고 있는지 확인한다. */
    public boolean isInitialKeyHeld(Settings.Action action) { return initialKeys.contains(action); }

    /** 누르고 있는 것으로 기록된 키가 하나라도 있는지 확인한다. */
    public boolean hasHeldKeys() { return heldKeys.hasHeldKeys() || !initialKeys.isEmpty(); }

    /** 누르고 있던 키(이동, IRS/IHS)를 모두 뗀 상태로 만든다. 하드 드롭 잠금은 유지한다. */
    public void releaseAllKeys() {
        heldKeys.clear();
        initialKeys.clear();
        updateInitialInputs();
    }

    /** 누르고 있던 키와 하드 드롭 잠금 상태를 모두 초기화한다. */
    public void clear() {
        releaseAllKeys();
        allowNextHardDrop();
    }

    /** 다음 하드 드롭 입력을 다시 허용한다. (하드 드롭 키를 뗐거나 창을 벗어났을 때) */
    public void allowNextHardDrop() { ignoreSpaceUntilRelease = false; }

    // 한 번 누르면 한 번 실행되는 일반 키를 등록한다.
    // 하드 드롭은 키를 뗄 때까지 반복 입력을 막고, 결과 화면/일시정지 중에는 해당 화면의 입력으로 전달한다.
    private void bind(Settings.Action action, String name, Runnable run) {
        String binding = name + "_" + action.name();
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), binding);
        component.getActionMap().put(binding, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (action == Settings.Action.HARD_DROP) {
                    if (ignoreSpaceUntilRelease) return;
                    ignoreSpaceUntilRelease = true;
                }
                if (host.isFinished() && action == Settings.Action.HARD_DROP) host.onConfirmResult();
                else if (host.isPaused()) host.onPauseInput(action);
                else if (host.acceptsGameplay() && allowed(name)) host.dispatch(action, run);
                component.repaint();
            }
        });
    }

    /** 하드 드롭 키를 떼는 것을 감지한다. */
    private void bindHardDropRelease() {
        String binding = "hardDropReleased";
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Settings.get().released(Settings.Action.HARD_DROP), binding);
        component.getActionMap().put(binding, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { allowNextHardDrop(); }
        });
    }

    /** 회전/홀드처럼 IRS/IHS 대상이 되는 키를 등록한다. 누르고 있는 동안의 상태를 기록해 다음 블록 스폰에 반영한다. */
    private void bindInitial(Settings.Action action, String name, Runnable run) {
        String binding = name + "_" + action.name();
        String released = binding + "Released";
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), binding);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), released);
        component.getActionMap().put(binding, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!host.acceptsGameplay()) return;
                if (host.isPaused()) host.onPauseInput(action);
                else if (allowed(name) && initialKeys.add(action)) { // add가 false면 OS 반복 입력
                    if (action == Settings.Action.ROTATE_CW) lastInitialRotation = 1;
                    else if (action == Settings.Action.ROTATE_CCW) lastInitialRotation = -1;
                    updateInitialInputs();
                    host.dispatch(action, run);
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
    private void bindHeld(Settings.Action action, Runnable run) {
        String key = action.name(), pressed = key + "HeldPressed", released = key + "HeldReleased";
        heldKeys.register(action, run);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().pressed(action), pressed);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Settings.get().released(action), released);
        component.getActionMap().put(pressed, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!host.acceptsGameplay()) return;
                if (host.isPaused()) { host.onPauseInput(action); component.repaint(); }
                else {
                    host.beforeHeldPress(action, heldKeys.isHeld(action));
                    heldKeys.press(action);
                }
            }
        });
        component.getActionMap().put(released, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { heldKeys.releaseHeldKey(action); }
        });
    }

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