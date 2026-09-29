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

    private final Button joinButton;
    private final Button backButton;

    public JoinRoomView(GameApp app) {

        this.app = app;

        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPrefSize(1000, 700);

        setStyle(
                "-fx-background-color: #20242b;"
        );

        // =================================================
        // TITLE
        // =================================================

        Label title =
                new Label("JOIN ROOM");

        title.setFont(
                Font.font(36));

        title.setTextFill(
                Color.WHITE);

        // =================================================
        // ROOM TITLE
        // =================================================

        Label roomTitle =
                new Label("ROOM ID");

        roomTitle.setFont(
                Font.font(18));

        roomTitle.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // ROOM ID
        // =================================================

        roomIdField =
                new TextField();

        roomIdField.setPromptText(
                "Enter room ID");

        roomIdField.setMaxWidth(280);
        roomIdField.setPrefHeight(50);

        roomIdField.setAlignment(
                Pos.CENTER);

        roomIdField.setFont(
                Font.font(20));

        /*
         * Room ID hiện tại là 5 ký tự.
         */
        roomIdField.setOnKeyTyped(e -> {

            String text =
                    roomIdField.getText();

            if (text.length() > 5) {

                roomIdField.setText(
                        text.substring(0, 5));

                roomIdField.positionCaret(5);
            }
        });

        // =================================================
        // JOIN
        // =================================================

        joinButton =
                new Button("JOIN");

        joinButton.setPrefWidth(250);
        joinButton.setPrefHeight(55);

        joinButton.setFont(
                Font.font(18));

        joinButton.setOnAction(
                e -> joinRoom());

        // =================================================
        // STATUS
        // =================================================

        statusLabel =
                new Label();

        statusLabel.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // BACK
        // =================================================

        backButton =
                new Button("BACK");

        backButton.setPrefWidth(250);
        backButton.setPrefHeight(45);

        backButton.setFont(
                Font.font(16));

        backButton.setOnAction(
                e -> {

                    reset();

                    app.showMainMenu();
                });

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                roomTitle,
                roomIdField,
                joinButton,
                statusLabel,
                backButton
        );
    }

    // =====================================================
    // JOIN ROOM
    // =====================================================

    private void joinRoom() {

        String roomId =
                roomIdField.getText()
                        .trim()
                        .toUpperCase();

        // =================================================
        // VALIDATE
        // =================================================

        if (roomId.isEmpty()) {

            statusLabel.setText(
                    "Please enter a room ID.");

            return;
        }

        if (roomId.length() != 5) {

            statusLabel.setText(
                    "Room ID must contain 5 characters.");

            return;
        }

        // =================================================
        // SEND
        // =================================================

        statusLabel.setText(
                "Joining room "
                        + roomId
                        + "...");

        joinButton.setDisable(
                true);

        backButton.setDisable(
                true);

        app.joinRoom(
                roomId);
    }

    // =====================================================
    // ERROR
    // =====================================================

    public void setError(
            String message) {

        statusLabel.setText(
                message);

        joinButton.setDisable(
                false);

        backButton.setDisable(
                false);
    }

    // =====================================================
    // RESET
    // =====================================================

    public void reset() {

        roomIdField.clear();

        statusLabel.setText(
                "");

        joinButton.setDisable(
                false);

        backButton.setDisable(
                false);
    }
}