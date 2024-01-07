package ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// 信息提示框
public class MessageDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK; // 确认按钮
    private JLabel msgLabel; // 消息内容标签

    /**
     * 构造并显示提示框
     * @param msg 要显示的消息
     * @param onOk 按下OK后进行的事件
     */
    public MessageDialog(String msg, Runnable onOk) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        msgLabel.setText(msg);
        buttonOK.addActionListener(e -> {
            onOk.run();
            dispose(); // OK后销毁对话框
        });
        pack();
        setVisible(true);
    }
}
