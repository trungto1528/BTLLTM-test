package com.test.ui;

import java.util.List;

import com.test.GameApp;
import com.test.common.map.MapInfo;
import com.test.core.PlayerDirectory;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LobbyView extends VBox {


    private final Label roomIdLabel;
    private final Label playersLabel;
    private final Label statusLabel;
    private final Label mapTitleLabel;

    private final ComboBox<MapInfo> mapComboBox;

    private final Button startButton;
    private final Button leaveButton;

    private final GridPane playerGrid;
    private final VBox[] playerCards;

    private boolean updatingMap;

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
        // PLAYERS TITLE
        // =================================================

        playersLabel =
                new Label("PLAYERS (0/4)");

        playersLabel.setFont(
                Font.font(20));

        playersLabel.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // PLAYER GRID
        // =================================================

        playerGrid =
                new GridPane();

        playerGrid.setHgap(14);
        playerGrid.setVgap(14);
        playerGrid.setAlignment(Pos.CENTER);

        playerCards =
                new VBox[4];

        for (int i = 0; i < 4; i++) {

            VBox card =
                    createPlayerCard();

            playerCards[i] =
                    card;

            int column =
                    i % 2;

            int row =
                    i / 2;

            playerGrid.add(
                    card,
                    column,
                    row);
        }

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
                playerGrid,
                mapTitleLabel,
                mapComboBox,
                statusLabel,
                startButton,
                leaveButton
        );

        clearPlayerCards();
    }

    // =====================================================
    // PLAYER CARD
    // =====================================================

    private VBox createPlayerCard() {

        Label nameLabel =
                new Label("EMPTY");

        nameLabel.setFont(
                Font.font(20));

        nameLabel.setTextFill(
                Color.WHITE);

        nameLabel.setAlignment(
                Pos.CENTER);

        nameLabel.setMaxWidth(
                Double.MAX_VALUE);

        Label hostLabel =
                new Label();

        hostLabel.setFont(
                Font.font(13));

        hostLabel.setTextFill(
                Color.GOLD);

        hostLabel.setAlignment(
                Pos.CENTER);

        hostLabel.setMaxWidth(
                Double.MAX_VALUE);

        VBox card =
                new VBox(
                        6,
                        nameLabel,
                        hostLabel);

        card.setAlignment(
                Pos.CENTER);

        card.setPrefSize(
                280,
                90);

        card.setMinSize(
                280,
                90);

        card.setMaxSize(
                280,
                90);

        card.setPadding(
                new Insets(10));

        card.setStyle(
                "-fx-background-color: #2c323b;"
                        + "-fx-border-color: #4b535e;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );

        return card;
    }

    // =====================================================
    // PLAYER CARD CONTENT
    // =====================================================

    private void setPlayerCard(
            VBox card,
            String name,
            boolean host) {

        Label nameLabel =
                (Label) card.getChildren().get(0);

        Label hostLabel =
                (Label) card.getChildren().get(1);

        if (name == null
                || name.isBlank()) {

            nameLabel.setText(
                    "EMPTY");

            hostLabel.setText("");

            card.setStyle(
                    "-fx-background-color: #252a31;"
                            + "-fx-border-color: #3a4048;"
                            + "-fx-border-width: 1;"
                            + "-fx-border-radius: 6;"
                            + "-fx-background-radius: 6;"
            );

            return;
        }

        nameLabel.setText(
                name);

        if (host) {

            hostLabel.setText(
                    "HOST");

        } else {

            hostLabel.setText("");
        }

        card.setStyle(
                "-fx-background-color: #2c323b;"
                        + "-fx-border-color: #59636f;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 6;"
                        + "-fx-background-radius: 6;"
        );
    }

    // =====================================================
    // CLEAR PLAYER CARDS
    // =====================================================

    private void clearPlayerCards() {

        for (VBox card :
                playerCards) {

            setPlayerCard(
                    card,
                    null,
                    false);
        }
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
    // PLAYERS WITH NAMES
    // =====================================================

    public void setPlayers(
            List<String> playerIds,
            String hostPlayerId,
            int maxPlayers,
            PlayerDirectory playerDirectory) {

        int current =
                playerIds == null
                        ? 0
                        : playerIds.size();

        playersLabel.setText(
                "PLAYERS ("
                        + current
                        + "/"
                        + maxPlayers
                        + ")");

        clearPlayerCards();

        if (playerIds == null) {
            return;
        }

        int visiblePlayers =
                Math.min(
                        playerIds.size(),
                        playerCards.length);

        for (int i = 0;
                i < visiblePlayers;
                i++) {

            String playerId =
                    playerIds.get(i);

            String name;

            if (playerDirectory == null) {

                name =
                        playerId;

            } else {

                name =
                        playerDirectory
                                .getNameOrFallback(
                                        playerId);
            }

            boolean host =
                    playerId != null
                            && playerId.equals(
                                    hostPlayerId);

            setPlayerCard(
                    playerCards[i],
                    name,
                    host);
        }
    }

    // =====================================================
    // MAP LIST
    // =====================================================

    public void setMaps(
            List<MapInfo> maps) {

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

        clearPlayerCards();

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