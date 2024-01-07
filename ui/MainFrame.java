package ui;

import javax.swing.*;

// 主窗口
public class MainFrame extends JFrame {
    private JPanel panel;

    public MainFrame() {
        super("Java Chat");
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }
}
