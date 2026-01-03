package com.heartbeat.server.pair;

import com.heartbeat.server.net.ClientSession;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
public class PairManager {
    private static final Queue<ClientSession> waiting = new ConcurrentLinkedQueue<>();
    public static synchronized PairRoom join(ClientSession session) {
        ClientSession other = waiting.poll();

        if(other == null) {
            System.out.println("WAITING: " + session.getUserId());
            waiting.add(session);
            return null;
        }

        System.out.println("PAIRED: " + other.getUserId() + " <-> " + session.getUserId());

        return new PairRoom(other, session);
    }
}
