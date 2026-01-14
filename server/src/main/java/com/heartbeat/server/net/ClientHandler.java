package com.heartbeat.server.net;

import com.google.gson.Gson;
import com.heartbeat.server.db.MessageDAO;
import com.heartbeat.server.pair.PairManager;
import com.heartbeat.server.pair.PairRoom;
import com.heartbeat.server.model.Message;
import com.heartbeat.server.model.MessageType;
import com.heartbeat.server.net.ClientSession;

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
                if (message == null || message.getType() == null) {
                    continue;
                }

                switch (message.getType()) {

                    // ===== LOGIN =====
                    case LOGIN -> {
                        session.setUserId(message.getSender());

                        session.send(new Message(
                                MessageType.SYSTEM,
                                "server",
                                "LOGIN_OK"
                        ));
                    }

                    // ===== PAIRING =====
                    case PAIR -> {

                        room = PairManager.join(session);

                        if (room != null) {

                            ClientSession a = room.getFirst();
                            ClientSession b = room.getSecond();

                            Message paired = new Message(
                                    MessageType.SYSTEM,
                                    "server",
                                    "PAIRED"
                            );

                            a.send(paired);
                            b.send(paired);

                            List<Message> historyA =
                                    MessageDAO.loadHistory(
                                            a.getUserId(),
                                            b.getUserId()
                                    );

                            for (Message msg : historyA) {
                                a.send(msg);
                            }

                            List<Message> historyB =
                                    MessageDAO.loadHistory(
                                            b.getUserId(),
                                            a.getUserId()
                                    );

                            for (Message msg : historyB) {
                                b.send(msg);
                            }
                        }
                    }


                    // ===== CHAT / EMOJI / SIGNALS =====
                    case CHAT, EMOJI, TYPING, MOOD, PULSE, ATTENTION -> {

                        if (room == null) continue;

                        String from = session.getUserId();
                        String to = room.getOtherUser(session);

                        message.setSender(from);

                        // зберігаємо тільки CHAT та EMOJI
                        if (message.getType() == MessageType.CHAT ||
                                message.getType() == MessageType.EMOJI) {

                            MessageDAO.save(
                                    from,
                                    to,
                                    message
                            );
                        }

                        room.relay(session, message);

                        session.send(message);
                    }


                    // ===== DISCONNECT =====
                    case UNPAIR -> {
                        PairManager.leave(session);
                        room = null;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected");
        }
    }
}
