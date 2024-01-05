package server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

// 包装一下ByteArrayOutputStream，用来暂存将要发送的数据包
// 其实可以继承DataOutputStream的，但是用那玩意需要处理根本不存在的异常太难用了
class PacketBuffer {
    private final ByteArrayOutputStream buf;

    /**
     * 创建数据包缓冲区
     */
    public PacketBuffer() {
        buf = new ByteArrayOutputStream();
    }

    /**
     * 将数据包缓冲区的字节输出到out
     * @param out 输出目标流
     * @throws IOException 写入目标流异常时抛出
     */
    public void writeTo(OutputStream out) throws IOException {
        buf.writeTo(out);
    }

    /**
     * 向缓冲区写入16位无符号整数
     * @param x 写入x的低16位
     */
    public void writeUint16(int x) {
        buf.write(x >>> 8);
        buf.write(x);
    }

    /**
     * 向缓冲区写入32位无符号整数
     * @param x 写入x的低32位
     */
    public void writeUint32(long x) {
        buf.write((int)(x >>> 24));
        buf.write((int)(x >>> 16));
        buf.write((int)(x >>> 8));
        buf.write((int)(x));
    }

    /**
     * 向缓冲区写入字节
     * @param x 写入x的低8位
     */
    public void write(int x) {
        buf.write(x);
    }

    /**
     * 向缓冲区写入字节数组
     * @param bytes 字节数组
     */
    public void writeBytes(byte[] bytes) {
        buf.writeBytes(bytes);
    }
}
