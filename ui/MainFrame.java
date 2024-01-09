package ui;

import javax.swing.*;
import java.util.Hashtable;

import client.User;

// 主窗口
public class MainFrame extends JFrame {
    private JPanel contentPane; // 主容器
    private JList<ListItem> sessionList; // 在线用户列表
    private JLabel idLabel; // 展示用户id的标签
    private JLabel nameLabel; // 展示用户名的标签
    private JPanel chatPanel; // 群聊天面板

    private void createUIComponents() {
        // 从封装的聊天面板类创建群聊面板
        chatPanel = new ChatPanel().getPanel();
    }

    // 在线用户列表列表项
    private static class ListItem {
        final User user; // 用户对象
        int msgCnt; // 未读消息数量

        /**
         * 构造列表项
         * @param user 用户对象
         */
        public ListItem(User user) {
            this.user = user;
            msgCnt = 0;
        }

        /**
         * ui列表中展示的字符串生成
         * @return 用于展示的字符串
         */
        public String toString() {
            if (msgCnt != 0) return String.format("%d条新消息 - %s (%d)", msgCnt, user.name, user.id);
            else return String.format("%s (%d)", user.name, user.id);
        }
    }

    private final Hashtable<Integer, ListItem> listItems = new Hashtable<>(); // 在线用户id对应列表项

    /**
     * 更新ui列表
     */
    private void updateList() {
        // 使用sessionList的值集合
        sessionList.setListData(listItems.values().toArray(new ListItem[0]));
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
    }

    /**
     * 登陆成功，服务器返回在线用户列表事件处理
     * @param users 用户列表
     */
    void setUserList(User[] users) {
        for (User user : users) {
            // 添加到hashtable
            listItems.put(user.id, new ListItem(user));
        }
        // 更新ui展示
        updateList();
    }

    /**
     * 用户上线事件处理
     * @param user 用户对象
     */
    void userOnline(User user) {
        // 修改hashtable并更新ui展示
        listItems.put(user.id, new ListItem(user));
        updateList();
    }

    /**
     * 用户离线事件处理
     * @param userId 用户id
     */
    void userOffline(int userId) {
        // 修改hashtable并更新ui展示
        listItems.remove(userId);
        updateList();
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
