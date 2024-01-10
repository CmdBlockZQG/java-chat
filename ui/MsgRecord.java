package ui;

import common.PacketBuffer;
import client.User;

import java.io.*;
import java.util.ArrayList;

// 消息记录文件抽象
class MsgRecord {
    // 内部类：消息记录条目
    public static class Entry implements Serializable{
        public final int userId; // 用户id
        public final String userName; // 用户名称
        public final long time; // 消息时间
        public final int msgType; // 消息类型
        public final String msg; // 消息内容

        /**
         * 构造消息记录
         * @param user 发送者对象
         * @param time 时间
         * @param msgType 消息类型
         * @param msg 消息内容
         */
        public Entry(User user, long time, int msgType, String msg) {
            this(user.id, user.name, time, msgType, msg);
        }

        /**
         * 构造消息记录
         * @param userId 发送者id
         * @param userName 发送者名称
         * @param time 时间
         * @param msgType 消息类型
         * @param msg 消息内容
         */
        public Entry(int userId, String userName, long time, int msgType, String msg) {
            this.userId = userId;
            this.userName = userName;
            this.time = time;
            this.msgType = msgType;
            this.msg = msg;
        }

        /**
         * 将消息记录条目写入到指定流中
         * @param stream 目标输出流
         * @throws IOException IO异常
         */
        public void writeTo(OutputStream stream) throws IOException {
            byte[] nameBytes = userName.getBytes(); // 用户名字节
            byte[] msgBytes = msg.getBytes(); // 消息内容字节
            PacketBuffer buf = new PacketBuffer();
            buf.writeUint16(userId); // 用户id
            buf.write(nameBytes.length); // 用户名字节长度
            buf.writeBytes(nameBytes); // 用户名字节
            buf.writeUint32(time); // 时间
            buf.write(msgType); // 消息类型
            buf.writeUint16(msgBytes.length); // 消息内容字节长度
            buf.writeBytes(msgBytes); // 消息内容字节
            buf.writeTo(stream); // 写入到输出流
        }

        /**
         * 从输入流读取无符号32位整数
         * @param input 输入流
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
         * 从指定输入流中读取消息记录条目
         * @param stream 输入流
         * @return 消息记录条目对象
         * @throws IOException IO异常
         */
        public static Entry readFromStream(InputStream stream) throws IOException {
            DataInputStream in = new DataInputStream(stream); // 数据输入流包装
            int userId = in.readUnsignedShort(); // 用户id
            int nameBytesLen = in.readUnsignedByte(); // 用户名字节长度
            byte[] nameBytes = new byte[nameBytesLen]; // 用户名字节
            in.readFully(nameBytes);
            long time = readUint32(in); // 消息时间
            int msgType = in.readUnsignedByte(); // 消息类型
            int msgBytesLen = in.readUnsignedShort(); // 消息内容字节长度
            byte[] msgBytes = new byte[msgBytesLen]; // 消息内容字节
            in.readFully(msgBytes);
            // 构造结果：消息记录条目对象
            return new Entry(userId, new String(nameBytes), time, msgType, new String(msgBytes));
        }
    }
    private final File file; // 记录文件
    private final FileOutputStream out; // 记录文件输出流

    /**
     * 构造消息记录文件抽象
     * @param target 消息记录目标id
     */
    public MsgRecord(int target) {
        int owner = Main.userId;
        // 存放消息记录文件的目录如果不存在，则创建
        File folder = new File("records/" + owner);
        if (!folder.exists() || !folder.isDirectory()) folder.mkdirs();
        //
        file = new File(String.format("records/%d/%d.dat", owner, target));
        FileOutputStream out;
        try {
            out = new FileOutputStream(file, true);
        } catch (IOException e) {
            out = null;
        }
        this.out = out;
    }

    /**
     * 从消息记录文件中读取全部条目
     * @return 消息记录条目数组
     */
    public Entry[] readAll() {
        // 打开文件输入流
        FileInputStream in;
        try {
            in = new FileInputStream(file);
        } catch (IOException e) {
            return new Entry[0];
        }
        Entry i;
        ArrayList<Entry> res = new ArrayList<>();
        // 循环读取消息记录条目，直到EOF
        while (true) {
            try {
                i = Entry.readFromStream(in);
            } catch (Exception e) { // EOFException
                break;
            }
            res.add(i);
        }
        // 关闭文件输入流
        try {
            in.close();
        } catch (IOException ignored) {}
        // 返回数组结果
        return res.toArray(new Entry[0]);
    }

    /**
     * 向已经打开的消息记录文件中追加内容
     * @param e 欲追加的条目
     */
    private void append(Entry e) {
        try {
            e.writeTo(out);
            out.flush();
        } catch (IOException ignored) {}
    }

    /**
     * 向已经打开的消息记录文件中追加内容
     * @param user 用户对象
     * @param time 消息时间
     * @param msgType 消息类型
     * @param msg 消息内容
     */
    public void append(User user, long time, int msgType, String msg) {
        append(new Entry(user, time, msgType, msg));
    }

    /**
     * 关闭消息记录文件抽象
     */
    public void close() {
        try {
            out.close();
        } catch (IOException ignored) {}
    }

    /**
     * 立即向消息记录文件中写入一个消息条目
     * @param target 私聊的目标用户id
     * @param e 消息条目
     */
    public static void appendEntry(int target, Entry e) {
        // 消息记录文件的所有者是当前登陆用户
        int owner = Main.userId;
        // 存放消息记录文件的目录如果不存在，则创建
        File folder = new File("records/" + owner);
        if (!folder.exists() || !folder.isDirectory()) folder.mkdir();
        // 打开消息记录文件输出流
        File file = new File(String.format("records/%d/%d.dat", owner, target));
        FileOutputStream out;
        // 写入条目并关闭文件
        try {
            out = new FileOutputStream(file, true);
            e.writeTo(out);
            out.close();
        } catch (IOException ignored) {}
    }
}
