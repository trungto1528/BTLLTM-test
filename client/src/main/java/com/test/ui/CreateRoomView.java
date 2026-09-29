package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CreateRoomView extends VBox {

    private final GameApp app;

    private final Label roomIdLabel;
    private final Label statusLabel;

    public CreateRoomView(GameApp app) {

        this.app = app;

        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPrefSize(1000, 700);
        setStyle("-fx-background-color: #20242b;");

        Label title = new Label("CREATE ROOM");

        title.setFont(Font.font(36));
        title.setTextFill(Color.WHITE);

        Label roomTitle = new Label("ROOM ID");

        roomTitle.setFont(Font.font(18));
        roomTitle.setTextFill(Color.LIGHTGRAY);

        roomIdLabel = new Label("------");

        roomIdLabel.setFont(Font.font(42));
        roomIdLabel.setTextFill(Color.WHITE);

        statusLabel = new Label("Creating room...");

        statusLabel.setTextFill(Color.LIGHTGRAY);

        Button startButton = new Button("START");

        startButton.setPrefWidth(250);
        startButton.setPrefHeight(55);

        startButton.setFont(Font.font(18));

        startButton.setDisable(true);

        startButton.setOnAction(e ->
                app.startGame());

        Button backButton = new Button("BACK");

        backButton.setPrefWidth(250);
        backButton.setPrefHeight(45);

        backButton.setOnAction(e ->
                app.showMainMenu());

        getChildren().addAll(
                title,
                roomTitle,
                roomIdLabel,
                statusLabel,
                startButton,
                backButton
        );
    }

    public void setRoomId(String roomId) {

        roomIdLabel.setText(roomId);
        statusLabel.setText("Waiting for players...");
    }
}