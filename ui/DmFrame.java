package ui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// 私聊窗口
public class DmFrame extends JFrame {
    /**
     * 创建私聊窗口
     * @param target 私聊的目标用户id
     */
    public DmFrame(int target) {
        super(String.format("私聊 - %s (%d)", Main.userSessions.get(target).user.name, target));

        // 创建封装的聊天面板
        ChatPanel chatPanel = new ChatPanel(this, target);
        Main.chatPanels.put(target, chatPanel); // 设置面板对象
        Main.userSessions.get(target).msgCnt = 0; // 清空未读消息计数

        setContentPane(chatPanel.getPanel());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();
        setVisible(true);

        addWindowListener(new WindowAdapter() { // 当关闭私聊窗口时
            public void windowClosing(WindowEvent e) {
                chatPanel.record.close(); // 关闭面板的消息记录文件
                Main.chatPanels.remove(target); // 移除面板（会导致重新开始统计未读消息数目）
            }
        });
    }
}