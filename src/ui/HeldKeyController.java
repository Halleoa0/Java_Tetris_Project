package ui;

import game.Board;
import game.Game;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import settings.Settings;

/** 누르고 있는 게임 키를 추적하고 DAS/ARR(첫 반복 지연/반복 간격) 반복 입력을 적용하는 클래스. */
final class HeldKeyController {
    private final Game game;
    private final JComponent panel;
    private final Set<String> heldKeys = new HashSet<>();
    private final Set<String> pressedMoveKeys = new HashSet<>();
    private final Set<String> repeatingKeys = new HashSet<>();
    private final Map<String, Integer> heldKeyElapsed = new HashMap<>();
    private final Map<String, Runnable> heldKeyActions = new HashMap<>();

    /** 조작할 게임과 다시 그릴 패널을 받아 생성한다. */
    HeldKeyController(Game game, JComponent panel) {
        this.game = game;
        this.panel = panel;
    }

    /** 동작(action)에 대해 반복 실행할 내용을 등록한다. */
    void register(Settings.Action action, Runnable run) {
        heldKeyActions.put(action.name(), run);
    }

    /** 키가 눌렸을 때 호출된다. 반대 방향 이동 키를 누르고 있지 않으면 즉시 한 번 실행하고 반복을 시작한다. */
    void press(Settings.Action action) {
        String key = action.name();
        if (action == Settings.Action.MOVE_LEFT || action == Settings.Action.MOVE_RIGHT)
            pressedMoveKeys.add(key);
        if (!oppositeMoveHeld(action)) activateHeldKey(key);
    }

    /** 키에서 손을 뗐을 때 반복을 멈춘다. 이동 키라면 계속 누르고 있던 반대 방향 키의 반복을 다시 시작한다. */
    void releaseHeldKey(Settings.Action action) {
        String key = action.name();
        boolean wasHeld = heldKeys.remove(key);
        repeatingKeys.remove(key);
        heldKeyElapsed.remove(key);
        if (action == Settings.Action.SOFT_DROP) game.setSoftDropHeld(false);
        pressedMoveKeys.remove(key);
        String opposite = action == Settings.Action.MOVE_LEFT ? Settings.Action.MOVE_RIGHT.name()
                : action == Settings.Action.MOVE_RIGHT ? Settings.Action.MOVE_LEFT.name() : null;
        if (wasHeld && opposite != null && pressedMoveKeys.contains(opposite)) activateHeldKey(opposite);
    }

    /** 매 프레임 호출되어 경과 시간만큼 DAS 대기 후 ARR 간격으로 동작을 반복 실행한다. (ARR 0이면 벽까지 즉시 이동) */
    void tickHeldKeys(int elapsedMs) {
        for (String key : heldKeys) {
            int elapsed = heldKeyElapsed.getOrDefault(key, 0) + elapsedMs;
            boolean softDrop = key.equals(Settings.Action.SOFT_DROP.name());
            if (!repeatingKeys.contains(key)) {
                int das = Settings.get().dasMs();
                if (elapsed < das) {
                    heldKeyElapsed.put(key, elapsed);
                    continue;
                }
                elapsed -= das;
                repeatingKeys.add(key);
                if (!softDrop) heldKeyActions.get(key).run();
            }
            if (!softDrop) {
                int arr = Settings.get().arrMs();
                if (arr == 0) {
                    for (int i = 0; i < Board.WIDTH; i++) heldKeyActions.get(key).run();
                    elapsed = 0;
                } else {
                    int repeats = Math.min(Board.WIDTH, elapsed / arr);
                    for (int i = 0; i < repeats; i++) heldKeyActions.get(key).run();
                    elapsed %= arr;
                }
            }
            heldKeyElapsed.put(key, elapsed);
        }
    }

    /** 해당 이동 키를 누르고 있는 상태인지 확인한다. */
    boolean isHeld(Settings.Action action) { return heldKeys.contains(action.name()); }

    /** 누르고 있는 이동 키가 하나라도 있는지 확인한다. */
    boolean hasHeldKeys() { return !heldKeys.isEmpty(); }

    /** 누르고 있는 키 상태를 모두 지우고 소프트 드롭을 해제한다. */
    void clear() {
        heldKeys.clear();
        pressedMoveKeys.clear();
        repeatingKeys.clear();
        heldKeyElapsed.clear();
        game.setSoftDropHeld(false);
    }

    /** 키를 누름 상태로 등록하고 동작을 한 번 실행한다. 이미 눌려 있으면 무시한다. */
    private void activateHeldKey(String key) {
        if (!heldKeys.add(key)) return;
        heldKeyElapsed.put(key, 0);
        repeatingKeys.remove(key);
        heldKeyActions.get(key).run();
        panel.repaint();
    }

    /** 좌/우 이동 키의 경우 반대 방향 키가 이미 눌려 있는지 확인한다. */
    private boolean oppositeMoveHeld(Settings.Action action) {
        return (action == Settings.Action.MOVE_LEFT && heldKeys.contains(Settings.Action.MOVE_RIGHT.name()))
                || (action == Settings.Action.MOVE_RIGHT && heldKeys.contains(Settings.Action.MOVE_LEFT.name()));
    }
}