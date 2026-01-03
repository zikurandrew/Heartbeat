package com.heartbeat.server.pair;

import com.heartbeat.server.model.Message;
import com.heartbeat.server.net.ClientSession;

import java.io.IOException;

public class PairRoom {
    private final ClientSession a;
    private final ClientSession b;

    public PairRoom(ClientSession a, ClientSession b) {
        this.a = a;
        this.b = b;
    }
    public void relay(ClientSession from, Message message) throws IOException {
        if (from == a) {
            b.send(message);
        } else if (from == b) {
            a.send(message);
        }
    }
}
