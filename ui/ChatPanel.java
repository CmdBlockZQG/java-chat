package ui;

import client.User;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

// 封装的聊天面板
public class ChatPanel {
    private static final int MSG_TEXT = 0; // 文本消息类型
    private static final int MSG_PIC = 1; // 图片消息类型
    private static final int MSG_FILE = 2; // 文件消息类型

    /**
     * 工具函数：将时间戳转换为字符串
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
    private JScrollPane msgScrollPane; // 消息框滚动容器
    private JTextPane msgPane; // 消息框
    private JTextArea msgArea; // 消息编辑框

    /**
     * 构造聊天面板
     */
    public ChatPanel() {
        // 设置消息框为html格式，不可编辑
        msgPane.setContentType("text/html");
        msgPane.setEditable(false);

        // 点击聊天框中的链接事件处理
        msgPane.addHyperlinkListener((HyperlinkListener) -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(HyperlinkListener.getEventType())) {
                System.out.println(HyperlinkListener.getURL());
            }
        });

        // 发送按钮点击事件处理
        sendBtn.addActionListener(actionEvent -> {
            String msg = msgArea.getText();
            if (msg.isEmpty()) return;
            // TODO: 发送消息
            msgArea.setText("");
        });
    }

    /**
     * 获取面板容器对象
     * @return 面板容器对象
     */
    public JPanel getPanel() {
        return contentPane;
    }

    /**
     * 将聊天框滚动到底部
     */
    private void scrollToBottom() {
        JScrollBar sb = msgScrollPane.getVerticalScrollBar();
        sb.setValue(sb.getMaximum());
    }

    /**
     * 在聊天框中展示新消息
     * @param user 发送者对象
     * @param time 发送时间时间戳（s）
     * @param msgType 消息类型
     * @param msg 消息内容字符串
     */
    public void displayGroupMsg(User user, long time, int msgType, String msg) {
        int userId = user.id;
        String userName = user.name;
        String color = user.id == Main.userId ? "green" : "blue";

        HTMLDocument doc = (HTMLDocument)msgPane.getStyledDocument();
        Consumer<String> appendTag = (html) -> {
            try {
                doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), html);
                doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<br>");
            } catch (Exception ignored) {}
        };

        String header = String.format(
                "<span style=\"color: %s;\">%s (%d) %s</span>",
                color, userName, userId, timeStamp2Date(time));
        appendTag.accept(header);

        String content = switch (msgType) {
            case MSG_TEXT -> String.format("<span>%s</span>", msg);
            case MSG_PIC -> String.format("<img src=\"file:img/%s\" />", msg);
            case MSG_FILE -> String.format("<span>发送文件：</span><a href=\"http://%s\">%s</a>",
                    msg, msg.substring(32));
            default -> "";
        };
        appendTag.accept(content);
        scrollToBottom();
    }

}
