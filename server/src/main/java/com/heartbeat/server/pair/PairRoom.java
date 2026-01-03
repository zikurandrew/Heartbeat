package com.heartbeat.server.pair;

import com.heartbeat.server.model.Message;
import com.heartbeat.server.net.ClientSession;

public class PairRoom {

    private final ClientSession a;
    private final ClientSession b;

    public PairRoom(ClientSession a, ClientSession b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Повертає сесію іншого користувача
     */
    public ClientSession getOther(ClientSession me) {
        if (me == a) return b;
        if (me == b) return a;
        return null;
    }

    /**
     * Повертає userId іншого користувача
     */
    public String getOtherUser(ClientSession me) {
        ClientSession other = getOther(me);
        return other != null ? other.getUserId() : null;
    }

    /**
     * Пересилає повідомлення іншому
     */
    public void relay(ClientSession from, Message message) {
        ClientSession to = getOther(from);
        if (to != null && to.isAlive()) {
            to.send(message);
        }
    }

    /**
     * Чи обидва ще підключені
     */
    public boolean isActive() {
        return a.isAlive() && b.isAlive();
    }
}
