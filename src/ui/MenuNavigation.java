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

    MenuNavigation(JPanel panel, List<TetrisBlockButton> buttons, Runnable changed) {
        this.panel = panel;
        this.buttons = buttons;
        this.changed = changed;
        selected = firstEnabled();
        bind("UP", () -> move(0, -1));
        bind("DOWN", () -> move(0, 1));
        bind("LEFT", () -> move(-1, 0));
        bind("RIGHT", () -> move(1, 0));
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

    TetrisBlockButton highlightedButton(TetrisBlockButton hovered) {
        return keyboard ? selected : hovered;
    }

    void useMouse(TetrisBlockButton button) {
        keyboard = false;
        spaceHeld = false;
        if (button != null && button.enabled) selected = button;
        refresh();
    }

    private TetrisBlockButton firstEnabled() {
        return buttons.stream().filter(button -> button.enabled).findFirst().orElse(null);
    }

    private void bind(String key, Runnable action) {
        String name = "menu." + key;
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        panel.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (panel.isShowing()) action.run();
            }
        });
    }

    private void move(int dx, int dy) {
        keyboard = true;
        if (selected == null) selected = firstEnabled();
        if (selected == null) return;
        TetrisBlockButton next = selected;
        double best = Double.POSITIVE_INFINITY;
        for (TetrisBlockButton candidate : buttons) {
            if (!candidate.enabled || candidate == selected) continue;
            // Separate rows/columns by their bounds, avoiding small shape-height offsets.
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

    private static int width(TetrisBlockButton button) {
        int columns = 0;
        for (String row : button.rows) columns = Math.max(columns, row.length());
        return columns * button.cellSize;
    }

    private static int height(TetrisBlockButton button) {
        return button.rows.length * button.cellSize;
    }

    private void refresh() {
        changed.run();
        panel.repaint();
    }
}
