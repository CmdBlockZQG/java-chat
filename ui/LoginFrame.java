package ui;

import javax.swing.*;

// 登陆窗口
public class LoginFrame extends JFrame {
    private JTextField idField; // 用户id输入框
    private JTextField nameField; // 用户名输入框
    private JButton loginButton; // 登陆按钮
    private JPanel panel;

    /**
     * 构造并显示登陆窗口
     */
    public LoginFrame() {
        super("登陆"); // 窗口标题
        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        loginButton.addActionListener(actionEvent -> {
            int id;
            try {
                id = Integer.parseInt(idField.getText()); // 检查id输入框字符串格式
            } catch (NumberFormatException e) {
                // 输入的id不合法
                new MessageDialog("id格式错误！", () -> {});
                return;
            }
            // 登陆
            Main.login(id, nameField.getText());
        });

        pack();
        setVisible(true);
    }
}
