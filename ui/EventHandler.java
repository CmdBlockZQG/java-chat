package ui;

import client.EventListener;
import client.User;

// 服务器事件处理器
class EventHandler implements EventListener {
    /**
     * 服务器返回错误，断开连接
     * @param msg 错误信息
     */
    public void failure(String msg) {
        new MessageDialog(msg, () -> System.exit(0));
    }

    /**
     * 登陆成功，收到当前在线用户列表
     * @param users 用户列表
     */
    public void userList(User[] users) {
        // 写入用户会话列表
        for (User user : users) {
            Main.userSessions.put(user.id, new UserSession(user));
        }
        // 更新主窗口ui列表
        Main.mainFrame.updateUserSessionList();
    }

    /**
     * 用户上线
     * @param user 用户对象
     */
    public void userOnline(User user) {
        // 更新用户会话列表
        Main.userSessions.put(user.id, new UserSession(user));
        // 更新主窗口ui列表
        Main.mainFrame.updateUserSessionList();
    }

    /**
     * 用户离线
     * @param userId 用户id
     */
    public void userOffline(int userId) {
        // 更新用户会话列表
        Main.userSessions.remove(userId);
        // 更新主窗口ui列表
        Main.mainFrame.updateUserSessionList();
    }

    /**
     * 收到群消息
     * @param userId 发送者id
     * @param time 时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    public void groupMsg(int userId, long time, int msgType, String msg) {
        User user = Main.userSessions.get(userId).user;
        Main.groupPanel.displayMsg(user, time, msgType, msg); // 在主窗口展示消息
        Main.groupRecord.append(user ,time, msgType, msg); // 写入消息记录
    }

    /**
     * 收到私聊消息
     * @param userId 发送者id
     * @param time 时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    public void dmMsg(int userId, long time, int msgType, String msg) {
        User user = Main.userSessions.get(userId).user;
        if (Main.chatPanels.containsKey(userId)) { // 私聊窗口已经打开
            ChatPanel chatPanel = Main.chatPanels.get(userId);
            // 在私聊窗口展示消息
            chatPanel.displayMsg(user, time, msgType, msg);
            // 写入消息记录文件
            chatPanel.record.append(user, time, msgType, msg);
        } else {
            Main.userSessions.get(userId).msgCnt++; // 未读消息+1
            Main.mainFrame.updateUserSessionList(); // 更新主窗口列表
            // 写入消息记录文件
            MsgRecord.appendEntry(userId, new MsgRecord.Entry(user, time, msgType, msg));
        }
    }
}
