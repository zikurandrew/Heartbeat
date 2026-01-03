package com.heartbeat.server.db;

import com.heartbeat.server.model.Message;
import com.heartbeat.server.model.MessageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    /**
     * Зберігає повідомлення в БД
     */
    public static void save(String sender, String receiver, Message message) {

        String sql = """
            INSERT INTO messages(sender, receiver, type, content)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, message.getType().name());
            ps.setString(4, message.getContent());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Завантажує історію між двома користувачами
     */
    public static List<Message> loadHistory(String userA, String userB) {

        List<Message> history = new ArrayList<>();

        String sql = """
            SELECT sender, type, content
            FROM messages
            WHERE (sender = ? AND receiver = ?)
               OR (sender = ? AND receiver = ?)
            ORDER BY timestamp ASC
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userA);
            ps.setString(2, userB);
            ps.setString(3, userB);
            ps.setString(4, userA);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message msg = new Message(
                        MessageType.valueOf(rs.getString("type")),
                        rs.getString("sender"),
                        rs.getString("content")
                );
                history.add(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return history;
    }
}
