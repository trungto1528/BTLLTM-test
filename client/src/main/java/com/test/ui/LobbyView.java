package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LobbyView extends VBox {

    private final GameApp app;

    private final Label roomIdLabel;
    private final Label playersLabel;
    private final Label statusLabel;

    private final Button startButton;

    public LobbyView(GameApp app) {

        this.app = app;

        setSpacing(18);
        setAlignment(Pos.CENTER);
        setPrefSize(1000, 700);
        setStyle("-fx-background-color: #20242b;");

        Label title = new Label("GAME LOBBY");

        title.setFont(Font.font(36));
        title.setTextFill(Color.WHITE);

        roomIdLabel = new Label("ROOM: ------");

        roomIdLabel.setFont(Font.font(24));
        roomIdLabel.setTextFill(Color.WHITE);

        playersLabel = new Label(
                "PLAYERS (0/4)\n");

        playersLabel.setFont(Font.font(20));
        playersLabel.setTextFill(Color.LIGHTGRAY);

        statusLabel = new Label(
                "Waiting for players...");

        statusLabel.setTextFill(Color.LIGHTGRAY);

        startButton = new Button("START");

        startButton.setPrefWidth(250);
        startButton.setPrefHeight(55);

        startButton.setFont(Font.font(18));

        startButton.setDisable(true);

        startButton.setOnAction(e ->
                app.startGame());

        Button leaveButton = new Button("LEAVE");

        leaveButton.setPrefWidth(250);
        leaveButton.setPrefHeight(45);

        leaveButton.setOnAction(e ->
                app.showMainMenu());

        getChildren().addAll(
                title,
                roomIdLabel,
                playersLabel,
                statusLabel,
                startButton,
                leaveButton
        );
    }

    public void setRoomId(String roomId) {

        roomIdLabel.setText(
                "ROOM: " + roomId);
    }

    public void setPlayers(
            int current,
            int max) {

        playersLabel.setText(
                "PLAYERS (" +
                current +
                "/" +
                max +
                ")");
    }

    public void setHost(boolean host) {

        startButton.setDisable(!host);

        if (host) {

            statusLabel.setText(
                    "You are the host.");

        } else {

            statusLabel.setText(
                    "Waiting for host...");
        }
    }
}