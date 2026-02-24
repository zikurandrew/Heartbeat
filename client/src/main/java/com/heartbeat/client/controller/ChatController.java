package com.heartbeat.client.controller;

import com.google.gson.Gson;
import com.heartbeat.client.net.ClientConnection;
import com.heartbeat.common.model.Message;
import com.heartbeat.common.model.MessageType;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    private Button emojiButton;
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
    private long lastTypingTime = 0;
    private Timeline dotsAnimation;
    private FadeTransition fadeAnimation;
    private HBox emojiMenu;
    private PauseTransition hideMenuTimer;
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
        createHoverEmojiMenu();

        messageField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                long now = System.currentTimeMillis();
                if (now - lastTypingTime > 1500) {
                    ClientConnection.send(new Message(MessageType.TYPING, null, "typing"));
                    lastTypingTime = now;
                }
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

                }

                if (content != null) {
                    addSystemLabel(content);
                }
            }
            case EMOJI -> showEmojiAnimation(msg.getContent());
            case MOOD -> moodLabel.setText(msg.getContent());
            case TYPING -> {
                Platform.runLater(() ->{

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
        String emoji = "♥";
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

    private void showEmojiAnimation(String emoji) {
        Platform.runLater(() -> {
            Stage floatingStage = new Stage();
            floatingStage.initStyle(StageStyle.TRANSPARENT);
            floatingStage.setAlwaysOnTop(true);

            Label emojiLabel = new Label(emoji);

            String baseFont = "-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji', sans-serif;";

            switch (emoji) {
                case "♥":
                    emojiLabel.setStyle(baseFont + """
                        -fx-font-size: 150px;
                        -fx-padding: 50px;
                        -fx-text-fill: rgba(230, 130, 210, 0.85);
                        -fx-effect: dropshadow(gaussian, rgba(255, 105, 180, 0.6), 25, 0.2, 0, 5);
                    """);

                    // Пульсує 2 секунди
                    ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.6), emojiLabel);
                    pulse.setByX(0.2); // Збільшується на 20%
                    pulse.setByY(0.2);
                    pulse.setAutoReverse(true);
                    pulse.setCycleCount(4);

                    // Після пульсації плавно зникає
                    FadeTransition fadeHeart = new FadeTransition(Duration.seconds(0.8), emojiLabel);
                    fadeHeart.setDelay(Duration.seconds(2.0));
                    fadeHeart.setFromValue(1.0);
                    fadeHeart.setToValue(0.0);
                    fadeHeart.setOnFinished(e -> floatingStage.close());

                    pulse.play();
                    fadeHeart.play();
                    break;

                case "🥰":
                case "😭":
                    if(emoji.equals("😭")){
                        emojiLabel.setStyle(baseFont + """
                            -fx-font-size: 150px;
                            -fx-padding: 100px;
                            -fx-text-fill: linear-gradient(to bottom right, rgba(200, 240, 255, 0.95), rgba(90, 180, 255, 0.7));
                            -fx-effect: dropshadow(gaussian, rgba(90, 170, 255, 0.5), 35, 0.1, 0, 5);
                            """);

                        TranslateTransition shiver = new TranslateTransition(Duration.millis(150), emojiLabel);
                        shiver.setByX(6); // Трохи зсувається вбік
                        shiver.setAutoReverse(true);
                        shiver.setCycleCount(10);

                        shiver.play();

                    } else {
                        emojiLabel.setStyle(baseFont + """
                            -fx-font-size: 150px;
                            -fx-padding: 100px;
                            -fx-text-fill: linear-gradient(to bottom right, rgba(255, 240, 180, 0.95), rgba(255, 130, 160, 0.8));
                            -fx-effect: dropshadow(gaussian, rgba(255, 190, 100, 0.6), 35, 0.2, 0, 5);
                            """);
                    }

                    ScaleTransition grow = new ScaleTransition(Duration.seconds(2.0), emojiLabel);
                    grow.setByX(1.0);
                    grow.setByY(1.0);

                    FadeTransition fadeEmoji = new FadeTransition(Duration.seconds(0.8), emojiLabel);
                    fadeEmoji.setDelay(Duration.seconds(0.6));
                    fadeEmoji.setFromValue(1.0);
                    fadeEmoji.setToValue(0.0);
                    fadeEmoji.setOnFinished(e -> floatingStage.close());

                    grow.play();
                    fadeEmoji.play();
                    break;

                default:
                    emojiLabel.setStyle(baseFont + "-fx-font-size: 150px;");

                    FadeTransition fadeDefault = new FadeTransition(Duration.seconds(2.0), emojiLabel);
                    fadeDefault.setFromValue(1.0);
                    fadeDefault.setToValue(0.0);
                    fadeDefault.setOnFinished(e -> floatingStage.close());

                    TranslateTransition floatUp = new TranslateTransition(Duration.seconds(2.0), emojiLabel);
                    floatUp.setByY(-50);

                    fadeDefault.play();
                    floatUp.play();
                    break;
            }

            StackPane root = new StackPane(emojiLabel);
            root.setStyle("-fx-background-color: transparent;");

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            floatingStage.setScene(scene);

            floatingStage.show();
        });
    }

    private void createHoverEmojiMenu(){
        emojiMenu = new HBox(12);
        emojiMenu.setStyle("""
            -fx-background-color: rgba(255, 255, 255, 0.75);
            -fx-background-radius: 30;
            -fx-padding: 8 15 8 15;
            -fx-effect: dropshadow(gaussian, rgba(175, 143, 189, 0.4), 15, 0, 0, 5);
        """);
        emojiMenu.setVisible(false);
        emojiMenu.setOpacity(0);

        String[] emojis = {"♥", "😭", "🥰", "😡", "😌"};
        for (String em : emojis){
            Label lbl = new Label(em);
            lbl.setStyle("""
                    -fx-font-size: 26px;
                    -fx-cursor: hand;
                    -fx-text-fill: #AF8FBD;
                    -fx-font-family: 'Segoe UI Emoji', sans-serif;
                    """);

            //анімація збільшення при наведені
            lbl.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(150), lbl);
                st.setToX(1.4); st.setToY(1.4); st.play();
            });
            lbl.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(150), lbl);
                st.setToX(1.0); st.setToY(1.0); st.play();
            });

            lbl.setOnMouseClicked(e ->{
                ClientConnection.send(new Message(MessageType.EMOJI, null, em));
                showEmojiAnimation(em);
                hideMenuTimer.playFromStart();
            });

            emojiMenu.getChildren().add(lbl);
        }

        emojiLayer.getChildren().add(emojiMenu);//меню на прозорий шар

        hideMenuTimer = new PauseTransition(Duration.millis(300));
        hideMenuTimer.setOnFinished(e -> hideEmojiMenuAnimation());

        emojiButton.setOnMouseEntered(e -> showEmojiMenuAnimation());//відкриття при наведені

        emojiButton.setOnMouseExited(e -> hideMenuTimer.playFromStart());//якщо мишка зійшла ховаємо

        emojiMenu.setOnMouseEntered(e -> hideMenuTimer.stop());//якщо мишка зайшла в меню зупиняємо таймер

        emojiMenu.setOnMouseExited(e -> hideMenuTimer.playFromStart()); //якщо мишка зійшла знову ставимо таймер

    }

    private void showEmojiMenuAnimation(){
        if (emojiMenu.isVisible() && emojiMenu.getOpacity() == 1.0 ) return;

        Bounds bounds = emojiButton.localToScene(emojiButton.getBoundsInLocal());//позиція кнопки

        emojiMenu.setLayoutX(bounds.getMinX() - 60);
        emojiMenu.setLayoutY(bounds.getMinY() - 60);

        emojiMenu.setVisible(true);

        //зсув на право
        TranslateTransition slideRight = new TranslateTransition(Duration.millis(250), emojiMenu);
        slideRight.setFromX(-30);
        slideRight.setToX(0);

        //плавна поява
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), emojiMenu);
        fadeIn.setFromValue(emojiMenu.getOpacity());
        fadeIn.setToValue(1.0);

        slideRight.play();
        fadeIn.play();
    }

    private void hideEmojiMenuAnimation(){
        TranslateTransition slideLeft = new TranslateTransition(Duration.millis(200), emojiMenu);
        slideLeft.setToX(-20); //їде вліво

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), emojiMenu);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> emojiMenu.setVisible(false));//невидимий, коли анімація закінчується

        slideLeft.play();
        fadeOut.play();

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
            typingLabel.setText("online");
            if(dotsAnimation != null) dotsAnimation.stop();
            if(fadeAnimation != null) fadeAnimation.stop();
        });

    }
    private void addSystemLabel(String text) {
        String displayText = text;
        String cssStyle = "";

        if (text.equals("LOGIN_OK")) {
            displayText = "✨ You entered heartbeat";
            cssStyle = """
                -fx-background-color: rgba(175, 143, 189, 0.15); 
                -fx-text-fill: #93689E; 
                -fx-padding: 6 15 6 15; 
                -fx-background-radius: 20; 
                -fx-font-size: 11px;
                -fx-font-weight: bold;
            """;
        } else if (text.startsWith("PAIRED")) {
            displayText = "🔗 Connected to lovely";
            cssStyle = """
                -fx-background-color: rgba(175, 143, 189, 0.25); 
                -fx-text-fill: #8A5A9E; 
                -fx-padding: 8 18 8 18; 
                -fx-background-radius: 20; 
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(gaussian, rgba(175,143,189,0.3), 5, 0, 0, 2);
            """;
        } else if (text.equals("UNPAIR") || text.equals("DISCONNECTED")) {
            displayText = "💔 Lovely disconnected";
            cssStyle = """
                -fx-background-color: rgba(255, 100, 100, 0.15);
                -fx-text-fill: #D9534F;
                -fx-padding: 6 15 6 15;
                -fx-background-radius: 20;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
            """;
        } else {
            cssStyle = "-fx-text-fill: gray; -fx-font-size: 10px; -fx-padding: 5;";
        }
        Label label = new Label(displayText);
        label.setStyle(cssStyle);

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