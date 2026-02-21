package com.heartbeat.client.controller;

import com.google.gson.Gson;
import com.heartbeat.client.net.ClientConnection;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.scene.image.ImageView;
import java.io.IOException;

public class ChatController {

    @FXML
    private VBox chatBox;
    @FXML
    private ScrollPane chatScroll;
    @FXML
    private TextField messageField;
    @FXML
    private Pane emojiLayer;
    @FXML
    private Label typingLabel;
    @FXML
    private Label moodLabel;
    @FXML
    private ImageView avatarImage;
    @FXML
    private HBox leftHeader;
    @FXML
    private HBox rightHeader;
    @FXML
    private Label waitingLabel;

    private final PauseTransition typingTimer = new PauseTransition(Duration.seconds(2));
    private Timeline dotsAnimation;
    private FadeTransition fadeAnimation;
    private final Gson gson = new Gson();
    private boolean running = true;

    @FXML
    public void initialize() {
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(21, 21, 21); //горизонталь, вертикаль, радіус
        avatarImage.setClip(clip);

        leftHeader.setVisible(false);
        leftHeader.setManaged(false);
        rightHeader.setVisible(false);
        rightHeader.setManaged(false);

        // Автопрокрутка вниз
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> chatScroll.setVvalue(1.0));

        typingAnimation();

        messageField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue.isEmpty() && !newValue.isEmpty()) {
                ClientConnection.send(new Message(MessageType.TYPING, null, "typing"));
            }
        });

        Thread listener = new Thread(this::listenServer);
        listener.setDaemon(true);
        listener.start();
    }

    private void listenServer() {
        try {
            String line;
            while (running && (line = ClientConnection.getIn().readLine()) != null) {
                Message msg = gson.fromJson(line, Message.class);
                if (msg == null) continue;

                Platform.runLater(() -> {
                    processMessage(msg);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Message msg) {
        switch (msg.getType()) {
            case CHAT -> {
                typingLabel.setVisible(false);
                typingLabel.setManaged(false);
                dotsAnimation.stop();
                fadeAnimation.stop();
                addMessageBubble(msg.getSender(), msg.getContent(), false);
            }
            case SYSTEM -> {
                String content = msg.getContent();

                if (content != null && content.startsWith("PAIRED")) {

                    leftHeader.setVisible(true);
                    leftHeader.setManaged(true);
                    rightHeader.setVisible(true);
                    rightHeader.setManaged(true);

                    waitingLabel.setVisible(false);
                    waitingLabel.setManaged(false);

                    showConnectedBanner();
                } else {
                    addSystemLabel(content);
                }
            }
            case EMOJI -> showEmojiAnimation(msg.getContent());
            case MOOD -> moodLabel.setText(msg.getContent());
            case TYPING -> {
                Platform.runLater(() ->{
                    typingLabel.setVisible(true);
                    typingLabel.setManaged(true);

                    if(dotsAnimation.getStatus() != Animation.Status.RUNNING) {
                        dotsAnimation.playFromStart();
                        fadeAnimation.playFromStart();
                    }

                    typingTimer.playFromStart();
                });
            }
            case HISTORY -> {
                boolean isMe = msg.getSender().equals(ClientConnection.getUsername());
                addMessageBubble(msg.getSender(), msg.getContent(), isMe);
            }
        }
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text.isBlank()) return;

        ClientConnection.send(new Message(MessageType.CHAT, null, text));

        addMessageBubble("Me", text, true);
        messageField.clear();
    }

    @FXML
    private void onPair() {
        ClientConnection.send(new Message(MessageType.PAIR, null, ""));
    }

    @FXML
    private void onEmoji() {
        String emoji = "😀";
        ClientConnection.send(new Message(MessageType.EMOJI, null, emoji));
        showEmojiAnimation(emoji);
    }

    @FXML
    private void onGood(){
        sendMood("good");
    }

    @FXML
    private void onNeutral(){
        sendMood("neutral");
    }

    @FXML
    private void onBad(){
        sendMood("bad");
    }

    private void sendMood(String moodText){
        ClientConnection.send(new Message(MessageType.MOOD, null, moodText));
    }

    // --- UI Методи ---

    private void addMessageBubble(String sender, String text, boolean isMe) {
        HBox container = new HBox();
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(5, 20, 5, 20));

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.getStyleClass().add(isMe ? "bubble-me" : "bubble-other");

        bubble.maxWidthProperty().bind(chatScroll.widthProperty().multiply(0.75));

        VBox content = new VBox();
        if (!isMe) {
            Label nameLbl = new Label(sender);
            nameLbl.getStyleClass().add("sender-name");
            content.getChildren().add(nameLbl);
            content.setAlignment(Pos.CENTER_LEFT);
        } else {
            content.setAlignment(Pos.CENTER_RIGHT);
        }

        content.getChildren().add(bubble);
        container.getChildren().add(content);

        chatBox.getChildren().add(container);
    }

    private void addSystemLabel(String text) {
        Label label = new Label("[SYSTEM] " + text);
        label.setStyle("-fx-text-fill: gray; -fx-font-size: 10px; -fx-padding: 5;");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        chatBox.getChildren().add(label);
    }

    private void showEmojiAnimation(String emoji) {
        Label lbl = new Label(emoji);
        lbl.setStyle("-fx-font-size: 50px;");
        lbl.setLayoutX(Math.random() * (emojiLayer.getWidth() - 50));
        lbl.setLayoutY(-50);

        emojiLayer.getChildren().add(lbl);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(3), lbl);
        tt.setToY(emojiLayer.getHeight() + 50);

        FadeTransition ft = new FadeTransition(Duration.seconds(3), lbl);
        ft.setFromValue(1);
        ft.setToValue(0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> emojiLayer.getChildren().remove(lbl));
        pt.play();
    }

    private void typingAnimation(){
        //Анімація крапок
        dotsAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> typingLabel.setText("typing")),
                new KeyFrame(Duration.seconds(0.4), e -> typingLabel.setText("typing.")),
                new KeyFrame(Duration.seconds(0.8), e -> typingLabel.setText("typing..")),
                new KeyFrame(Duration.seconds(1.2), e -> typingLabel.setText("typing...")),
                new KeyFrame(Duration.seconds(1.6))
        );
        dotsAnimation.setCycleCount(Animation.INDEFINITE);

        //Анімація пульсації
        fadeAnimation = new FadeTransition(Duration.seconds(1), typingLabel);
        fadeAnimation.setFromValue(0.4);
        fadeAnimation.setToValue(1.0);
        fadeAnimation.setCycleCount(Animation.INDEFINITE);
        fadeAnimation.setAutoReverse(true);

        typingTimer.setOnFinished(event ->{
            typingLabel.setVisible(false);
            typingLabel.setManaged(false);
            if(dotsAnimation != null) dotsAnimation.stop();
            if(fadeAnimation != null) fadeAnimation.stop();
        });

        typingLabel.setVisible(false);
        typingLabel.setManaged(false);
    }
    private void showConnectedBanner() {

        Label label = new Label("🔗 Connected to lovely");
        label.setStyle("""
        -fx-background-color: #1f1f1f;
        -fx-text-fill: #00ff99;
        -fx-padding: 8 15 8 15;
        -fx-background-radius: 20;
        -fx-font-size: 12px;
    """);

        HBox wrapper = new HBox(label);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(10, 0, 10, 0));

        label.setOpacity(0);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.6), label);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        chatBox.getChildren().add(wrapper);
    }

}