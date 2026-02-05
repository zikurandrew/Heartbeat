package com.heartbeat.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RoomDAO {

    public int findRoom(int u1, int u2) throws SQLException {
        int user1 = Math.min(u1, u2);
        int user2 = Math.max(u1, u2);

        String sql = """
            SELECT id FROM rooms
            WHERE user1 = ? AND user2 = ?
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, user1);
            ps.setInt(2, user2);

            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    public int createRoom(int u1, int u2) throws SQLException {
        int user1 = Math.min(u1, u2);
        int user2 = Math.max(u1, u2);

        String sql = """
            INSERT INTO rooms (user1, user2)
            VALUES (?, ?)
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps =
                     c.prepareStatement(sql,
                             PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, user1);
            ps.setInt(2, user2);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }

            throw new SQLException("Room id not generated");
        }
    }
}
