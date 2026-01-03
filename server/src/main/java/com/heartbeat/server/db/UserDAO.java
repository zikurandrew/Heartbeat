package com.heartbeat.server.db;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public static boolean register(String username, String password) {

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static boolean login(String username, String password) {

        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return false;

            String hash = rs.getString("password");
            return BCrypt.checkpw(password, hash);

        } catch (Exception e) {
            return false;
        }
    }
}
