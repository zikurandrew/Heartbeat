package com.heartbeat.common.model;

public class Message {

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private long timestamp;
    private String roomId;

    public Message() {}
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
