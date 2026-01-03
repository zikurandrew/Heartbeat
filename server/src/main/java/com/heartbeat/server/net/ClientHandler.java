package com.heartbeat.server.net;

import com.heartbeat.server.model.Message;
import com.heartbeat.server.model.MessageType;
import com.heartbeat.server.pair.PairRoom;
import com.heartbeat.server.pair.PairManager;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final ClientSession session;
    private PairRoom room;

    public ClientHandler(Socket socket) throws IOException {
        this.session = new ClientSession(socket);
    }

    @Override
    public void run() {
        try {
            Message login = session.read();
            session.setUserId(login.getSender());

            room = PairManager.join(session);
            if(room == null) {
                session.send(new Message(
                        MessageType.SYSTEM,
                        "server",
                        "WAITING_FOR_PAIR"
                ));
            } else {
                session.send(new Message(
                        MessageType.SYSTEM,
                        "server",
                        "PAIRED"
                ));
            }
            Message message;
            while ((message = session.read()) != null) {
                if(room != null && message.getType() == MessageType.CHAT) {
                    room.relay(session, message);
                }
            }
        } catch (Exception e) {
            System.out.println("Client left");
        }
    }
}
