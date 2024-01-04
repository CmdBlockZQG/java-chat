package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.net.Socket;

class Client {
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
     * 群发通知：某用户下线
     * @param userId 下线用户的id
     */
    private static void notifyUserOffline(int userId) {
        // TODO
    }

    /**
     * 群发通知：某用户上线
     * @param userId 上线用户id
     * @param userName 上线用户名字
     */
    private static void notifyUserOnline(int userId, String userName) {
        // TODO
    }

    private DataOutputStream output; // 写入输出数据流时必须使用synchronized块，防止多个线程同时写入造成数据包错误
    private Socket socket;
    private int userId = 0;
    private String userName;

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

        if (clients.containsKey(id)) { // 同帐号重复登陆
            clients.get(id).close("id在另一个客户端登陆"); // 把前一个用户踢下线
        }
        notifyUserOnline(id, name);
        clients.put(id, this);
        // TODO 登陆成功数据包
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
            synchronized (output) {
                output.writeByte(0); // 错误包类型
                byte[] bytes = msg.getBytes(); // 错误信息字节
                output.writeByte(bytes.length); // 错误信息长度
                output.write(bytes);
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
    public void sendGroupMsg(int msgType, String msg) {
        // TODO
    }

    /**
     * 用户发送私聊消息
     * @param targetId 发送目标id
     * @param msgType 消息内容类型
     * @param msg 消息内容
     */
    public void sendDmMsg(int targetId, int msgType, String msg) {
        // TODO
    }

}
