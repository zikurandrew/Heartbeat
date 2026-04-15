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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Locale;
import javafx.scene.control.ComboBox;

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
    private ResourceBundle resources;
    @FXML
    private ComboBox<String> languageBox;
    @FXML
    private Button pairButton;
    @FXML
    private ImageView avatarImage;
    @FXML
    private HBox leftHeader;
    @FXML
    private HBox rightHeader;
    @FXML
    private Label waitingLabel;
    @FXML private StackPane rootPane;
    @FXML private Region switchBackground;
    @FXML private StackPane switchThumb;
    @FXML private Label themeIcon;
    private boolean isDarkTheme = false;

    private final PauseTransition typingTimer = new PauseTransition(Duration.seconds(2));
    private long lastTypingTime = 0;
    private Timeline dotsAnimation;
    private FadeTransition fadeAnimation;
    private HBox emojiMenu;
    private final Gson gson = new Gson();
    private boolean running = true;

    @FXML
    public void initialize() {
        languageBox.getItems().addAll("English", "Українська");
        languageBox.setValue(resources.getLocale().getLanguage().equals("uk") ? "Українська" : "English");
        languageBox.setOnAction(event -> switchLanguage());

        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(21, 21, 21); //горизонталь, вертикаль, радіус
        avatarImage.setClip(clip);

        leftHeader.setVisible(false);
        leftHeader.setManaged(false);
        rightHeader.setVisible(false);
        rightHeader.setManaged(false);

        // Автопрокрутка вниз
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> chatScroll.setVvalue(1.0));

        typingAnimation();
        createEmojiMenu();

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
                addMessageBubble(msg.getSender(), msg.getContent(), false, msg.getTimestamp());
            }
            case SYSTEM -> {
                String content = msg.getContent();

                if (content != null) {
                    if (content.startsWith("PAIRED")) {
                        leftHeader.setVisible(true);
                        leftHeader.setManaged(true);
                        rightHeader.setVisible(true);
                        rightHeader.setManaged(true);
                        waitingLabel.setVisible(false);
                        waitingLabel.setManaged(false);
                    }
                    else if (content.equals("UNPAIR") || content.equals("DISCONNECTED")) {
                        leftHeader.setVisible(false);
                        leftHeader.setManaged(false);
                        rightHeader.setVisible(false);
                        rightHeader.setManaged(false);
                        waitingLabel.setVisible(true);
                        waitingLabel.setManaged(true);
                        waitingLabel.setText(resources.getString("sys.lovely.left"));
                    }

                    addSystemLabel(content);
                }
            }
            case EMOJI -> showEmojiAnimation(msg.getContent());
            case MOOD -> {
                Platform.runLater(() -> {
                    moodLabel.setUserData(msg.getContent());
                    updateMoodDisplay(msg.getContent());
                });
            }
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
                addMessageBubble(msg.getSender(), msg.getContent(), isMe, msg.getTimestamp());
            }
        }
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text.isBlank()) return;

        ClientConnection.send(new Message(MessageType.CHAT, null, text));

        addMessageBubble("Me", text, true, System.currentTimeMillis());
        messageField.clear();
    }

    @FXML
    private void onPair() {
        ClientConnection.send(new Message(MessageType.PAIR, null, ""));
    }

    @FXML
    private void onGood(){
        sendMood("GOOD");
    }

    @FXML
    private void onNeutral(){
        sendMood("NEUTRAL");
    }

    @FXML
    private void onBad(){
        sendMood("BAD");
    }

    private void sendMood(String moodText){
        ClientConnection.send(new Message(MessageType.MOOD, null, moodText));
    }

    public void handleInitialMessage(String type) {
        Platform.runLater(() -> {
            addSystemLabel(type);
        });
    }

    private void updateMoodDisplay(String moodKey) {
        if (moodKey == null) return;

        String prefix = resources.getString("chat.partner.mood") + " ";

        if (moodKey.equals("GOOD")) {
            moodLabel.setText(prefix + resources.getString("mood.good"));
        } else if (moodKey.equals("NEUTRAL")) {
            moodLabel.setText(prefix + resources.getString("mood.neutral"));
        } else if (moodKey.equals("BAD")) {
            moodLabel.setText(prefix + resources.getString("mood.bad"));
        } else {
            moodLabel.setText(prefix + moodKey);
        }
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void switchLanguage() {
        String selected = languageBox.getValue();
        Locale locale = selected.equals("Українська") ? new Locale("uk", "UA") : Locale.ENGLISH;
        resources = ResourceBundle.getBundle("i18n.messages", locale);

        if (waitingLabel.isVisible()) {
            waitingLabel.setText(resources.getString("chat.waiting"));
        }
        moodLabel.setText(resources.getString("chat.partner.mood"));

        if (typingLabel.getText().equals("online") || typingLabel.getText().equals("в мережі")) {
            typingLabel.setText(resources.getString("chat.online"));
        }

        messageField.setPromptText(resources.getString("input.prompt"));
        pairButton.setText(resources.getString("button.pair"));

        // Проходиться по всіх елементах в історії чату
        for (javafx.scene.Node node : chatBox.getChildren()) {
            // Шукає системні повідомлення всередині (якщо вони загорнуті в HBox)
            if (node instanceof javafx.scene.layout.HBox hbox) {
                for (javafx.scene.Node innerNode : hbox.getChildren()) {
                    if (innerNode instanceof Label label && label.getUserData() != null) {
                        // Бере ключ з "бірки" і ставить новий переклад
                        String key = (String) label.getUserData();
                        label.setText(resources.getString(key));
                    }
                }
            }
        }

        if (moodLabel.getUserData() != null) {
            updateMoodDisplay((String) moodLabel.getUserData());
        }
    }

    @FXML
    private void onThemeToggle() {
        isDarkTheme = !isDarkTheme;

        TranslateTransition transition = new TranslateTransition(Duration.millis(300), switchThumb);
        transition.setToX(isDarkTheme ? 26 : 0);
        transition.play();

        themeIcon.setText(isDarkTheme ? "🌙" : "☀");

        switchBackground.setStyle(
                "-fx-background-color: " + (isDarkTheme ? "rgba(30, 20, 40, 0.6)" : "rgba(255,255,255,0.6)") + ";" +
                        "-fx-background-radius: 14;"
        );

        try {
            rootPane.getStylesheets().clear();
            String cssPath = isDarkTheme ? "/css/dark.css" : "/css/chat.css";
            rootPane.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        } catch (NullPointerException e) {
            System.err.println("ПОМИЛКА: Не вдалося знайти файл стилів" +
                    (isDarkTheme ? "dark.css" : "chat.css") + " у папці resources/css/");
        }
    }

    // --- UI Методи ---

    private void addMessageBubble(String sender, String text, boolean isMe, long timestamp) {
        HBox container = new HBox();
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(5, 20, 5, 20));

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-text-fill: " + (isMe ? "white;" : "#444444;") + " -fx-font-size: 14px;");
        textLabel.maxWidthProperty().bind(chatScroll.widthProperty().multiply(0.65));

        Label timeLabel = new Label(formatTime(timestamp));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isMe ? "rgba(255,255,255,0.7);" : "rgba(0,0,0,0.4);"));

        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.BOTTOM_RIGHT);
        // щоб час не відривався далеко від тексту
        timeBox.setPadding(new Insets(2, 0, 0, 10));

        VBox bubble = new VBox();
        bubble.getChildren().addAll(textLabel, timeBox);
        bubble.getStyleClass().add(isMe ? "bubble-me" : "bubble-other");
        bubble.setPadding(new Insets(8, 12, 6, 12));

        VBox messageLayout = new VBox(2);
        messageLayout.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        if (!isMe) {
            Label nameLbl = new Label(sender);
            nameLbl.getStyleClass().add("sender-name");
            nameLbl.setPadding(new Insets(0, 0, 2, 8));
            messageLayout.getChildren().add(nameLbl);
        }

        messageLayout.getChildren().add(bubble);
        container.getChildren().add(messageLayout);

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
                            -fx-text-fill: linear-gradient(to bottom right, rgba(255, 230, 175, 0.95), rgba(255, 125, 150, 0.8));
                            -fx-effect: dropshadow(gaussian, rgba(255, 175, 100, 0.6), 35, 0.2, 0, 5);
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

                case "😡":
                    emojiLabel.setStyle(baseFont + """
                            -fx-font-size: 180px;
                            -fx-padding: 100px;
                            -fx-text-fill: linear-gradient(to bottom right, rgba(255, 100, 130, 0.95), rgba(235, 40, 70, 0.85));
                            -fx-effect: dropshadow(gaussian, rgba(255, 60, 90, 0.5), 30, 0.15, 0, 0);
                            """);

                    TranslateTransition shake = new TranslateTransition(Duration.millis(50), emojiLabel);
                    shake.setByX(15);
                    shake.setAutoReverse(true);
                    shake.setCycleCount(10);

                    FadeTransition fadeAngry = new FadeTransition(Duration.seconds(1.0), emojiLabel);
                    fadeAngry.setDelay(Duration.seconds(0.8));
                    fadeAngry.setToValue(0.0);
                    fadeAngry.setOnFinished(e -> floatingStage.close());

                    shake.play();
                    fadeAngry.play();
                    break;

                case "😌":
                default:
                    if(emoji.equals("😌")){
                        emojiLabel.setStyle(baseFont + """
                            -fx-font-size: 180px;
                            -fx-padding: 100px;
                            -fx-text-fill: linear-gradient(to bottom right, rgba(255, 250, 210, 0.95), rgba(230, 190, 100, 0.8));
                            -fx-effect: dropshadow(gaussian, rgba(240, 200, 110, 0.5), 40, 0.1, 0, 5);
                            """);
                    } else {
                        emojiLabel.setStyle(baseFont + "-fx-font-size: 150px; -fx-padding: 100px;");
                    }

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

    private void createEmojiMenu(){
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
            });

            emojiMenu.getChildren().add(lbl);
        }

        emojiLayer.getChildren().add(emojiMenu);//меню на прозорий шар

        emojiButton.setOnMouseClicked(e ->{
            if(emojiMenu.isVisible() && emojiMenu.getOpacity() == 1.0){
                hideEmojiMenuAnimation();
            } else {
                showEmojiMenuAnimation();
            }
        });

        Platform.runLater(() ->{
            if(emojiButton.getScene() != null){
                emojiButton.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, event ->{ //слухач кліків
                    if(emojiMenu.isVisible() && emojiMenu.getOpacity() > 0){
                        Node clickedNode = (Node) event.getTarget();

                        if(!isNodeInside(clickedNode, emojiMenu) && !isNodeInside(clickedNode, emojiButton)){
                            hideEmojiMenuAnimation();
                        }
                    }
                });
            }
        });

    }

    private boolean isNodeInside(Node node, Node parent){
        while (node != null){
            if(node == parent) return true;
            node = node.getParent();
        }
        return false;
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

    private void typingAnimation() {
        //Анімація крапок
        dotsAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> typingLabel.setText(resources.getString("chat.typing"))),
                new KeyFrame(Duration.seconds(0.4), e -> typingLabel.setText(resources.getString("chat.typing") + ".")),
                new KeyFrame(Duration.seconds(0.8), e -> typingLabel.setText(resources.getString("chat.typing") + "..")),
                new KeyFrame(Duration.seconds(1.2), e -> typingLabel.setText(resources.getString("chat.typing") + "...")),
                new KeyFrame(Duration.seconds(1.6))
        );
        dotsAnimation.setCycleCount(Animation.INDEFINITE);

        // Анімація пульсації
        fadeAnimation = new FadeTransition(Duration.seconds(1), typingLabel);
        fadeAnimation.setFromValue(0.4);
        fadeAnimation.setToValue(1.0);
        fadeAnimation.setCycleCount(Animation.INDEFINITE);
        fadeAnimation.setAutoReverse(true);

        typingTimer.setOnFinished(event -> {
            typingLabel.setText(resources.getString("chat.online"));

            if (dotsAnimation != null) dotsAnimation.stop();
            if (fadeAnimation != null) fadeAnimation.stop();

            typingLabel.setOpacity(1.0);
        });
    }

    private void addSystemLabel(String text) {
        String displayText = text;
        String cssStyle = "";

        if (text.equals("LOGIN_OK")) {
            displayText = resources.getString("sys.login.ok");
            cssStyle = """
                -fx-background-color: rgba(175, 143, 189, 0.15);
                -fx-text-fill: #93689E;
                -fx-padding: 6 15 6 15;
                -fx-background-radius: 20;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
            """;
        }
        else if (text.equals("REGISTER_OK")) {
            displayText = resources.getString("sys.register.ok");
            cssStyle = """
                -fx-background-color: rgba(175, 143, 189, 0.15);
                -fx-text-fill: #93689E;
                -fx-padding: 6 15 6 15;
                -fx-background-radius: 20;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
            """;
        } else if (text.startsWith("PAIRED")) {
            displayText = resources.getString("sys.paired");
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
            displayText = resources.getString("sys.unpaired");
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

        if (text.equals("LOGIN_OK")) label.setUserData("sys.login.ok");
        else if (text.equals("REGISTER_OK")) label.setUserData("sys.register.ok");
        else if (text.startsWith("PAIRED")) label.setUserData("sys.paired");
        else if (text.equals("UNPAIR") || text.equals("DISCONNECTED")) label.setUserData("sys.unpaired");

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