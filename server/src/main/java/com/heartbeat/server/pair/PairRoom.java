package com.heartbeat.server.pair;

import com.heartbeat.common.model.Message;
import com.heartbeat.server.net.ClientSession;

import java.util.UUID;

public class PairRoom {

    private final ClientSession a;
    private final ClientSession b;
    private final String id;

    public PairRoom(ClientSession a, ClientSession b) {
        this.a = a;
        this.b = b;
        this.id = UUID.randomUUID().toString();
    }

    public String getOtherUser(ClientSession me) {
        return me == a ? b.getUserId() : a.getUserId();
    }

    // Відправляє повідомлення обом користувачам
    public void broadcast(Message message) {
        message.setRoomId(id);
        a.send(message);
        b.send(message);
    }

    public boolean contains(ClientSession session) {
        return session == a || session == b;
    }

    public ClientSession getA() {
        return a;
    }

    public ClientSession getB() {
        return b;
    }

    public String getId() {
        return id;
    }
}
