package com.heartbeat.client.controller;

import com.heartbeat.client.net.ClientConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private void onLogin() throws Exception {

        String username = usernameField.getText();
        if (username.isBlank()) return;

        ClientConnection.connect();
        ClientConnection.login(username);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/chat.fxml")
        );

        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(loader.load(), 600, 400));
    }
}
