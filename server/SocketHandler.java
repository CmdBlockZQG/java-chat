package server;

import java.net.Socket;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;

// socket处理器
class SocketHandler implements Runnable {
    private static final int LOGIN_TIMEOUT = 5000; // 登陆超时时间
    // 客户端->服务端数据包类型常量
    private static final int PACKET_LOGIN = 0;
    private static final int PACKET_GROUP_MSG = 1;
    private static final int PACKET_DM_MSG = 2;

    private Socket socket; // socket连接
    private DataInputStream input; // 输入数据流
    private OutputStream output; // 输出数据流
    private volatile boolean isLogined = false; // 是否已经登陆
    private Client client;

    /**
     * 构造socket处理器
     * @param socket 连接socket
     * @throws IOException
     */
    public SocketHandler(Socket socket) throws IOException {
        this.socket = socket;
        input = new DataInputStream(socket.getInputStream());
        output = socket.getOutputStream();
    }

    /** 
     * 在未登录的情况下结束返回错误信息并断开连接，登陆后应使用Client中的close
     * @param msg 错误信息
     */
    private void close(String msg) {
        assert(!isLogined); // 本方法应该只在登陆之前调用
        // 因为还没登陆，输出流只有这里会操作
        // 所以不用同步，直接写入即可
        try { // 发送错误返回包并关闭连接，忽略异常
            output.write(0); // 错误包类型
            byte[] bytes = msg.getBytes(); // 错误信息字节
            output.write(bytes.length); // 错误信息长度
            output.write(bytes); // 错误信息字节
            socket.close(); // 发送完错误信息直接下线
        } catch (Exception e) { }
    }

    /**
     * 处理登陆数据包
     * @throws IOException
     */
    private void login() throws IOException {
        int userId = input.readUnsignedShort(); // 用户id
        int nameLength = input.readUnsignedByte(); // 用户名字符串长度
        byte[] nameBytes = new byte[nameLength];
        input.readFully(nameBytes); // 读取用户名字节
        String name = new String(nameBytes); // 用户名字符串

        client = new Client(socket, userId, name); // 注册已登录的客户端
    }

    /**
     * 处理群发消息数据包
     * @throws IOException
     */
    private void groupMsg() throws IOException {
        int msgType = input.readUnsignedByte(); // 消息类型
        int msgLen = input.readUnsignedShort(); // 消息长度
        byte[] msgBytes = new byte[msgLen];
        input.readFully(msgBytes); // 消息内容字节

        client.sendGroupMsg(msgType, msgBytes);
    }

    /**
     * 处理私聊消息数据包
     * @throws IOException
     */
    private void dmMsg() throws IOException {
        int targetId = input.readUnsignedShort(); // 目标用户id
        int msgType = input.readUnsignedByte(); // 消息类型
        int msgLen = input.readUnsignedShort(); // 消息长度
        byte[] msgBytes = new byte[msgLen];
        input.readFully(msgBytes); // 消息内容字节

        client.sendDmMsg(targetId, msgType, msgBytes);
    }

    /**
     * 对Socket的服务线程
     */
    public void run() {
        // 开一个线程，LOGIN_TIMEOUT时间之后判断是否登陆成功，如果登陆超时直接踢下线
        new Thread() {
            public void run() {
                // 先睡眠LOGIN_TIMEOUT时间
                try {
                    Thread.sleep(LOGIN_TIMEOUT);
                } catch (InterruptedException e) {
                    System.err.println(e);
                    e.printStackTrace();
                    return;
                }
                if (!isLogined) { // 判断是否已经登陆
                    // 未登录，直接踢下线
                    close("登陆超时");
                }
            }
        }.start();

        // 读取数据包类型字节
        int packetType;
        try {
            packetType = input.readByte(); // 数据包类型，应该为0
        } catch (IOException e) {
            close("登陆数据包错误");
            return;
        }
        // 第一个包不是登陆包
        if (packetType != PACKET_LOGIN) {
            close("您还未登陆"); // 踢下线
            return;
        }
        // 登陆
        try {
            login();
        } catch (IOException e) { // 登陆有问题
            close("登陆数据包错误");
            return;
        }

        isLogined = true;

        while (true) {
            try {
                packetType = input.readByte(); // 数据包类型
                client.status.setStatus(true);
                switch (packetType) {
                    case PACKET_GROUP_MSG: groupMsg(); break;
                    case PACKET_DM_MSG: dmMsg(); break;
                    default: client.close("数据包类型错误"); break;
                }
                client.status.setStatus(false);
            } catch (IOException e) {
                // 数据包超时将会使得输入流产生异常，因为socket已经被ConnCheck关闭
                // 此时用户应该已经下线，直接退出服务线程即可
                return;
            }
        }
    }

}
