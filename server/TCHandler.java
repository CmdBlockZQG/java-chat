package server;


import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

// 临时连接处理器
public class TCHandler {
    private static final int PACKET_TC_UPLOAD = 128; // 临时连接上传包类型
    private static final int PACKET_TC_DOWNLOAD = 129; // 临时连接下载包类型
    private static final int DATA_TIMEOUT = 20000; // 数据包接收超时时间
    private final Socket socket; // 套接字
    private final DataInputStream input; // 输入流
    private final OutputStream output; // 输出流
    private final String id; // 请求的文件id
    private volatile long lastReceiveTime; // 上次收到数据的时间戳。用于检查连接超时

    /**
     * 构造临时连接处理器
     * @param conn socket连接
     * @param in 输入流
     * @param out 输出流
     * @param md5 目标文件md5
     */
    public TCHandler(Socket conn, DataInputStream in, OutputStream out, String md5){
        socket = conn;
        input = in;
        output = out;
        id = md5;
    }

    /**
     * 发送上传结束数据包
     * @param msg 错误信息，null或空字符串表示成功
     * @throws IOException IO异常
     */
    private void endUpload(String msg) throws IOException {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_TC_UPLOAD);
        if (msg == null) {
            buf.write(0);
        } else {
            byte[] bytes = msg.getBytes();
            buf.write(bytes.length);
            buf.writeBytes(bytes);
        }
        buf.writeTo(output);
    }

    /**
     * 启动guard,定时检查上传是否超时
     * @return Timer对象
     */
    private Timer startGuard() {
        lastReceiveTime = System.currentTimeMillis(); // 设置初始时间
        Timer timer = new Timer(true);
        // 每隔DATA_TIMEOUT检查一次是否接收超时
        timer.schedule(new TimerTask() {
            public void run() {
            if (System.currentTimeMillis() - lastReceiveTime > DATA_TIMEOUT) {
                // 至少在过去的DATA_TIMEOUT时间内没有进行任何数据接收
                try {
                    endUpload("上传超时");
                } catch (IOException ignored) {} finally {
                    try {
                        new File("files/tmp-" + id).delete(); // 删除临时文件
                        socket.close(); // 一旦这里关闭连接，接收文件的循环就会抛出IOException，handleUpload函数会直接退出
                    } catch (IOException ignored) {}
                }
            }
            }
        }, 0, DATA_TIMEOUT);
        return timer;
    }

    /**
     * 处理文件上传
     * @throws IOException IO异常
     */
    private void handleUpload() throws IOException {
        Timer guard = startGuard();
        File file = new File("files/tmp-" + id);

        int totLen = input.readInt(); // 文件总长度

        try (FileOutputStream f = new FileOutputStream(file)) {
            byte[] bytes = new byte[1024];
            int curLen;
            // 一旦上传超时，timer会直接关闭socket，这里就会抛出IOException，导致函数直接退出（当然会先执行finally）
            while ((curLen = input.read(bytes)) != -1) {
                lastReceiveTime = System.currentTimeMillis(); // 每次接收刷新时间
                // 将输入流的字节直接写入文件
                f.write(bytes, 0, curLen);
                totLen -= curLen;
                if (totLen <= 0) break; // 上传完成或读取的字节数超过约定长度
            }
        } catch (FileNotFoundException e) {
            endUpload("服务器内部错误");
        } finally {
            // timer必须被关闭，无论在哪退出了
            guard.cancel();
            // 最后必须要检查文件是否正确
            if (totLen != 0) { // 中间出了任何问题都会导致接收到的文件尺寸不对
                // 如果出了问题
                file.delete(); // 删除临时文件
                endUpload("文件上传失败");
            } else { // 文件没出问题
                File newFile = new File("files/" + id);
                if (newFile.isFile() && newFile.exists()) newFile.delete(); //删除可能存在的原文件
                file.renameTo(newFile); // 将临时文件变成正式
                endUpload(null); // 上传成功
            }
        }
    }

    /**
     * 处理文件下载
     * @throws IOException IO异常
     */
    private void handleDownload() throws IOException {
        PacketBuffer buf = new PacketBuffer();
        buf.write(PACKET_TC_DOWNLOAD); // 写入下载数据包类型
        File f = new File("files/" + id);
        if (!f.exists() || !f.isFile()) { // 文件不存在
            byte[] msg = "文件不存在".getBytes(); // 错误信息
            buf.write(msg.length); // 错误信息长度
            buf.writeBytes(msg); // 错误信息字节
            buf.writeTo(output); // 写入socket
            return;
        }
        long totLen = f.length();
        buf.write(0); // 表明没有错误
        buf.writeUint32(totLen); // 总长度
        buf.writeTo(output); // 写入socket,下面开始发送文件内容
        try (FileInputStream in = new FileInputStream("files/" + id)) {
            byte[] bytes = new byte[1024];
            int curLen;
            while ((curLen = in.read(bytes)) != -1) {
                output.write(bytes, 0, curLen);
                totLen -= curLen;
            }
        } catch (FileNotFoundException ignored) {} // 前面已经检查过文件存在性了
    }

    /**
     * 运行临时连接处理器，并在处理完成后关闭socket
     * @param packetType 数据包类型
     */
    public void run(int packetType) {
        try {
            switch (packetType) {
                case PACKET_TC_UPLOAD: handleUpload(); break; // 上传
                case PACKET_TC_DOWNLOAD: handleDownload(); break; // 下载
            }
        } catch (IOException ignored) {} finally {
            try { // 关闭socket,若前面已经关闭过再关一次也无妨
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
