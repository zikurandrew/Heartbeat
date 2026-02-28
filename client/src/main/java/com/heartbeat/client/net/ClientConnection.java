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

    private static String currentUsername;

    public static void connect() throws IOException {
        socket = new Socket("localhost", 5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static void register(String username, String password) {
        send(new Message(MessageType.REGISTER, username, password));
    }

    public static void login(String username, String password) {
        currentUsername = username;
        send(new Message(MessageType.LOGIN, username, password));
    }

    public static Message waitResponse() throws Exception {
        String line = in.readLine();
        if (line != null) {
            return new Gson().fromJson(line, Message.class);
        }
        return null;
    }

    public static void send(Message message) {
        if (out != null) {
            out.println(gson.toJson(message));
        }
    }

    public static String getUsername() {
        return currentUsername;
    }

    public static BufferedReader getIn() {
        return in;
    }

    public static void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}