package com.heartbeat.server.db;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    // Файл у тій же папці server/
    private static final String DB_FILE = "heartbeat.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

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
        // Створюємо файл, якщо його ще немає
        try {
            File dbFile = new File(DB_FILE);
            if (dbFile.createNewFile()) {
                log.info("Database file created: " + dbFile.getAbsolutePath());
            } else {
                log.info("Database file already exists: " + dbFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create database file", e);
            throw new RuntimeException(e);
        }

        // Ініціалізація таблиць та дефолтних користувачів
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Таблиця користувачів
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL
                )
            """);

            // Таблиця кімнат
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user1 INTEGER NOT NULL,
                    user2 INTEGER NOT NULL,
                    UNIQUE(user1, user2)
                )
            """);

            // Таблиця повідомлень
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    room_id TEXT NOT NULL,
                    sender TEXT NOT NULL,
                    receiver TEXT NOT NULL,
                    type TEXT NOT NULL,
                    content TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (room_id) REFERENCES rooms(id)
                )
            """);

            // Додаємо дефолтних користувачів
            addDefaultUsers(conn);

            log.info("Database initialized successfully");

        } catch (SQLException e) {
            log.error("Database initialization failed", e);
            throw new RuntimeException(e);
        }
    }

    private static void addDefaultUsers(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Статичні хеші для пароля "123456"
            String passwordHash = BCrypt.hashpw("123456", BCrypt.gensalt());

            // Bob
            var rs = stmt.executeQuery("SELECT 1 FROM users WHERE username='bob'");
            if (!rs.next()) {
                stmt.executeUpdate("INSERT INTO users(username, password_hash) VALUES ('bob', '" + passwordHash + "')");
                log.info("User 'bob' added");
            }

            // Alice
            rs = stmt.executeQuery("SELECT 1 FROM users WHERE username='alice'");
            if (!rs.next()) {
                stmt.executeUpdate("INSERT INTO users(username, password_hash) VALUES ('alice', '" + passwordHash + "')");
                log.info("User 'alice' added");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
