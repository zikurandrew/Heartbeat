package com.heartbeat.server;

import com.heartbeat.server.net.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {

    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("Server starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());

                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
// ЗАПУСК
//cd server
//mvn clean package
//java -jar target/server-1.0-SNAPSHOT.jar
// mvn exec:java -Dexec.mainClass="com.heartbeat.server.MainServer"

