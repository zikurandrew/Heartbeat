package com.heartbeat.server.pair;

import com.heartbeat.server.net.ClientSession;

import java.util.LinkedList;
import java.util.Queue;

public class PairManager {

    private static final Queue<ClientSession> waiting = new LinkedList<>();

    public static synchronized PairRoom join(ClientSession session) {

        if (waiting.contains(session)) {
            return null;
        }

        if (waiting.isEmpty()) {
            waiting.add(session);
            return null;
        }

        ClientSession other = waiting.poll();
        return new PairRoom(other, session);
    }

    public static synchronized void leave(ClientSession session) {
        waiting.remove(session);
    }
}
