import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/** 임시 설정 화면 */
final class SettingsPanel extends JPanel {
    SettingsPanel(ScreenManager screens) {
        setLayout(new FlowLayout());
        add(new JLabel("준비 중"));
        JButton back = new JButton("뒤로가기");
        back.addActionListener(event -> screens.showMenu());
        add(back);
    }
}
