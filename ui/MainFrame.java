package ui;

import client.User;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 主窗口
public class MainFrame extends JFrame {
    private JPanel contentPane; // 主容器
    private JList<UserSession> sessionList; // 在线用户列表
    private JLabel idLabel; // 展示用户id的标签
    private JLabel nameLabel; // 展示用户名的标签
    private JPanel chatPanel; // 群聊天面板

    private void createUIComponents() {
        // 创建主窗口的面板
        ChatPanel cp = new ChatPanel(this, ChatPanel.TARGET_GROUP);
        chatPanel = cp.getPanel();
        Main.groupPanel = cp;
    }

    /**
     * 更新ui列表
     */
    void updateUserSessionList() {
        // 使用sessionList的值集合
        sessionList.setListData(Main.userSessions.values().toArray(new UserSession[0]));
    }

    /**
     * 构造主窗口
     */
    public MainFrame() {
        super("Java Chat"); // 标题
        setContentPane(contentPane); // 设置主容器
        setLocationRelativeTo(null); // 默认位置屏幕中央
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 关闭时退出

        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 在线用户列表不允许多选

        // 展示登陆用户的id和名字
        idLabel.setText(String.valueOf(Main.userId));
        nameLabel.setText(Main.userName);

        // 处理双击列表项打开对应私聊窗口事件
        sessionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击列表项
                    User user = sessionList.getSelectedValue().user;
                    if (Main.chatPanels.containsKey(user.id)) {
                        Main.chatPanels.get(user.id).parent.requestFocus(); // 如果私聊窗口已经打开，则聚焦过去
                    } else {
                        new DmFrame(user.id).requestFocus(); // 打开私聊窗口，并聚焦过去
                    }
                }
            }
        });
    }

    /**
     * 显示主窗口
     */
    public void open() {
        // 调整大小并展示
        pack();
        setVisible(true);
    }
}
