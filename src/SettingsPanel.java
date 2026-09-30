import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

// 환경설정 화면. 모든 변경은 Settings 에 즉시 반영/저장//
final class SettingsPanel extends JPanel {
    private static final Color BG = new Color(41, 41, 41);
    private static final Color CARD = new Color(30, 36, 51);
    private static final Color TEXT = Color.WHITE;
    private static final Color SUB = new Color(164, 174, 195);

    private final Settings s = Settings.get();
    private final Font font = loadFont();

    private final Map<Settings.Action, JButton> keyButtons = new EnumMap<>(Settings.Action.class);
    private final JComboBox<String> profileBox = new JComboBox<>();
    private JPanel palettePreview;
    private Settings.Action waiting; // 키 입력 대기 중인 동작
    private boolean refreshing;

    private final ScreenManager screens;

    SettingsPanel(ScreenManager screens) {
        this.screens = screens;
        build();
    }

    private void build() {
        keyButtons.clear();
        setLayout(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(1280, 720));

        JLabel title = new JLabel("SETTINGS", JLabel.CENTER);
        title.setFont(font.deriveFont(Font.BOLD, 30f));
        title.setForeground(TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(0, 240, 0, 240));
        body.add(soundSection());
        body.add(Box.createVerticalStrut(14));
        body.add(displaySection());
        body.add(Box.createVerticalStrut(14));
        body.add(gameplaySection());
        body.add(Box.createVerticalStrut(14));
        body.add(keySection());
        body.add(Box.createVerticalStrut(20));

        JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        bottom.setOpaque(false);
        JButton resetAll = button("설정 초기화 (키 제외)");
        resetAll.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "키 설정을 제외한 모든 설정을 초기화할까요?", "초기화",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { s.resetAllExceptKeys(); rebuild(); }
        });
        JButton back = button("뒤로가기");
        back.addActionListener(e -> {
            AudioManager.get().playMenuSelect();
            cancelKeyWait();
            screens.showMenu();
        });
        bottom.add(resetAll);
        bottom.add(back);
        add(bottom, BorderLayout.SOUTH);

        refreshProfiles();
        refreshKeyButtons();
    }

    //초기화 후 슬라이더/체크박스 등을 Settings 값으로 다시 맞추기 위해 화면을 다시 구성//
    private void rebuild() {
        removeAll();
        build();
        revalidate();
        repaint();
    }

    //섹션//

    private JPanel soundSection() {
        JPanel p = card("사운드");
        p.add(slider("전체 음량", s::masterVolume, s::setMasterVolume));
        p.add(slider("배경음 크기", s::bgmVolume, s::setBgmVolume));
        p.add(slider("효과음 크기", s::sfxVolume, s::setSfxVolume));
        p.add(check("음소거", s.muted(), s::setMuted));
        p.add(check("창이 비활성화되면 소리 끄기", s.muteWhenUnfocused(), s::setMuteWhenUnfocused));
        return p;
    }

    private JPanel displaySection() {
        JPanel p = card("화면 / 접근성");
        JComboBox<Settings.ColorMode> box = new JComboBox<>(Settings.ColorMode.values());
        box.setRenderer((list, v, i, sel, foc) -> new JLabel(v.label));
        box.setSelectedItem(s.colorMode());
        box.addActionListener(e -> {
            s.setColorMode((Settings.ColorMode) box.getSelectedItem());
            palettePreview.repaint();
        });
        p.add(row("색맹 모드", box));

        palettePreview = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int i = 0;
                for (Tetromino t : Tetromino.values()) {
                    g.setColor(s.minoColor(t));
                    g.fillRoundRect(i++ * 44, 4, 38, 22, 6, 6);
                }
            }
        };
        palettePreview.setOpaque(false);
        palettePreview.setPreferredSize(new Dimension(310, 30));
        p.add(row("색상 미리보기", palettePreview));
        p.add(check("고스트 블록 표시", s.ghostPiece(), s::setGhostPiece));
        p.add(check("격자선 표시", s.showGrid(), s::setShowGrid));
        return p;
    }

    private JPanel gameplaySection() {
        JPanel p = card("게임플레이");
        JComboBox<Settings.Difficulty> diff = new JComboBox<>(Settings.Difficulty.values());
        diff.setRenderer((list, v, i, sel, foc) -> new JLabel(v.label));
        diff.setSelectedItem(s.difficulty());
        diff.addActionListener(e -> s.setDifficulty((Settings.Difficulty) diff.getSelectedItem()));
        p.add(row("난이도", diff));
        p.add(slider("DAS (첫 반복 지연, ms)", s::dasMs, s::setDasMs, 50, 400));
        p.add(slider("ARR (반복 간격, ms)", s::arrMs, s::setArrMs, 0, 120));
        return p;
    }

    private JPanel keySection() {
        JPanel p = card("키 설정 (사용자별)");

        JPanel profileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        profileRow.setOpaque(false);
        profileBox.addActionListener(e -> {
            if (refreshing || profileBox.getSelectedItem() == null) return;
            s.switchProfile((String) profileBox.getSelectedItem());
            refreshKeyButtons();
        });
        JButton add = button("추가");
        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "새 사용자 이름 (공백 없이 16자 이내)");
            if (name != null && !s.createProfile(name))
                JOptionPane.showMessageDialog(this, "사용할 수 없는 이름이거나 이미 존재합니다.");
            refreshProfiles(); refreshKeyButtons();
        });
        JButton del = button("삭제");
        del.addActionListener(e -> {
            if (!s.deleteProfile(s.currentProfile()))
                JOptionPane.showMessageDialog(this, "마지막 사용자는 삭제할 수 없습니다.");
            refreshProfiles(); refreshKeyButtons();
        });
        profileRow.add(label("사용자"));
        profileRow.add(profileBox);
        profileRow.add(add);
        profileRow.add(del);
        p.add(profileRow);
        p.add(Box.createVerticalStrut(8));

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 6));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Settings.Action a : Settings.Action.values()) {
            JButton b = button("");
            b.setFocusTraversalKeysEnabled(false);
            // SPACE/ENTER 로 버튼이 눌리는 기본 동작 제거 (키 지정 중 SPACE 를 눌렀을 때 대기 모드가 다시 켜지는 문제 방지)
            for (String ks : new String[] {"SPACE", "released SPACE", "ENTER", "released ENTER"})
                b.getInputMap(JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(ks), "none");
            b.addActionListener(e -> beginKeyWait(a, b));
            b.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (waiting != a) return;
                    int code = e.getKeyCode();
                    if (code == KeyEvent.VK_ESCAPE) { cancelKeyWait(); return; }
                    if (!s.setKey(a, code)) {
                        b.setText("사용 불가 키");
                        return;
                    }
                    waiting = null;
                    refreshKeyButtons();
                    e.consume();
                }
            });
            keyButtons.put(a, b);
            JLabel l = label(a.label);
            grid.add(l);
            grid.add(b);
        }
        p.add(grid);
        p.add(Box.createVerticalStrut(8));
        JButton reset = button("현재 사용자 키 초기화");
        reset.addActionListener(e -> { s.resetKeys(); refreshKeyButtons(); });
        p.add(reset);
        JLabel hint = new JLabel("※ ESC / ENTER / M 은 일시정지·메뉴용이라 지정할 수 없어요. 이미 쓰는 키는 서로 교환됩니다.");
        hint.setForeground(SUB);
        hint.setFont(font.deriveFont(12f));
        p.add(hint);
        return p;
    }

    //키 입력 대기//

    private void beginKeyWait(Settings.Action a, JButton b) {
        cancelKeyWait();
        waiting = a;
        b.setText("키를 누르세요... (ESC 취소)");
        b.requestFocusInWindow();
    }

    private void cancelKeyWait() {
        if (waiting != null) { waiting = null; refreshKeyButtons(); }
    }

    private void refreshKeyButtons() {
        for (var e : keyButtons.entrySet()) e.getValue().setText(s.keyText(e.getKey()));
    }

    private void refreshProfiles() {
        refreshing = true;
        profileBox.removeAllItems();
        for (String n : s.profileNames()) profileBox.addItem(n);
        profileBox.setSelectedItem(s.currentProfile());
        refreshing = false;
    }

    // 화면 구성 도우미 함수 (UI 컴포넌트 생성) //

    private JPanel card(String titleText) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = new JLabel(titleText);
        t.setForeground(new Color(75, 205, 235));
        t.setFont(font.deriveFont(Font.BOLD, 18f));
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        p.add(t);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(font.deriveFont(14f));
        return l;
    }

    private JPanel row(String name, JComponent c) {
        JPanel r = new JPanel(new BorderLayout(12, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = label(name);
        l.setPreferredSize(new Dimension(220, 28));
        r.add(l, BorderLayout.WEST);
        r.add(c, BorderLayout.CENTER);
        return r;
    }

    private JPanel slider(String name, IntSupplier get, IntConsumer set) { return slider(name, get, set, 0, 100); }

    private JPanel slider(String name, IntSupplier get, IntConsumer set, int min, int max) {
        JSlider sl = new JSlider(min, max, get.getAsInt());
        sl.setOpaque(false);
        JLabel val = label(String.valueOf(sl.getValue()));
        val.setPreferredSize(new Dimension(40, 28));
        sl.addChangeListener(e -> {
            val.setText(String.valueOf(sl.getValue()));
            set.accept(sl.getValue());
        });
        JPanel r = row(name, sl);
        r.add(val, BorderLayout.EAST);
        return r;
    }

    private JCheckBox check(String text, boolean value, Consumer<Boolean> set) {
        JCheckBox c = new JCheckBox(text, value);
        c.setOpaque(false);
        c.setForeground(TEXT);
        c.setFont(font.deriveFont(14f));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.addActionListener(e -> set.accept(c.isSelected()));
        return c;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(font.deriveFont(13f));
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    private static Font loadFont() {
        try { return MenuFonts.loadKRBlack(); }
        catch (RuntimeException e) { return new Font("SansSerif", Font.PLAIN, 14); }
    }
}
