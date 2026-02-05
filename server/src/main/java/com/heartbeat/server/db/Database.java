package com.heartbeat.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class Database {

    private static final Logger log =
            LoggerFactory.getLogger(Database.class);

    private static final String URL = "jdbc:sqlite:heartbeat.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            init();
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBC driver not found", e);
            throw new RuntimeException(e);
        }
    }

    private static void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            user1 INTEGER NOT NULL,
                            user2 INTEGER NOT NULL,
                            UNIQUE(user1, user2)
                        );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            room_id INTEGER NOT NULL,
                            sender INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            content TEXT,
                            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (room_id) REFERENCES rooms(id)
                        );
            """);

        } catch (SQLException e) {
            log.error("Database initialization failed", e);
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}