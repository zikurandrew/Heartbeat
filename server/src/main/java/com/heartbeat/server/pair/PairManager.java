package com.heartbeat.server.pair;

import com.heartbeat.server.net.ClientSession;
import com.heartbeat.server.pair.PairRoom;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PairManager {

    private static final Queue<ClientSession> waiting =
            new ConcurrentLinkedQueue<>();

    /**
     * Додає користувача в чергу або створює пару
     */
    public static synchronized PairRoom join(ClientSession session) {

        ClientSession other = waiting.poll();

        if (other == null || !other.isAlive()) {
            waiting.add(session);
            return null;
        }

        return new PairRoom(session, other);
    }

    /**
     * Видаляє користувача з черги (якщо він вийшов)
     */
    public static synchronized void leave(ClientSession session) {
        waiting.remove(session);
    }
}
