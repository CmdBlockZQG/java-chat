package ui;

import client.Client;
import client.EventListener;
import client.User;

// 服务器事件处理器
class EventHandler implements EventListener {
    private final Client client;

    /**
     * 构造事件处理器
     * @param client 客户端对象
     */
    public EventHandler(Client client) {
        this.client = client;
    }

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
        System.out.println("登陆成功");
        for (User user : users) {
            System.out.println(user);
        }
    }

    /**
     * 用户上线
     * @param user 用户对象
     */
    public void userOnline(User user) {

    }

    /**
     * 用户离线
     * @param userId 用户id
     */
    public void userOffline(int userId) {

    }

    /**
     * 收到群消息
     * @param userId 发送者id
     * @param time 时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    public void groupMsg(int userId, long time, int msgType, String msg) {

    }

    /**
     * 收到私聊消息
     * @param userId 发送者id
     * @param time 时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    public void dmMsg(int userId, long time, int msgType, String msg) {

    }
}
