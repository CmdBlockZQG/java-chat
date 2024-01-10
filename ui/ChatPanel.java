package ui;

import client.TCRequest;
import client.User;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

// 封装的聊天面板
public class ChatPanel {
    private static final int MSG_TEXT = 0; // 文本消息类型
    private static final int MSG_PIC = 1; // 图片消息类型
    private static final int MSG_FILE = 2; // 文件消息类型
    public static int TARGET_GROUP = -1; // 表示目标为群聊

    /**
     * 工具函数：将时间戳转换为字符串
     *
     * @param timeStamp 时间戳（s）
     * @return 结果字符串
     */
    private static String timeStamp2Date(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timeStamp * 1000));
    }

    private JPanel contentPane; // 主容器
    private JButton picBth; // 发送图片按钮
    private JButton fileBtn; // 发送文件按钮
    private JButton sendBtn; // 发送按钮
    private JTextPane msgPane; // 消息框
    private JTextArea msgArea; // 消息编辑框

    final JFrame parent; // 所在的窗口
    private final int target; // 私聊对象的用户id，TARGET_GROUP表示群聊
    final MsgRecord record; // 消息记录文件

    /**
     * 构造聊天面板
     */
    public ChatPanel(JFrame parent, int target) {
        this.parent = parent;
        this.target = target;

        // 设置消息框为html格式，不可编辑
        msgPane.setContentType("text/html");
        msgPane.setEditable(false);

        // 加载消息记录
        if (target == TARGET_GROUP) {
            record = Main.groupRecord; // 群聊消息记录对象是常驻的
        } else {
            record = new MsgRecord(target); // 加载私聊消息记录对象
        }
        MsgRecord.Entry[] msgList = record.readAll(); // 读取全部消息记录
        for (MsgRecord.Entry msg : msgList) { // 添加进消息框
            displayMsg(msg.userId, msg.userName, msg.time, msg.msgType, msg.msg);
        }

        // 点击聊天框中的链接事件处理
        msgPane.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                String url = String.valueOf(e.getURL()).substring(7); // 去掉"http://"的协议头
                String md5 = url.substring(0, 32); // md5串
                String file = url.substring(32); // 文件名

                // 展示保存文件对话框
                FileDialog fileDialog = new FileDialog(parent, "保存文件", FileDialog.SAVE);
                fileDialog.setFile(file);
                fileDialog.setVisible(true);
                // 获取保存路径
                String dir = fileDialog.getDirectory();
                file = fileDialog.getFile();
                if (dir == null || file == null) { // 路径有问题
                    new MessageDialog("请选择正确的保存位置！", () -> {
                    });
                    return;
                }
                try {
                    TCRequest.download(md5, dir + file); // 从服务端下载文件
                } catch (IOException err) {
                    new MessageDialog("文件下载失败：" + err, () -> {
                    });
                    return;
                }
                new MessageDialog("文件下载完成", () -> {
                });
            }
        });

        // 发送按钮点击事件处理
        sendBtn.addActionListener(e -> {
            // 发送文本消息并清空消息编辑框
            sendMsg(MSG_TEXT, msgArea.getText());
            msgArea.setText("");
        });

        // 发送图片按钮点击事件
        picBth.addActionListener(e -> {
            // 展示打开文件对话框
            FileDialog fileDialog = new FileDialog(parent, "选择图片", FileDialog.LOAD);
            fileDialog.setVisible(true);
            // 获取图片路径
            String dir = fileDialog.getDirectory();
            String file = fileDialog.getFile();
            if (dir == null || file == null) { // 路径有问题
                new MessageDialog("请选择正确的图片文件！", () -> {
                });
                return;
            }
            String md5;
            try {
                md5 = TCRequest.upload(dir + file); // 上传图片并获取图片id
            } catch (IOException err) {
                new MessageDialog("上传失败：" + err, () -> {
                });
                return;
            }
            sendMsg(MSG_PIC, md5); // 发送图片消息
        });

        // 发送文件按钮点击事件
        fileBtn.addActionListener(e -> {
            // 展示打开文件对话框
            FileDialog fileDialog = new FileDialog(parent, "选择文件", FileDialog.LOAD);
            fileDialog.setVisible(true);
            // 获取文件路径
            String dir = fileDialog.getDirectory();
            String file = fileDialog.getFile();
            if (dir == null || file == null) { // 路径有问题
                new MessageDialog("请选择正确的文件！", () -> {
                });
                return;
            }
            String md5;
            try {
                md5 = TCRequest.upload(dir + file); // 上传文件并获取文件id
            } catch (IOException err) {
                new MessageDialog("上传失败：" + err, () -> {
                });
                return;
            }
            sendMsg(MSG_FILE, md5 + file); // 发送文件消息
        });
    }

    /**
     * 发送消息
     *
     * @param msgType 消息类型
     * @param msg     消息内容
     */
    private void sendMsg(int msgType, String msg) {
        if (msg.isEmpty()) return; // 忽略空消息
        if (target == TARGET_GROUP) { // 发送群消息
            try {
                Main.client.sendGroupMsg(msgType, msg);
            } catch (IOException ignored) {
            }
        } else { // 发送私聊消息
            try {
                Main.client.sendDmMsg(target, msgType, msg);
                if (target != Main.userId) { // 如果不是发送给自己的消息，则自己发送的消息需要处理
                    long time = System.currentTimeMillis() / 1000;
                    displayMsg(Main.user, time, msgType, msg); // 展示消息
                    record.append(Main.user, time, msgType, msg); // 存储到消息记录
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 获取面板容器对象
     *
     * @return 面板容器对象
     */
    public JPanel getPanel() {
        return contentPane;
    }

    /**
     * 将聊天框滚动到底部
     */
    private void scrollToBottom() {
        msgPane.setCaretPosition(msgPane.getStyledDocument().getLength());
    }

    /**
     * 在聊天框中展示新消息
     *
     * @param userId   发送者id
     * @param userName 发送者用户名
     * @param time     发送时间时间戳（s）
     * @param msgType  消息类型
     * @param msg      消息内容字符串
     */
    public void displayMsg(int userId, String userName, long time, int msgType, String msg) {
        String color = userId == Main.userId ? "green" : "blue";

        if (msgType == MSG_PIC) { // 图片消息需要先下载图
            try {
                // 创建图片缓存目录
                File folder = new File("img");
                if (!folder.exists() || !folder.isDirectory()) folder.mkdir();
                // 下载图片
                TCRequest.download(msg, "img/" + msg);
            } catch (IOException e) {
                // 图片下载失败，把图片消息转换为文本消息显示错误信息
                msgType = MSG_TEXT;
                msg = "图片加载失败：" + e;
            }
        }

        // 向TextPane中加入html标签
        HTMLDocument doc = (HTMLDocument) msgPane.getStyledDocument();
        Consumer<String> appendTag = (html) -> {
            try {
                doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), html);
                doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<br>");
            } catch (Exception ignored) {
            }
        };

        // 消息头部，展示发送者和发送时间
        String header = String.format(
                "<span style=\"color: %s;\">%s (%d) %s</span>",
                color, userName, userId, timeStamp2Date(time));
        appendTag.accept(header);

        // 消息内容
        String content = switch (msgType) {
            case MSG_TEXT -> String.format("<span>%s</span>", msg); // 文本消息直接span包一下
            case MSG_PIC -> String.format("<img src=\"file:img/%s\" />", msg); // 图片消息插入img标签
            case MSG_FILE -> String.format("<span>发送文件：</span><a href=\"http://%s\">%s</a>",
                    msg, msg.substring(32)); // 文件消息插入文件下载超链接
            default -> "";
        };
        appendTag.accept(content); // 加入刚才生成的html tag
        scrollToBottom(); // 滚动到聊天框底部
    }

    /**
     * 在聊天框中展示新消息
     *
     * @param user    发送者对象
     * @param time    发送时间时间戳（s）
     * @param msgType 消息类型
     * @param msg     消息内容字符串
     */
    public void displayMsg(User user, long time, int msgType, String msg) {
        displayMsg(user.id, user.name, time, msgType, msg);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 300), null, 0, false));
        msgPane = new JTextPane();
        scrollPane1.setViewportView(msgPane);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        contentPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        picBth = new JButton();
        picBth.setText("图片");
        panel1.add(picBth);
        fileBtn = new JButton();
        fileBtn.setText("文件");
        panel1.add(fileBtn);
        final JScrollPane scrollPane2 = new JScrollPane();
        contentPane.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(400, 100), null, 0, false));
        msgArea = new JTextArea();
        scrollPane2.setViewportView(msgArea);
        sendBtn = new JButton();
        sendBtn.setText("发送");
        contentPane.add(sendBtn, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
