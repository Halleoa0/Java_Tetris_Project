package ui;

import audio.AudioManager;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/// 키보드로 버튼을 선택할 수 있도록 하기 위함
final class MenuNavigation {
    private final JPanel panel;
    private final List<TetrisBlockButton> buttons;
    private final Runnable changed;
    private TetrisBlockButton selected;
    private boolean keyboard = true;
    private boolean spaceHeld;

    /** ESC(뒤로가기) 동작 없이 키보드 내비게이션을 만든다. */
    MenuNavigation(JPanel panel, List<TetrisBlockButton> buttons, Runnable changed) {
        this(panel, buttons, changed, null);
    }

    /**
     * 방향키(선택 이동), SPACE(선택 버튼 실행), ESC(뒤로가기) 키를 패널에 등록한다.
     * 패널이 다시 화면에 보이면 첫 번째 활성 버튼을 선택한다.
     */
    MenuNavigation(JPanel panel, List<TetrisBlockButton> buttons, Runnable changed, Runnable back) {
        this.panel = panel;
        this.buttons = buttons;
        this.changed = changed;
        selected = firstEnabled();
        bind("UP", () -> move(0, -1));
        bind("DOWN", () -> move(0, 1));
        bind("LEFT", () -> move(-1, 0));
        bind("RIGHT", () -> move(1, 0));
        if (back != null) bind("pressed ESCAPE", () -> {
            AudioManager.get().playMenuSelect();
            back.run();
        });
        bind("pressed SPACE", () -> {
            spaceHeld = true;
            keyboard = true;
            refresh();
        });
        bind("released SPACE", () -> {
            boolean activate = spaceHeld;
            spaceHeld = false;
            if (activate && selected != null && selected.enabled) {
                AudioManager.get().playMenuSelect();
                selected.action.run();
            }
        });
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            spaceHeld = false;
            if (panel.isShowing()) {
                selected = firstEnabled();
                keyboard = true;
                refresh();
            }
        });
    }

    /** 강조해서 그릴 버튼을 반환한다. 키보드 사용 중이면 선택된 버튼, 마우스 사용 중이면 hover된 버튼. */
    TetrisBlockButton highlightedButton(TetrisBlockButton hovered) {
        return keyboard ? selected : hovered;
    }

    /** 키보드, 스페이스바 입력을 false로 변경해 마우스 입력만 받게 하고, 마우스가 가리킨 버튼을 선택(selected) 버튼으로 바꾼다. */
    void useMouse(TetrisBlockButton button) {
        keyboard = false;
        spaceHeld = false;
        if (button != null && button.enabled) selected = button;
        refresh();
    }

    /** 버튼 목록에서 첫 번째 유효 버튼(마우스 커서가 가리키고 있는 버튼)을 반환한다. 없으면 null. */
    private TetrisBlockButton firstEnabled() {
        return buttons.stream().filter(button -> button.enabled).findFirst().orElse(null);
    }

    /** 키 입력을 동작에 연결한다. 패널이 화면에 보일 때만 실행된다. */
    private void bind(String key, Runnable action) {
        String name = "menu." + key;
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        panel.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (panel.isShowing()) action.run();
            }
        });
    }

    /** 방향(dx, dy) 쪽에 있는 버튼 중 가장 가까운 버튼으로 선택을 옮긴다. 진행 방향에서 벗어난 거리일수록 불리하게 계산한다. */
    private void move(int dx, int dy) {
        keyboard = true;
        if (selected == null) selected = firstEnabled();
        if (selected == null) return;
        TetrisBlockButton next = selected;
        double best = Double.POSITIVE_INFINITY;
        for (TetrisBlockButton candidate : buttons) {
            if (!candidate.enabled || candidate == selected) continue;
            // 버튼의 경계 영역으로 행/열을 구분해서, 블록 모양에 따른 작은 높이 차이에 영향받지 않도록 한다.
            if (dx > 0 && candidate.x < selected.x + width(selected)
                    || dx < 0 && candidate.x + width(candidate) > selected.x
                    || dy > 0 && candidate.y < selected.y + height(selected)
                    || dy < 0 && candidate.y + height(candidate) > selected.y) continue;
            double x = candidate.x + width(candidate) / 2.0 - selected.x - width(selected) / 2.0;
            double y = candidate.y + height(candidate) / 2.0 - selected.y - height(selected) / 2.0;
            double along = dx * x + dy * y;
            double across = dy * x - dx * y;
            double score = along * along + 4 * across * across;
            if (score < best) {
                best = score;
                next = candidate;
            }
        }
        selected = next;
        refresh();
    }

    /** 버튼 모양의 가로 폭(픽셀)을 계산한다. */
    private static int width(TetrisBlockButton button) {
        int columns = 0;
        for (String row : button.rows) columns = Math.max(columns, row.length());
        return columns * button.cellSize;
    }

    /** 버튼 모양의 세로 높이(픽셀)를 계산한다. */
    private static int height(TetrisBlockButton button) {
        return button.rows.length * button.cellSize;
    }

    /** 선택 변경 콜백을 실행하고 패널을 다시 그린다. */
    private void refresh() {
        changed.run();
        panel.repaint();
    }
}
