package com.heartbeat.server.net;

import com.google.gson.Gson;
import com.heartbeat.common.model.Message;

import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {

    private final Socket socket;
    private final PrintWriter out;
    private final Gson gson = new Gson();

    private String userId;

    public ClientSession(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
    }

    public void send(Message message) {
        out.println(gson.toJson(message));
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
