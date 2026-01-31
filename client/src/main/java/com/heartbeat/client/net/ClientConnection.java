package com.heartbeat.client.net;

import com.google.gson.Gson;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;

import java.io.*;
import java.net.Socket;

public class ClientConnection {

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static final Gson gson = new Gson();

    public static void connect() throws Exception {
        socket = new Socket("localhost", 5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );
    }

    public static void login(String username) {
        send(new Message(
                MessageType.LOGIN,
                username,
                ""
        ));
    }

    public static void pair() {
        send(new Message(
                MessageType.PAIR,
                null,
                ""
        ));
    }

    public static void send(Message message) {
        out.println(gson.toJson(message));
    }

    public static BufferedReader getIn() {
        return in;
    }
}
