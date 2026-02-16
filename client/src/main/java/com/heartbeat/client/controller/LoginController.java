package com.heartbeat.client.controller;

import com.heartbeat.client.net.ClientConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank()) return;

        try {
            ClientConnection.connect();
            ClientConnection.login(username, password);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene chatScene = new Scene(loader.load(), 800, 600);

            stage.setScene(chatScene);

            stage.setMinWidth(400);
            stage.setMinHeight(500);

            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}