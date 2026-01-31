package com.heartbeat.server.pair;

import com.heartbeat.common.model.Message;
import com.heartbeat.server.net.ClientSession;

public class PairRoom {

    private final ClientSession a;
    private final ClientSession b;

    public PairRoom(ClientSession a, ClientSession b) {
        this.a = a;
        this.b = b;
    }

    public String getOtherUser(ClientSession me) {
        return me == a ? b.getUserId() : a.getUserId();
    }

    public void broadcast(Message message) {
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
}
