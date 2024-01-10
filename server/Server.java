package server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import common.Config;

// 服务器主类
public class Server {
    private static ServerSocket server; // Socket服务器

    public static void main(String[] args) {
        try {
            server = new ServerSocket(Config.PORT); // 运行Socket服务
        } catch (IOException e) {
            System.err.println("端口" + Config.PORT + "已经被占用");
            System.exit(1);
        }
        System.out.println("服务端已启动 端口" + Config.PORT);

        // 线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(128); // 创建线程池

        while (true) { // 服务循环
            Socket socket;
            try {
                socket = server.accept(); // 有客户端连接
                socket.setKeepAlive(true); // 依靠TCP自动关闭死掉的链接
            } catch (IOException e) {
                System.err.println("服务器错误：" + e);
                break;
            }

            try {
                // 创建socket处理器，运行处理线程
                SocketHandler handler = new SocketHandler(socket);
                threadPool.submit(handler);
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignored) { }
            }
        }
    }
}