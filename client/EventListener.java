package client;

// 事件处理器接口
public interface EventListener {
    /**
     * 服务器返回错误，断开连接
     * @param msg 错误信息
     */
    void failure(String msg);

    /**
     * 登陆成功，收到当前在线用户列表
     * @param users 用户列表
     */
    void userList(User[] users);

    /**
     * 用户上线
     * @param user 用户对象
     */
    void userOnline(User user);

    /**
     * 用户离线
     * @param userId 用户id
     */
    void userOffline(int userId);

    /**
     * 收到群消息
     * @param userId 发送者id
     * @param time 时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    void groupMsg(int userId, long time, int msgType, String msg);

    /**
     * 收到私聊消息
     * @param userId 发送者id
     * @param time 时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    void dmMsg(int userId, long time, int msgType, String msg);
}
