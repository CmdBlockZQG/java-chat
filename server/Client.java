package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.net.Socket;

class Client {
    // 服务端->客户端数据包类型常量
    private static final int PACKET_LOGIN_SUCCESS = 1;
    private static final int PACKET_USER_ONLINE = 2;
    private static final int PACKET_USER_OFFLINE = 3;
    private static final int PACKET_GROUP_MSG = 4;
    private static final int PACKET_DM_MSG = 5;


    private static Hashtable<Integer, Client> clients = new Hashtable<Integer, Client>();
    
    private static final long CONN_CHECK_PERIOD = 60000; // 检查客户端超时的间隔时间
    /**
     * 检查所有已登录客户端数据包是否超时
     */
    private static void checkConn() {
        for (Client client : clients.values()) {
            if (client.status.isTimeout()) { // 这个客户端超时了
                client.close("数据包超时");
            }
        }
    }
    static { // ConnCheck
        Timer timer = new Timer(true); // 后台线程
        timer.schedule(new TimerTask() {
            public void run() {
                checkConn();
            }
        }, 0, CONN_CHECK_PERIOD);
    }


    /**
     * 向所有在线用户广播数据
     * @param buf 字节流
     */
    private static void broadCast(PacketBuffer buf) {
        for (Client client : clients.values()) {
            synchronized (client.output) {
                try {
                    buf.writeTo(client.output);
                } catch (IOException e) { }
            }
        }
    }

    /**
     * 群发通知：某用户下线
     * @param userId 下线用户的id
     */
    private static void notifyUserOffline(int userId) {
        PacketBuffer buf = new PacketBuffer();
        // 用户下线数据包类型
        buf.write(PACKET_USER_OFFLINE);
        // 用户id
        buf.writeUint16(userId);
        // 向所有在线用户进行群发
        broadCast(buf);
    }

    /**
     * 群发通知：某用户上线
     * @param userId 上线用户id
     * @param userNameBytes 上线用户名字节
     */
    private static void notifyUserOnline(int userId, byte[] userNameBytes) {
        PacketBuffer buf = new PacketBuffer();
        // 用户上线数据包类型
        buf.write(PACKET_USER_ONLINE);
        // 用户id
        buf.writeUint16(userId);
        // 用户名字节数
        buf.write(userNameBytes.length);
        // 用户名字节
        buf.writeBytes(userNameBytes);
        // 向所有在线用户进行群发
        broadCast(buf);
    }

    /**
     * 获取当前在线用户列表（登陆成功包）
     * @return 数据流
     */
    private static PacketBuffer getUserList() {
        PacketBuffer buf = new PacketBuffer();
        int cnt = clients.size();
        // 在线用户数量
        buf.writeUint16(cnt);
        for (Client client : clients.values()) {
            // 当前用户id
            buf.writeUint16(client.userId);
            // 当前用户用户名长度
            buf.write(client.userNameBytes.length);
            // 用户名文本
            buf.writeBytes(client.userNameBytes);
        }
        return buf;
    }

    private DataOutputStream output; // 写入输出数据流时必须使用synchronized块，防止多个线程同时写入造成数据包错误
    private Socket socket;
    private int userId = 0;
    private String userName;
    private byte[] userNameBytes;

    public ConnStatus status; // 接收数据包状态对象，用于检测链接超时

    /**
     * 用户登陆
     * @param conn 用户Socket对象
     * @param id 用户id
     * @param name 用户名
     * @throws IOException
     */
    public Client(Socket conn, int id, String name) throws IOException {
        socket = conn;
        output = new DataOutputStream(conn.getOutputStream());
        userId = id;
        userName = name;
        userNameBytes = name.getBytes();

        if (clients.containsKey(id)) { // 同帐号重复登陆
            clients.get(id).close("id在另一个客户端登陆"); // 把前一个用户踢下线
        }
        notifyUserOnline(id, userNameBytes); // 通知所有在线用户
        clients.put(id, this);
        // 发送登陆成功数据包
        synchronized (output) {
            // 数据包类型
            output.write(PACKET_LOGIN_SUCCESS);
            // 用户列表
            getUserList().writeTo(output);
        }
    }

    /**
     * 关闭用户连接
     * @param msg 错误/提示信息
     */
    public void close(String msg) {
        // 用户已经下线，忽略
        if (!clients.containsKey(userId)) {
            return;
        }
        clients.remove(userId);
        try {
            byte[] bytes = msg.getBytes(); // 错误信息字节
            synchronized (output) {
                output.writeByte(0); // 错误包类型
                output.writeByte(bytes.length); // 错误信息长度
                output.write(bytes); // 错误信息字节
            }
            socket.close();
        } catch (IOException e) {

        } finally {
            notifyUserOffline(userId);
        }
        
    }

    /**
     * 用户发送群消息
     * @param msgType 消息内容类型
     * @param msg 消息内容
     */
    public void sendGroupMsg(int msgType, byte[] msg) {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_GROUP_MSG); // 群发消息数据包类型
        buf.writeUint16(userId); // 发送者id
        buf.writeUint32(System.currentTimeMillis() / 1000); // 时间戳
        buf.write(msgType); // 消息类型
        buf.writeUint16(msg.length); // 消息内容长度
        buf.writeBytes(msg); // 内容字节

        broadCast(buf);
    }

    /**
     * 用户发送私聊消息
     * @param targetId 发送目标id
     * @param msgType 消息内容类型
     * @param msg 消息内容
     */
    public void sendDmMsg(int targetId, int msgType, byte[] msg) {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_DM_MSG); // 私聊消息数据包类型
        buf.writeUint16(userId); // 发送者id
        buf.writeUint32(System.currentTimeMillis() / 1000); // 时间戳
        buf.write(msgType); // 消息类型
        buf.writeUint16(msg.length); // 消息内容长度
        buf.writeBytes(msg); // 内容字节

        Client target = clients.get(targetId);
        if (target == null) return;
        synchronized (target.output) {
            try {
                buf.writeTo(target.output);
            } catch (IOException e) { }
        }
    }

}
