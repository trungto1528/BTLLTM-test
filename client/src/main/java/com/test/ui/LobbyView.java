package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LobbyView extends VBox {

    private final Label roomIdLabel;
    private final Label playersLabel;
    private final Label statusLabel;

    private final Button startButton;
    private final Button leaveButton;

    public LobbyView(GameApp app) {

        setSpacing(18);
        setAlignment(Pos.CENTER);
        setPrefSize(1000, 700);

        setStyle(
                "-fx-background-color: #20242b;"
        );

        // =================================================
        // TITLE
        // =================================================

        Label title =
                new Label("GAME LOBBY");

        title.setFont(
                Font.font(36));

        title.setTextFill(
                Color.WHITE);

        // =================================================
        // ROOM ID
        // =================================================

        roomIdLabel =
                new Label("ROOM: ------");

        roomIdLabel.setFont(
                Font.font(24));

        roomIdLabel.setTextFill(
                Color.WHITE);

        // =================================================
        // PLAYERS
        // =================================================

        playersLabel =
                new Label("PLAYERS (0/4)");

        playersLabel.setFont(
                Font.font(20));

        playersLabel.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // STATUS
        // =================================================

        statusLabel =
                new Label(
                        "Waiting for players..."
                );

        statusLabel.setFont(
                Font.font(16));

        statusLabel.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // START
        // =================================================

        startButton =
                new Button("START");

        startButton.setPrefWidth(250);
        startButton.setPrefHeight(55);

        startButton.setFont(
                Font.font(18));

        /*
         * Chỉ host mới được START.
         */
        startButton.setDisable(true);

        startButton.setOnAction(
                e -> app.startGame());

        // =================================================
        // LEAVE
        // =================================================

        leaveButton =
                new Button("LEAVE");

        leaveButton.setPrefWidth(250);
        leaveButton.setPrefHeight(45);

        leaveButton.setFont(
                Font.font(16));

        /*
         * Không chỉ chuyển UI.
         *
         * Phải báo server:
         *
         * LEAVE_ROOM
         */
        leaveButton.setOnAction(
                e -> app.leaveRoom());

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                roomIdLabel,
                playersLabel,
                statusLabel,
                startButton,
                leaveButton
        );
    }

    // =====================================================
    // ROOM ID
    // =====================================================

    public void setRoomId(
            String roomId) {

        if (roomId == null
                || roomId.isBlank()) {

            roomIdLabel.setText(
                    "ROOM: ------");

            return;
        }

        roomIdLabel.setText(
                "ROOM: " + roomId);
    }

    // =====================================================
    // PLAYERS
    // =====================================================

    public void setPlayers(
            int current,
            int max) {

        playersLabel.setText(
                "PLAYERS ("
                        + current
                        + "/"
                        + max
                        + ")");
    }

    // =====================================================
    // HOST
    // =====================================================

    public void setHost(
            boolean host) {

        startButton.setDisable(
                !host);

        if (host) {

            statusLabel.setText(
                    "You are the host.");

        } else {

            statusLabel.setText(
                    "Waiting for host...");
        }
    }

    // =====================================================
    // WAITING
    // =====================================================

    public void setWaitingStatus() {

        statusLabel.setText(
                "Waiting for players...");
    }

    // =====================================================
    // STARTING
    // =====================================================

    public void setStartingStatus() {

        statusLabel.setText(
                "Starting game...");

        startButton.setDisable(true);
        leaveButton.setDisable(true);
    }

    // =====================================================
    // ERROR
    // =====================================================

    public void setError(
            String message) {

        statusLabel.setText(
                message);
    }

    // =====================================================
    // RESET
    // =====================================================

    public void reset() {

        roomIdLabel.setText(
                "ROOM: ------");

        playersLabel.setText(
                "PLAYERS (0/4)");

        statusLabel.setText(
                "Waiting for players...");

        startButton.setDisable(true);
        leaveButton.setDisable(false);
    }
}