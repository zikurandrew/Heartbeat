package com.heartbeat.server.net;

import com.google.gson.Gson;
import com.heartbeat.server.model.Message;

import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {

    private final Socket socket;
    private final PrintWriter out;
    private String userId;

    private final Gson gson = new Gson();

    public ClientSession(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void send(Message message) {
        out.println(gson.toJson(message));
    }

    public boolean isAlive() {
        return !socket.isClosed();
    }
}
