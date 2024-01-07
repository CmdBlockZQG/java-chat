package client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

// 处理服务器socket连接，接收来自服务器的数据包
class SocketHandler extends Thread {
    private static final int PACKET_FAILURE = 0; // 错误数据包类型
    private static final int PACKET_LOGIN_SUCCESS = 1; // 登陆成功数据包类型
    private static final int PACKET_USER_ONLINE = 2; // 用户上线数据包类型
    private static final int PACKET_USER_OFFLINE = 3; // 用户下线数据包类型
    private static final int PACKET_GROUP_MSG = 4; // 群消息数据包类型
    private static final int PACKET_DM_MSG = 5; // 私聊消息数据包类型

    private final DataInputStream input; // 输入流
    private final EventListener eventListener; // 事件处理器

    /**
     * 从输入流读取无符号32位整数
     * @return 读取结果
     * @throws IOException IO异常
     */
    private long readUint32() throws IOException {
        return ((long)input.readUnsignedByte() << 24) +
                ((long)input.readUnsignedByte() << 16) +
                ((long)input.readUnsignedByte() << 8) +
                (long)input.readUnsignedByte();
    }

    /**
     * 创建socket监听器
     * @param conn socket对象
     * @param listener 时间处理器
     * @throws IOException IO异常
     */
    public SocketHandler(Socket conn, EventListener listener) throws IOException {
        input = new DataInputStream(conn.getInputStream()); // 输入流
        eventListener = listener; // 事件处理器
    }

    /**
     * 错误数据包处理
     * @throws IOException IO异常
     */
    private void failure() throws IOException {
        int len = input.readUnsignedByte(); // 错误信息长度
        byte[] msg = new byte[len];
        input.readFully(msg); // 读取错误信息字符串字节

        eventListener.failure(new String(msg)); // 发射错误事件
    }

    /**
     * 登陆成功（在线用户列表）数据包处理
     * @throws IOException IO异常
     */
    private void loginSuccess() throws IOException {
        int cnt = input.readUnsignedShort(); // 在线用户数量
        User[] users = new User[cnt]; // 用户列表
        for (int i = 0; i < cnt; ++i) {
            int id = input.readUnsignedShort(); // 用户id
            int len = input.readUnsignedByte(); // 用户名字节数
            byte[] bytes = new byte[len]; // 用户名
            input.readFully(bytes); // 读取用户名字节
            users[i] = new User(id, new String(bytes)); // 创建用户对象
        }
        eventListener.userList(users); // 发射用户列表事件
    }

    /**
     * 用户上线数据包处理
     * @throws IOException IO异常
     */
    private void userOnline() throws IOException {
        int id = input.readUnsignedShort(); // 用户id
        int len = input.readUnsignedByte(); // 用户名字节数
        byte[] bytes = new byte[len]; // 用户名
        input.readFully(bytes); // 读取用户名字节
        User user = new User(id, new String(bytes)); // 创建用户对象

        eventListener.userOnline(user); // 发射用户上线事件
    }

    /**
     * 用户离线数据包处理
     * @throws IOException IO异常
     */
    private void userOffline() throws IOException {
        int id = input.readUnsignedShort(); // 用户id
        eventListener.userOffline(id); // 发射用户离线事件
    }

    /**
     * 群消息数据包处理
     * @throws IOException IO异常
     */
    private void groupMsg() throws IOException {
        int userId = input.readUnsignedShort(); // 发送者id
        long time = readUint32(); // 时间戳
        int msgType = input.readUnsignedByte(); // 消息类型
        int len = input.readUnsignedShort(); // 消息字节数
        byte[] bytes = new byte[len];
        input.readFully(bytes); // 消息内容字节
        eventListener.groupMsg(userId, time, msgType, new String(bytes)); // 发射群消息事件
    }

    /**
     * 私聊消息数据包处理
     * @throws IOException IO异常
     */
    private void dmMsg() throws IOException {
        int userId = input.readUnsignedShort(); // 发送者id
        long time = readUint32(); // 时间戳
        int msgType = input.readUnsignedByte(); // 消息类型
        int len = input.readUnsignedShort(); // 消息字节数
        byte[] bytes = new byte[len];
        input.readFully(bytes); // 消息内容字节
        eventListener.dmMsg(userId, time, msgType, new String(bytes)); // 发射私聊消息事件
    }

    /**
     * IO循环，接受来自服务器的数据包
     */
    public void run() {
        int packetType;
        while (true) {
            try {
                packetType = input.readUnsignedByte(); // 数据包类型
                switch (packetType) {
                    case PACKET_FAILURE: failure(); return; // 错误数据包之后服务器将立即退出
                    case PACKET_LOGIN_SUCCESS: loginSuccess(); break; // 登陆成功（在线用户列表）
                    case PACKET_USER_ONLINE: userOnline(); break; // 用户上线
                    case PACKET_USER_OFFLINE: userOffline(); break; // 用户离线
                    case PACKET_GROUP_MSG: groupMsg(); break; // 群消息
                    case PACKET_DM_MSG: dmMsg(); break; // 私聊消息
                    default: eventListener.failure("与服务器连接异常"); return; // 理论上不应该到达的分支
                }
            } catch (IOException e) {
                eventListener.failure("与服务器连接异常");
                return; // 连接IO错误可以remake了
            }
        }
    }
}
