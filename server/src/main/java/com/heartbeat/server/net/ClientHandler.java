package com.heartbeat.server.net;

import com.google.gson.Gson;
import com.heartbeat.server.db.MessageDAO;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import com.heartbeat.server.pair.PairManager;
import com.heartbeat.server.pair.PairRoom;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();

    private ClientSession session;
    private PairRoom room;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true)
        ) {

            session = new ClientSession(socket, out);

            String line;
            while ((line = in.readLine()) != null) {

                Message message = gson.fromJson(line, Message.class);
                if (message == null || message.getType() == null) continue;

                switch (message.getType()) {

                    case LOGIN -> {
                        session.setUserId(message.getSender());
                        session.send(new Message(
                                MessageType.SYSTEM,
                                "server",
                                "LOGIN_OK"
                        ));
                    }

                    case PAIR -> {
                        room = PairManager.join(session);

                        if (room != null) {
                            Message paired = new Message(
                                    MessageType.SYSTEM,
                                    "server",
                                    "PAIRED"
                            );

                            room.broadcast(paired);

                            String userA = room.getA().getUserId();
                            String userB = room.getB().getUserId();

                            List<Message> history =
                                    MessageDAO.loadHistory(userA, userB);

                            for (Message msg : history) {
                                room.broadcast(msg);
                            }
                        }
                    }

                    case CHAT, EMOJI -> {

                        if (room == null) continue;

                        String from = session.getUserId();
                        String to = room.getOtherUser(session);

                        Message fixed = new Message(
                                message.getType(),
                                from,
                                message.getContent()
                        );

                        MessageDAO.save(from, to, fixed);
                        room.broadcast(fixed);
                    }

                    case UNPAIR -> {
                        PairManager.leave(session);
                        room = null;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected");
        } finally {
            PairManager.leave(session);
            if (session != null) session.close();
        }
    }
}
