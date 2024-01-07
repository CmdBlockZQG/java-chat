package client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

// Socket客户端
public class Client {
    private static final String host = "127.0.0.1"; // 服务器地址
    private static final int port = 1145; // 端口号

    private static final int PACKET_LOGIN = 0; // 登陆包类型
    private static final int PACKET_GROUP_MSG = 1; // 群发消息包类型
    private static final int PACKET_DM_MSG = 2; // 私聊消息包类型
    private static final int PACKET_CLOSE = 127; // 下线数据包类型

    private final int userId; // 用户id
    private final String userName; // 用户名
    private final Socket socket; // socket对象
    private final OutputStream output; // 输出流

    /**
     * 创建socket客户端并登陆
     * @param id 用户id
     * @param name 用户名
     * @throws IOException IO异常
     */
    public Client(int id, String name) throws IOException {
        userId = id;
        userName = name;
        socket = new Socket(host, port); // socket连接对象
        output = socket.getOutputStream(); // 输出流

        login(); // 登陆
    }

    /**
     * 注册事件处理器，并开始接收服务器消息
     * @param listener 事件侦听器
     * @throws IOException IO异常
     */
    public void registerListener(EventListener listener) throws IOException {
        new SocketHandler(socket, listener).start(); // 开始监听服务器消息并产生事件
    }

    /**
     * 发送登陆包
     * @throws IOException IO异常
     */
    private void login() throws IOException {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_LOGIN); // 登陆包类型
        buf.writeUint16(userId); // 用户id
        byte[] bytes = userName.getBytes();
        buf.write(bytes.length); // 用户名字节长度
        buf.writeBytes(bytes); // 用户名字节
        buf.writeTo(output);
    }

    /**
     * 客户端下线
     * @throws IOException IO异常
     */
    public void close() throws IOException {
        output.write(PACKET_CLOSE);
        socket.close();
    }

    /**
     * 发送群消息
     * @param msgType 消息类型
     * @param content 消息内容
     * @throws IOException IO异常
     */
    public void sendGroupMsg(int msgType, String content) throws IOException {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_GROUP_MSG); // 群消息包类型
        buf.write(msgType); // 消息类型
        byte[] bytes = content.getBytes();
        buf.writeUint16(bytes.length); // 消息内容字节数
        buf.writeBytes(bytes); // 消息内容
        buf.writeTo(output); // 发送数据流
    }

    /**
     * 发送私聊消息
     * @param target 目标用户id
     * @param msgType 消息类型
     * @param content 消息内容
     * @throws IOException IO异常
     */
    public void sendDmMsg(int target, int msgType, String content) throws IOException {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_DM_MSG); // 私聊消息包类型
        buf.writeUint16(target); // 私聊目标id
        buf.write(msgType); // 消息类型
        byte[] bytes = content.getBytes();
        buf.writeUint16(bytes.length); // 消息内容字节数
        buf.writeBytes(bytes); // 消息内容
        buf.writeTo(output); // 发送数据流
    }
}
