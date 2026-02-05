package com.heartbeat.client;

import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class Client extends Application {

    private final Gson gson = new Gson();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private VBox chatBox;
    private Pane emojiLayer;

    private String username;

    @Override
    public void start(Stage stage) throws Exception {
        // ===========================
        // UI
        // ===========================
        chatBox = new VBox(5);
        ScrollPane chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);

        emojiLayer = new Pane();
        emojiLayer.setPickOnBounds(false);

        TextField inputField = new TextField();
        Button sendBtn = new Button("Send");
        HBox inputArea = new HBox(5, inputField, sendBtn);

        Button emojiBtn = new Button("😀");
        Button pairBtn = new Button("PAIR");

        HBox topBar = new HBox(5, pairBtn, emojiBtn);

        pairBtn.setOnAction(e ->
                sendMessage(new Message(MessageType.PAIR, username, ""))
        );;

        StackPane root = new StackPane();
        VBox mainLayout = new VBox(5, topBar, chatScroll, inputArea);
        root.getChildren().addAll(mainLayout, emojiLayer);

        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("Heartbeat Client");
        stage.show();

        // ===========================
        // CONNECT TO SERVER
        // ===========================
        socket = new Socket("localhost", 5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // ===========================
        // LOGIN
        // ===========================
        TextInputDialog loginDialog = new TextInputDialog();
        loginDialog.setHeaderText("Enter your username:");
        loginDialog.showAndWait().ifPresent(name -> username = name);

        TextInputDialog pwdDialog = new TextInputDialog();
        pwdDialog.setHeaderText("Enter your password:");
        String password = pwdDialog.showAndWait().orElse("");

        // Відправляємо на сервер LOGIN
        Message loginMsg = new Message(MessageType.LOGIN, username, password);
        sendMessage(loginMsg);

        // ===========================
        // LISTENER THREAD
        // ===========================
        Thread listener = new Thread(this::listenServer);
        listener.setDaemon(true);
        listener.start();

        // ===========================
        // SEND CHAT
        // ===========================
        sendBtn.setOnAction(e -> {
            String text = inputField.getText();
            if (!text.isBlank()) {
                Message msg = new Message(MessageType.CHAT, username, text);
                sendMessage(msg); // відправляємо на сервер
                // миттєвий локальний відгук
                chatBox.getChildren().add(new Label(username + ": " + text));
                inputField.clear();
            }
        });

        // ===========================
        // SEND EMOJI
        // ===========================
        emojiBtn.setOnAction(e -> {
            Message msg = new Message(MessageType.EMOJI, username, "😀");
            sendMessage(msg);
            // локально теж показуємо
            showEmoji("😀");
        });
    }

    private void sendMessage(Message msg) {
        out.println(gson.toJson(msg));
    }


    private void listenServer() {
        try {
            String line;

            while ((line = in.readLine()) != null) {

                Message msg = gson.fromJson(line, Message.class);
                if (msg == null || msg.getType() == null) continue;

                switch (msg.getType()) {

                    case SYSTEM -> Platform.runLater(() ->
                            chatBox.getChildren().add(
                                    new Label("[SYSTEM] " + msg.getContent())
                            )
                    );

                    case CHAT -> Platform.runLater(() ->
                            chatBox.getChildren().add(
                                    new Label(msg.getSender() + ": " + msg.getContent())
                            )
                    );

                    case EMOJI -> Platform.runLater(() ->
                            showEmoji(msg.getContent())
                    );
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showEmoji(String emoji) {
        Label lbl = new Label(emoji);
        lbl.setStyle("-fx-font-size: 50px;");
        lbl.setLayoutX(Math.random() * (emojiLayer.getWidth() - 50));
        lbl.setLayoutY(-50);
        emojiLayer.getChildren().add(lbl);

        javafx.animation.TranslateTransition translate =
                new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(3), lbl);
        translate.setToY(emojiLayer.getHeight() + 50);

        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(javafx.util.Duration.seconds(3), lbl);
        fade.setFromValue(1);
        fade.setToValue(0);

        javafx.animation.ParallelTransition pt =
                new javafx.animation.ParallelTransition(translate, fade);
        pt.setOnFinished(e -> emojiLayer.getChildren().remove(lbl));
        pt.play();
    }

    public static void main(String[] args) {
        launch();
    }
}
