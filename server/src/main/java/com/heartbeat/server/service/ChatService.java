package com.heartbeat.server.service;

import com.heartbeat.server.db.MessageDAO;
import com.heartbeat.server.db.RoomDAO;
import com.heartbeat.common.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatService {

    private static final Logger log =
            LoggerFactory.getLogger(ChatService.class);

    private final RoomDAO roomDAO = new RoomDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public int getOrCreateRoom(int user1Id, int user2Id)
            throws ServiceException {

        try {
            if (user1Id == user2Id) {
                throw new ServiceException("Cannot chat with yourself");
            }

            int roomId = roomDAO.findRoom(user1Id, user2Id);

            if (roomId == -1) {
                roomId = roomDAO.createRoom(user1Id, user2Id);
                log.info("Room created: {} <-> {}", user1Id, user2Id);
            }

            return roomId;

        } catch (Exception e) {
            log.error("Room error", e);
            throw new ServiceException("Room creation failed");
        }
    }

    public void saveMessage(Message message)
            throws ServiceException {

        try {
            if (message.getContent() == null ||
                    message.getContent().isBlank()) {
                throw new ServiceException("Empty message");
            }

            MessageDAO.save(message);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Save message failed", e);
            throw new ServiceException("Failed to save message");
        }
    }
}
