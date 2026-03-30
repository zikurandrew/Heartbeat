package com.heartbeat.client.controller;

import com.google.gson.Gson;
import com.heartbeat.client.net.ClientConnection;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Locale;
import java.util.ResourceBundle;

public class AuthController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button actionButton;
    @FXML
    private Hyperlink switchLink;

    private boolean isLoginMode = true;

    @FXML
    private void onAction() {
        if (isInputInvalid()) {
            showErrorAlert("Поля не можуть бути порожніми");
            return;
        }

        try {
            ClientConnection.connect();

            if (isLoginMode) {
                ClientConnection.login(usernameField.getText(), passwordField.getText());
            } else {
                ClientConnection.register(usernameField.getText(), passwordField.getText());
            }

            Message responseLine = ClientConnection.waitResponse();

            if (responseLine != null && responseLine.getType() == MessageType.SYSTEM) {
                String content = responseLine.getContent();

                if (content.equals("LOGIN_OK") || content.equals("REGISTER_OK")) {
                    goToChat(content);
                } else if (content.startsWith("LOGIN_FAIL") || content.startsWith("REGISTER_FAIL")) {
                    String error = content.contains(":") ? content.split(":", 2)[1].trim() : "Невідома помилка";
                    showErrorAlert(error);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Не вдалося з'єднатися з сервером");
        }
    }

    @FXML
    private void onSwitchMode() {
        isLoginMode = !isLoginMode;

        if (isLoginMode) {
            actionButton.setText("SIGN IN");
            switchLink.setText("Don't have an account? Sign up");
        } else {
            actionButton.setText("SIGN UP");
            switchLink.setText("Already have an account? Login");
        }
    }

    private boolean isInputInvalid() {
        return usernameField.getText().isBlank() || passwordField.getText().isBlank();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void goToChat(String messageContent) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        loader.setResources(bundle);
        Scene chatScene = new Scene(loader.load(), 800, 600);

        ChatController chatController = loader.getController();
        chatController.handleInitialMessage(messageContent);

        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(chatScene);
        stage.setMinWidth(400);
        stage.setMinHeight(500);
        stage.centerOnScreen();
    }
}