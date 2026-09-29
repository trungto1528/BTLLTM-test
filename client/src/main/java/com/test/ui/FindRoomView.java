package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class FindRoomView extends VBox {

    private final GameApp app;

    private final VBox roomList;

    private final Label statusLabel;

    private final Button refreshButton;
    private final Button backButton;

    public FindRoomView(GameApp app) {

        this.app = app;

        setSpacing(18);
        setAlignment(Pos.TOP_CENTER);
        setPrefSize(1000, 700);

        setPadding(
                new Insets(40));

        setStyle(
                "-fx-background-color: #20242b;"
        );

        // =================================================
        // TITLE
        // =================================================

        Label title =
                new Label("FIND ROOM");

        title.setFont(
                Font.font(36));

        title.setTextFill(
                Color.WHITE);

        // =================================================
        // STATUS
        // =================================================

        statusLabel =
                new Label("Searching for rooms...");

        statusLabel.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // ROOM LIST
        // =================================================

        roomList =
                new VBox(10);

        roomList.setAlignment(
                Pos.CENTER);

        roomList.setMaxWidth(
                600);

        // =================================================
        // REFRESH
        // =================================================

        refreshButton =
                new Button("REFRESH");

        refreshButton.setPrefWidth(250);
        refreshButton.setPrefHeight(45);

        refreshButton.setFont(
                Font.font(16));

        refreshButton.setOnAction(
                e -> refresh());

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
                e -> app.showMainMenu());

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                statusLabel,
                roomList,
                refreshButton,
                backButton
        );
    }

    // =====================================================
    // SHOW
    // =====================================================

    public void onShow() {

        refresh();
    }

    // =====================================================
    // REFRESH
    // =====================================================

    public void refresh() {

        roomList.getChildren().clear();

        statusLabel.setText(
                "Searching for rooms...");

        refreshButton.setDisable(
                true);

        app.findRooms();
    }

    // =====================================================
    // ROOM LIST
    // =====================================================

    public void clearRooms() {

        roomList.getChildren().clear();
    }

    public void addRoom(
            String roomId,
            int currentPlayers,
            int maxPlayers) {

        HBox row =
                new HBox(15);

        row.setAlignment(
                Pos.CENTER_LEFT);

        row.setPrefWidth(
                600);

        row.setPadding(
                new Insets(10));

        row.setStyle(
                "-fx-background-color: #30353d;"
        );

        // =================================================
        // ROOM ID
        // =================================================

        Label roomLabel =
                new Label(roomId);

        roomLabel.setFont(
                Font.font(20));

        roomLabel.setTextFill(
                Color.WHITE);

        roomLabel.setPrefWidth(
                250);

        // =================================================
        // PLAYERS
        // =================================================

        Label playersLabel =
                new Label(
                        currentPlayers
                                + "/"
                                + maxPlayers);

        playersLabel.setFont(
                Font.font(18));

        playersLabel.setTextFill(
                Color.LIGHTGRAY);

        playersLabel.setPrefWidth(
                100);

        // =================================================
        // JOIN
        // =================================================

        Button joinButton =
                new Button("JOIN");

        joinButton.setPrefWidth(
                100);

        joinButton.setPrefHeight(
                40);

        joinButton.setOnAction(
                e -> {

                    joinButton.setDisable(
                            true);

                    app.joinRoom(
                            roomId);
                });

        // =================================================
        // ADD
        // =================================================

        row.getChildren().addAll(
                roomLabel,
                playersLabel,
                joinButton
        );

        roomList.getChildren().add(
                row);
    }

    // =====================================================
    // EMPTY
    // =====================================================

    public void showEmpty() {

        roomList.getChildren().clear();

        Label emptyLabel =
                new Label(
                        "No available rooms.");

        emptyLabel.setFont(
                Font.font(18));

        emptyLabel.setTextFill(
                Color.LIGHTGRAY);

        roomList.getChildren().add(
                emptyLabel);

        statusLabel.setText(
                "No rooms available.");

        refreshButton.setDisable(
                false);
    }

    // =====================================================
    // FINISHED
    // =====================================================

    public void finishLoading() {

        refreshButton.setDisable(
                false);

        if (roomList.getChildren().isEmpty()) {

            showEmpty();

            return;
        }

        statusLabel.setText(
                "Available rooms:");
    }

    // =====================================================
    // ERROR
    // =====================================================

    public void setError(
            String message) {

        statusLabel.setText(
                message);

        refreshButton.setDisable(
                false);
    }
}