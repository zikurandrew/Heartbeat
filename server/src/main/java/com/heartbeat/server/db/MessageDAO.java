package com.heartbeat.server.db;

import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public static void save(String sender, String receiver, Message msg) throws Exception {

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO messages(sender, receiver, type, content, timestamp) VALUES (?, ?, ?, ?, ?)")) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, msg.getType().name());
            ps.setString(4, msg.getContent());
            ps.setLong(5, msg.getTimestamp());

            ps.executeUpdate();
        }
    }

    public static List<Message> loadHistory(String a, String b) throws Exception {

        List<Message> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     """
                     SELECT * FROM messages
                     WHERE (sender = ? AND receiver = ?)
                        OR (sender = ? AND receiver = ?)
                     ORDER BY timestamp
                     """
             )) {

            ps.setString(1, a);
            ps.setString(2, b);
            ps.setString(3, b);
            ps.setString(4, a);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Message(
                        MessageType.valueOf(rs.getString("type")),
                        rs.getString("sender"),
                        rs.getString("content")
                ));
            }
        }

        return list;
    }
}
