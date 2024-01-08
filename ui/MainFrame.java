package ui;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import client.User;

// 主窗口
public class MainFrame extends JFrame {
    private JPanel contentPane;
    private JList<ListItem> sessionList;
    private JLabel idLabel;
    private JLabel nameLabel;
    private JTextPane groupMsgPane;
    private JTextArea msgArea;
    private JButton sendBtn;
    private JButton picBtn;
    private JButton fileBtn;
    private JScrollPane groupMsgScrollPane;

    private static class ListItem {
        final User user;
        int msgCnt;

        public ListItem(User user) {
            this.user = user;
            msgCnt = 0;
        }

        public String toString() {
            if (msgCnt != 0) return String.format("%d条新消息 - %s (%d)", msgCnt, user.name, user.id);
            else return String.format("%s (%d)", user.name, user.id);
        }
    }

    private static String timeStamp2Date(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timeStamp * 1000));
    }

    private final Hashtable<Integer, ListItem> listItems = new Hashtable<>();

    private void updateList() {
        sessionList.setListData(listItems.values().toArray(new ListItem[0]));
    }

    private void scrollToBottom() {
        JScrollBar sb = groupMsgScrollPane.getVerticalScrollBar();
        sb.setValue(sb.getMaximum());
    }

    private void displayGroupMsg(int userId, long time, int msgType, String msg) {
        // String userName = listItems.get(userId).user.name;
        String userName = "野兽先辈";
        String color = userId == me.id ? "green" : "blue";
        HTMLDocument doc = (HTMLDocument)groupMsgPane.getStyledDocument();
        try {
            String html = String.format("<span style=\"display: block; color: %s;\">%s (%d) %s</span>", color, userName, userId, timeStamp2Date(time));
            // doc.insertString(doc.getLength(), html, null);
            doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), html);
            doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<br>");
            html = String.format("<span>%s</span>\n", msg);
            // doc.insertString(doc.getLength(), html, null);
            doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), html);
            doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<br>");
            scrollToBottom();
        } catch (Exception ignored) {}
    }

    private final User me;

    public MainFrame(User me) {
        super("Java Chat");
        setContentPane(contentPane);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.me = me;
        idLabel.setText(String.valueOf(me.id));
        nameLabel.setText(me.name);

        groupMsgPane.setContentType("text/html");
        groupMsgPane.setEditable(false);

        sendBtn.addActionListener(actionEvent -> {
            String msg = msgArea.getText();
            if (msg.isEmpty()) return;
            displayGroupMsg(1, 1704734351, 0, msg);
            msgArea.setText("");
        });
    }

    void setUserList(User[] users) {
        for (User user : users) {
            if (user.id == me.id) continue;
            listItems.put(user.id, new ListItem(user));
        }
        updateList();
    }

    void userOnline(User user) {
        listItems.put(user.id, new ListItem(user));
        updateList();
    }

    void userOffline(int userId) {
        listItems.remove(userId);
        updateList();
    }

    public void open() {
        pack();
        setVisible(true);
    }
}
