import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/** 임시 상점 화면 */
final class ShopPanel extends JPanel {
    ShopPanel(ScreenManager screens) {
        setLayout(new FlowLayout());
        add(new JLabel("준비 중"));
        JButton back = new JButton("뒤로가기");
        back.addActionListener(event -> {
            AudioManager.get().playMenuSelect();
            screens.showMenu();
        });
        add(back);
    }
}
