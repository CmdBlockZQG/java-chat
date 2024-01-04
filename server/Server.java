package server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Server {
    public static int PORT = 1145;
    private static ServerSocket server;
    private static ExecutorService threadPool;

    public static void main(String[] args) {
        try {
            server = new ServerSocket(PORT);
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(0);
        }

        threadPool = Executors.newFixedThreadPool(64);

        while (true) {
            Socket socket;
            try {
                socket = server.accept();
                socket.setKeepAlive(true);
            } catch (IOException e) {
                System.err.println(e);
                e.printStackTrace();
                continue;
            }

            try {
                SocketHandler handler = new SocketHandler(socket);
                threadPool.submit(handler);
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) { }
            }
        }
    }
}