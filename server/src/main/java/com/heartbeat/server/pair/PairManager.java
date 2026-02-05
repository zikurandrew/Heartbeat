package com.heartbeat.server.pair;

import com.heartbeat.server.net.ClientSession;

import java.util.*;

public class PairManager {

    private static final Queue<ClientSession> waiting = new LinkedList<>();
    private static final Map<ClientSession, PairRoom> activeRooms = new HashMap<>();

    public static synchronized PairRoom join(ClientSession session) {
        if (waiting.contains(session) || activeRooms.containsKey(session)) {
            return null;
        }

        if (waiting.isEmpty()) {
            waiting.add(session);
            return null;
        }

        ClientSession other = waiting.poll();

        PairRoom room = new PairRoom(other, session);

        activeRooms.put(other, room);
        activeRooms.put(session, room);

        return room;
    }

    public static synchronized void leave(ClientSession session) {
        waiting.remove(session);

        PairRoom room = activeRooms.remove(session);
        if (room != null) {
            ClientSession other = room.getOtherUser(session).equals(room.getA().getUserId()) ? room.getA() : room.getB();
            activeRooms.remove(other);
        }
    }

    public static synchronized PairRoom getRoom(ClientSession session) {
        return activeRooms.get(session);
    }
}
