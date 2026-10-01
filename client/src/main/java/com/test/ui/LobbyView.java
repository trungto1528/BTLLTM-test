package com.test.ui;

import com.test.GameApp;
import com.test.common.map.MapInfo;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LobbyView extends VBox {

    private final GameApp app;

    private final Label roomIdLabel;
    private final Label playersLabel;
    private final Label statusLabel;
    private final Label mapTitleLabel;

    private final ComboBox<MapInfo> mapComboBox;

    private final Button startButton;
    private final Button leaveButton;

    private boolean updatingMap;

    public LobbyView(GameApp app) {

        this.app = app;

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
        // MAP
        // =================================================

        mapTitleLabel =
                new Label("MAP");

        mapTitleLabel.setFont(
                Font.font(16));

        mapTitleLabel.setTextFill(
                Color.LIGHTGRAY);

        mapComboBox =
                new ComboBox<>();

        mapComboBox.setPrefWidth(300);
        mapComboBox.setPrefHeight(42);

        /*
         * Chỉ hiển thị tên map.
         */
        mapComboBox.setCellFactory(
                list -> new javafx.scene.control.ListCell<>() {

                    @Override
                    protected void updateItem(
                            MapInfo item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty);

                        if (empty || item == null) {

                            setText(null);

                        } else {

                            setText(
                                    item.getName());
                        }
                    }
                });

        mapComboBox.setButtonCell(
                new javafx.scene.control.ListCell<>() {

                    @Override
                    protected void updateItem(
                            MapInfo item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty);

                        if (empty || item == null) {

                            setText("No map");

                        } else {

                            setText(
                                    item.getName());
                        }
                    }
                });

        mapComboBox.setOnAction(
                event -> {

                    if (updatingMap) {
                        return;
                    }

                    MapInfo selected =
                            mapComboBox.getValue();

                    if (selected == null) {
                        return;
                    }

                    if (!app.isCurrentHost()) {
                        return;
                    }

                    app.changeMap(
                            selected.getId());
                });

        // =================================================
        // STATUS
        // =================================================

        statusLabel =
                new Label(
                        "Waiting for players...");

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

        leaveButton.setOnAction(
                e -> app.leaveRoom());

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                roomIdLabel,
                playersLabel,
                mapTitleLabel,
                mapComboBox,
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
    // MAP LIST
    // =====================================================

    public void setMaps(
            java.util.List<MapInfo> maps) {

        updatingMap = true;

        try {

            mapComboBox.setItems(
                    FXCollections.observableArrayList(
                            maps));
        } finally {

            updatingMap = false;
        }
    }

    // =====================================================
    // CURRENT MAP
    // =====================================================

    public void setMap(
            String mapId) {

        if (mapId == null
                || mapId.isBlank()) {

            return;
        }

        updatingMap = true;

        try {

            for (MapInfo map
                    : mapComboBox.getItems()) {

                if (mapId.equals(
                        map.getId())) {

                    mapComboBox.setValue(
                            map);

                    return;
                }
            }

        } finally {

            updatingMap = false;
        }
    }

    // =====================================================
    // HOST
    // =====================================================

    public void setHost(
            boolean host) {

        startButton.setDisable(
                !host);

        mapComboBox.setDisable(
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
        mapComboBox.setDisable(true);
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

        updatingMap = true;

        try {

            mapComboBox.getItems().clear();
            mapComboBox.setValue(null);

        } finally {

            updatingMap = false;
        }

        startButton.setDisable(true);
        mapComboBox.setDisable(true);
        leaveButton.setDisable(false);
    }
}