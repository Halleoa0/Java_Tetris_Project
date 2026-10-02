package ui;

import app.ScreenManager;
import audio.AudioManager;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;



/** 임시 상점 화면 */
public final class ShopPanel extends JPanel {
    public ShopPanel(ScreenManager screens) {
        setLayout(new FlowLayout());
        add(new JLabel("준비 중"));
        JButton back = new JButton("뒤로가기");
        back.addActionListener(event -> {
            AudioManager.get().playMenuSelect();
            screens.showHome();
        });
        add(back);
    }
}
