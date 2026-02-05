package com.heartbeat.server.service;

import com.heartbeat.server.db.UserDAO;
import com.heartbeat.common.model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO = new UserDAO();

    public User register(String username, String rawPassword) throws ServiceException {
        try {
            if (username == null || username.trim().isEmpty()) {
                throw new ServiceException("Username is empty");
            }

            if (rawPassword == null || rawPassword.length() < 6) {
                throw new ServiceException("Password too short");
            }

            if (userDAO.exists(username.trim())) {
                throw new ServiceException("User already exists");
            }

            String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            User user = new User(username.trim(), hash);
            userDAO.save(user);

            log.info("User registered: {}", username.trim());
            return user;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Register failed", e);
            throw new ServiceException("Internal server error");
        }
    }

    public User login(String username, String rawPassword) throws ServiceException {
        try {
            if (username == null || username.trim().isEmpty()) {
                throw new ServiceException("Username is empty");
            }

            User user = userDAO.findByUsername(username.trim());

            if (user == null) {
                throw new ServiceException("User not found");
            }

            if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
                throw new ServiceException("Invalid password");
            }

            log.info("User logged in: {}", username.trim());
            return user;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new ServiceException("Internal server error");
        }
    }
}
