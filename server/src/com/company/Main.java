package com.company;

import com.company.utils.Log;
import com.company.utils.PortCheck;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

    public static final int DEFAULT_PORT = 8080;

    private static int port;

    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        Log.i("server is running...");
        preparePort(args);
        prepareSocketServer();
    }

    private static void prepareSocketServer() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            Log.i("server is waiting for client...");
            while (true) {
                Socket socket = serverSocket.accept();
                Log.i("client accept:" + socket.getInetAddress().getHostAddress());
                new MsgDispatcher(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void preparePort(String[] args) {
        Log.i("preparing port...");
        if (args == null || args.length == 0) {
            port = DEFAULT_PORT;
        } else {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                port = DEFAULT_PORT;
            }
        }
        while (!PortCheck.isPortAvailable(port) && port < 65535) {
            port++;
        }
        if (port == 65535 && !PortCheck.isPortAvailable(port)) {
            throw new IllegalArgumentException(port + " port is not available");
        }
        try {
            InetAddress ia = InetAddress.getLocalHost();
            Log.i("IP: " + ia.getHostAddress() + " /Port is " + port);
        } catch (UnknownHostException e) {
            Log.i("IP: 127.0.0.1 /PORT: " + port);
        }
    }
}
