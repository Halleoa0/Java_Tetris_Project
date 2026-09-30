import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/** 상점 화면의 임시 자리표시자. */
final class ShopPanel extends JPanel {
    ShopPanel(ScreenManager screens) {
        setLayout(new FlowLayout());
        add(new JLabel("준비 중"));
        JButton back = new JButton("뒤로가기");
        back.addActionListener(event -> screens.showMenu());
        add(back);
    }
}
