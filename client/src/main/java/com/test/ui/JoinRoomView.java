package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class JoinRoomView extends VBox {

    private final GameApp app;

    private final TextField roomIdField;
    private final Label statusLabel;

    public JoinRoomView(GameApp app) {

        this.app = app;

        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPrefSize(1000, 700);
        setStyle("-fx-background-color: #20242b;");

        Label title = new Label("JOIN ROOM");

        title.setFont(Font.font(36));
        title.setTextFill(Color.WHITE);

        Label roomTitle = new Label("ROOM ID");

        roomTitle.setFont(Font.font(18));
        roomTitle.setTextFill(Color.LIGHTGRAY);

        roomIdField = new TextField();

        roomIdField.setPromptText("Enter room ID");

        roomIdField.setMaxWidth(280);
        roomIdField.setPrefHeight(50);

        roomIdField.setAlignment(Pos.CENTER);

        roomIdField.setFont(Font.font(20));

        Button joinButton = new Button("JOIN");

        joinButton.setPrefWidth(250);
        joinButton.setPrefHeight(55);

        joinButton.setFont(Font.font(18));

        joinButton.setOnAction(e ->
                joinRoom());

        statusLabel = new Label();

        statusLabel.setTextFill(Color.LIGHTGRAY);

        Button backButton = new Button("BACK");

        backButton.setPrefWidth(250);
        backButton.setPrefHeight(45);

        backButton.setOnAction(e ->
                app.showMainMenu());

        getChildren().addAll(
                title,
                roomTitle,
                roomIdField,
                joinButton,
                statusLabel,
                backButton
        );
    }

    private void joinRoom() {

        String roomId =
                roomIdField.getText()
                        .trim()
                        .toUpperCase();

        if (roomId.isEmpty()) {

            statusLabel.setText(
                    "Please enter a room ID.");

            return;
        }

        statusLabel.setText(
                "Joining room " + roomId + "...");

        /*
         * Sau này gọi WebSocket:
         *
         * JOIN_ROOM|A7K29
         */

        app.joinRoom(roomId);
    }
}