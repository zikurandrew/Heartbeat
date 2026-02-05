package com.heartbeat.server.db;

import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageDAO {

    // =========================
    // INSERT SQL
    // =========================
    private static final String INSERT_SQL = """
        INSERT INTO messages (sender, receiver, type, content, timestamp, room_id)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

    // =========================
    // HISTORY SQL BY USERS
    // =========================
    private static final String HISTORY_SQL = """
        SELECT sender, receiver, type, content, timestamp, room_id
        FROM messages
        WHERE (sender = ? AND receiver = ?)
           OR (sender = ? AND receiver = ?)
        ORDER BY timestamp
    """;

    // =========================
    // HISTORY SQL BY ROOM
    // =========================
    private static final String HISTORY_BY_ROOM_SQL = """
        SELECT sender, receiver, type, content, timestamp, room_id
        FROM messages
        WHERE room_id = ?
        ORDER BY timestamp
    """;

    // =========================
    // HISTORY SQL BY ROOM (LIMIT N)
    // =========================
    private static final String LAST_MESSAGES_BY_ROOM_SQL = """
        SELECT sender, receiver, type, content, timestamp, room_id
        FROM messages
        WHERE room_id = ?
        ORDER BY timestamp DESC
        LIMIT ?
    """;

    // =========================
    // SAVE MESSAGE
    // =========================
    public static void save(Message message) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

            ps.setString(1, message.getSender());
            ps.setString(2, message.getReceiver());
            ps.setString(3, message.getType().name());
            ps.setString(4, message.getContent());
            ps.setLong(5, message.getTimestamp());
            ps.setString(6, message.getRoomId());

            ps.executeUpdate();
        }
    }

    // =========================
    // LOAD CHAT HISTORY BY USERS
    // =========================
    public static List<Message> loadHistory(String userA, String userB) throws SQLException {
        List<Message> history = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(HISTORY_SQL)) {

            ps.setString(1, userA);
            ps.setString(2, userB);
            ps.setString(3, userB);
            ps.setString(4, userA);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(map(rs));
            }
        }

        return history;
    }

    // =========================
    // LOAD CHAT HISTORY BY ROOM
    // =========================
    public static List<Message> loadHistoryByRoom(String roomId) throws SQLException {
        List<Message> history = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(HISTORY_BY_ROOM_SQL)) {

            ps.setString(1, roomId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(map(rs));
            }
        }

        return history;
    }

    // =========================
    // LOAD LAST N MESSAGES BY ROOM
    // =========================
    public static List<Message> loadLastMessagesByRoom(String roomId, int limit) throws SQLException {
        List<Message> history = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(LAST_MESSAGES_BY_ROOM_SQL)) {

            ps.setString(1, roomId);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(map(rs));
            }
        }

        history.sort(Comparator.comparingLong(Message::getTimestamp));

        return history;
    }


    // =========================
    // MAP ROW → MESSAGE
    // =========================
    private static Message map(ResultSet rs) throws SQLException {
        Message msg = new Message(
                MessageType.valueOf(rs.getString("type")),
                rs.getString("sender"),
                rs.getString("content")
        );

        msg.setReceiver(rs.getString("receiver"));
        msg.setTimestamp(rs.getLong("timestamp"));
        msg.setRoomId(rs.getString("room_id"));

        return msg;
    }
}
