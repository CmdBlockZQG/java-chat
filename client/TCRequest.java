package client;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;

// 临时连接请求
public class TCRequest {
    private static final String host = "127.0.0.1"; // 服务器地址
    private static final int port = 1145; // 端口号
    private static final int PACKET_TC_UPLOAD = 128; // 临时连接上传包类型
    private static final int PACKET_TC_DOWNLOAD = 129; // 临时连接下载包类型

    /**
     * 文件上传
     * @param filename 欲上传的文件名
     * @return 文件md5
     * @throws IOException 上传失败
     */
    public static String upload(String filename) throws IOException {
        File file = new File(filename); // 打开文件
        long totLen = 0; // 文件总长度计数器
        // 创建md5 digest
        MessageDigest md5Digest = null;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (Exception ignored) {} // MD5肯定存在啊
        // 文件输入流
        DigestInputStream din = new DigestInputStream(new FileInputStream(file), md5Digest);
        // 循环将文件全部过一遍，计算md5同时统计文件总长度
        byte[] fileBuf = new byte[1024];
        int curLen;
        while ((curLen = din.read(fileBuf)) != -1) totLen += curLen;

        assert md5Digest != null;
        // md5Digest.digest()得到md5字节，利用Bigint类将其转换为16进制字符串
        byte[] md5 = new BigInteger(1, md5Digest.digest()).toString(16).getBytes(); // 计算得到md5值

        // 打开socket连接
        Socket conn = new Socket(host, port);
        OutputStream output = conn.getOutputStream();
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_TC_UPLOAD); // 上传包类型
        buf.writeBytes(md5); // md5值
        buf.writeUint32(totLen); // 文件总长度
        buf.writeTo(output);

        // 写入文件内容
        FileInputStream fin = new FileInputStream(file);
        while ((curLen = fin.read(fileBuf)) != -1) output.write(fileBuf, 0, curLen);
        // output.close();

        // 打开输入流接收服务器返回
        String md5Str = new String(md5); // 返回md5值
        // 打开输入流，读取服务器返回的信息
        DataInputStream input = new DataInputStream(conn.getInputStream());
        input.skipBytes(1); // 跳过返回包类型
        int msgLen = input.readUnsignedByte(); // 错误信息长度
        if (msgLen != 0) { // 上传失败
            byte[] msgBytes = new byte[msgLen];
            input.readFully(msgBytes);
            conn.close();
            throw new IOException(new String(msgBytes));
        }
        // 关闭连接返回结果
        conn.close();
        return md5Str;
    }

    /**
     * 从输入流读取无符号32位整数
     * @return 读取结果
     * @throws IOException IO异常
     */
    private static long readUint32(DataInputStream input) throws IOException {
        return ((long)input.readUnsignedByte() << 24) +
                ((long)input.readUnsignedByte() << 16) +
                ((long)input.readUnsignedByte() << 8) +
                (long)input.readUnsignedByte();
    }

    /**
     * 文件下载
     * @param id 欲下载的文件id
     * @param filename 保存位置
     * @throws IOException 下载失败
     */
    public static void download(String id, String filename) throws IOException {
        Socket conn = new Socket(host, port); // 打开socket连接
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_TC_DOWNLOAD); // 发送下载包类型
        byte[] md5Bytes = id.getBytes();
        if (md5Bytes.length != 32) throw new IOException("文件id错误");
        buf.writeBytes(id.getBytes()); // 文件id
        buf.writeTo(conn.getOutputStream());

        // socket输入流
        DataInputStream input = new DataInputStream(conn.getInputStream());
        input.skipBytes(1); // 跳过返回包类型
        int msgLen = input.readUnsignedByte(); // 错误信息长度
        if (msgLen != 0) { // 服务器返回了错误
            // 读取错误信息
            byte[] msgBytes = new byte[msgLen];
            input.readFully(msgBytes);
            conn.close(); // 关闭socket
            // 抛出错误信息
            throw new IOException(new String(msgBytes));
        }

        // 打开目标文件输出流
        File file = new File(filename);
        OutputStream output = new FileOutputStream(file);

        // 接收文件内容
        long totLen = readUint32(input); // 文件总长度
        int curLen;
        byte[] bytes = new byte[1024];
        while ((curLen = input.read(bytes)) != -1) {
            output.write(bytes, 0, curLen);
            totLen -= curLen;
            if (totLen == 0) break;
        }
        // 成功接收文件，关闭文件及socket连接
        output.close();
        conn.close();
    }

    // 测试用主函数
    public static void main(String[] args) throws IOException {
        System.out.println(upload("client/Client.java"));
        // System.out.println(download("c08430fbed443cb7a52059ba4f7ae381", "haha"));
    }
}
