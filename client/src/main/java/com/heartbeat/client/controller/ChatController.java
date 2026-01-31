package com.heartbeat.client.controller;

import com.google.gson.Gson;
import com.heartbeat.client.net.ClientConnection;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    private final Gson gson = new Gson();

    @FXML
    public void initialize() {

        ClientConnection.pair();

        new Thread(() -> {
            try {
                String line;
                while ((line = ClientConnection.getIn().readLine()) != null) {
                    Message msg = gson.fromJson(line, Message.class);

                    Platform.runLater(() ->
                            chatArea.appendText(
                                    msg.getSender() + ": " +
                                            msg.getContent() + "\n"
                            )
                    );
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text.isBlank()) return;

        ClientConnection.send(new Message(
                MessageType.CHAT,
                null,
                text
        ));

        messageField.clear();
    }
}
