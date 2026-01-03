package com.heartbeat.server.net;

import com.google.gson.Gson;
import com.heartbeat.server.model.Message;

import java.io.*;
import java.net.Socket;
public class ClientSession {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Gson gson =  new Gson();

    private String userId;
    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(Message message) throws IOException {
        out.println(gson.toJson(message));
    }
    public Message read() throws IOException {
        String json = in.readLine();
        if(json == null) return null;
        return gson.fromJson(json, Message.class);
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

}
