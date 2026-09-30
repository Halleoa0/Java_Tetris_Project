import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;

/** 메인 메뉴의 기본 Swing 버튼을 세로로 배치한다. */
final class HomePanel extends JPanel {
    HomePanel(ScreenManager screens) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        addButton("게임", screens::startGame);
        add(Box.createVerticalStrut(12));
        addButton("튜토리얼", screens::startTutorial);
        add(Box.createVerticalStrut(12));
        addButton("설정", screens::showSettings);
        add(Box.createVerticalStrut(12));
        addButton("상점", screens::showShop);
        add(Box.createVerticalGlue());
    }

    private void addButton(String title, Runnable action) {
        JButton button = new JButton(title);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(event -> action.run());
        add(button);
    }
}
