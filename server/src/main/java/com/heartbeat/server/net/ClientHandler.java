package com.heartbeat.server.net;

import com.google.gson.Gson;
import com.heartbeat.server.db.MessageDAO;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import com.heartbeat.server.pair.PairManager;
import com.heartbeat.server.pair.PairRoom;
import com.heartbeat.server.service.AuthService;
import com.heartbeat.server.service.ServiceException;
import com.heartbeat.common.model.User;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();
    private final AuthService authService = new AuthService();

    private ClientSession session;

    private PairRoom room;

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            session = new ClientSession(socket, out);
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("SERVER RECEIVED: " + line);
                Message message = gson.fromJson(line, Message.class);
                if (message != null && message.getTimestamp() == 0) {
                    message.setTimestamp(System.currentTimeMillis());
                }

                switch (message.getType()) {

                    case REGISTER -> {
                        try {
                            User user = authService.register(message.getSender(), message.getContent());
                            session.send(new Message(MessageType.SYSTEM, "server", "REGISTER_OK"));
                        } catch (ServiceException e) {
                            session.send(new Message(MessageType.SYSTEM, "server", "REGISTER_FAIL: " + e.getMessage()));
                        }
                    }

                    case LOGIN -> {
                        try {
                            User user = authService.login(message.getSender(), message.getContent());
                            session.setUserId(user.getUsername());
                            session.send(new Message(MessageType.SYSTEM, "server", "LOGIN_OK"));
                        } catch (ServiceException e) {
                            session.send(new Message(MessageType.SYSTEM, "server", "LOGIN_FAIL: " + e.getMessage()));
                        }
                    }

                    case PAIR -> {
                        room = PairManager.join(session);
                        if (room != null) {
                            Message paired = new Message(MessageType.SYSTEM, "server", "PAIRED");
                            paired.setRoomId(room.getId());
                            room.broadcast(paired);

                            List<Message> history = MessageDAO.loadLastMessagesByRoom(room.getId(), 100);

                            for (Message msg : history) {
                                msg.setType(MessageType.HISTORY); // щоб клієнт обробив як історію
                                room.getA().send(msg);
                                room.getB().send(msg);
                            }
                        }
                    }

                    case CHAT, EMOJI, MOOD, TYPING -> {
                        PairRoom room = PairManager.getRoom(session);
                        if (room == null) {
                            System.out.println("CHAT ignored, no room for " + session.getUserId());
                            continue;
                        }

                        Message fixed = new Message(
                                message.getType(),
                                session.getUserId(),
                                message.getContent()
                        );

                        fixed.setReceiver(room.getOtherUser(session));
                        fixed.setRoomId(room.getId());

                        if (message.getType() == MessageType.CHAT) {
                            MessageDAO.save(fixed);
                        }

                        ClientSession partnerSession = (room.getA() == session) ? room.getB() : room.getA();
                        if (partnerSession != null) {
                            partnerSession.send(fixed);
                        }
                    }


                    case UNPAIR -> {
                        if (PairManager.getRoom(session) != null) {
                            PairManager.leave(session);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            PairManager.leave(session);
            if (session != null) session.close();
        }
    }
}
