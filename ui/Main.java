package ui;

import client.Client;
import client.User;

import java.io.IOException;

// ui客户端入口类
public class Main {
    private static LoginFrame loginFrame; // 登陆窗口对象
    static MainFrame mainFrame; // 主窗口对象
    static Client client = null; // 客户端对象
    static int userId; // 用户id
    static String userName; // 用户名

    /**
     * 客户端ui运行入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 添加程序结束钩子，退出前通知服务器客户端下线
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client == null) return;
            try {
                // 关闭客户端
                client.close();
            } catch (IOException ignored) {}
        }));
        // 显示登陆窗口
        loginFrame = new LoginFrame();
    }

    /**
     * 登陆
     * @param id 用户ud
     * @param name 用户名
     */
    public static void login(int id, String name) {
        userId = id;
        userName = name;
        try {
            // 登陆并注册事件监听器
            client = new Client(id, name);
            mainFrame = new MainFrame(); // 先预载主窗口
            client.registerListener(new EventHandler(client));
        } catch (IOException e) {
            new MessageDialog("网络错误，登陆失败！", () -> System.exit(0));
        }
        // 销毁登陆窗口
        loginFrame.dispose();
        // 打开主窗口
        mainFrame.open();
    }
}
