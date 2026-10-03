package com.test.game;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;

public class GameAppNetwork {

    private final GameApp app;

    public GameAppNetwork(
            GameApp app) {

        this.app = app;
    }

    // =====================================================
    // SERVER MESSAGE
    // =====================================================

    public void handleServerMessage(
            String message) {

        if (message == null
                || message.isBlank()) {

            return;
        }

        System.out.println(
                "[APP] "
                        + message);

        if (message.startsWith(
                "WELCOME|")) {

            handleWelcome(
                    message);

            return;
        }

        if (message.startsWith(
                "NAME_SET|")) {

            handleNameSet(
                    message);

            return;
        }

        if (message.startsWith(
                "NAME_ERROR|")) {

            handleNameError(
                    message);

            return;
        }

        if (message.startsWith(
                "ROOM_CREATED|")) {

            handleRoomCreated(
                    message);

            return;
        }

        if (message.startsWith(
                "ROOM_JOINED|")) {

            handleRoomJoined(
                    message);

            return;
        }

        if (message.startsWith(
                "ROOM_STATE|")) {

            handleRoomState(
                    message);

            return;
        }

        if (message.startsWith(
                "ROOM_LIST|")) {

            handleRoomList(
                    message);

            return;
        }

        if ("ROOM_LIST_END".equals(
                message)) {

            Platform.runLater(
                    () ->
                            app.getFindRoomView()
                                    .finishLoading());

            return;
        }

        if (message.startsWith(
                "ROOM_ERROR|")) {

            handleRoomError(
                    message);

            return;
        }

        if (message.startsWith(
                "ROOM_LEFT|")) {

            handleRoomLeft();

            return;
        }

        if (message.startsWith(
                "PLAYER_LEFT|")) {

            handlePlayerLeft(
                    message);

            return;
        }

        if (message.startsWith(
                "GAME_STARTED|")) {

            handleGameStarted(
                    message);

            return;
        }

        if (message.startsWith(
                "CONNECTION_ERROR|")) {

            System.err.println(
                    message);
        }
    }

    // =====================================================
    // WELCOME
    // =====================================================

    private void handleWelcome(
            String message) {

        String playerId =
                message.substring(
                        "WELCOME|".length());

        if (playerId.isBlank()) {
            return;
        }

        app.setLocalPlayerId(
                playerId);

        app.setCurrentPlayerName(
                null);

        app.setCurrentRoomId(
                null);

        app.setCurrentMapId(
                null);

        app.setCurrentHost(
                false);

        app.getPlayerDirectory()
                .clear();

        Platform.runLater(
                app::showMainMenu);
    }

    // =====================================================
    // NAME SET
    // =====================================================

    private void handleNameSet(
            String message) {

        String name =
                message.substring(
                        "NAME_SET|".length());

        app.setCurrentPlayerName(
                name);

        String localPlayerId =
                app.getLocalPlayerId();

        if (localPlayerId != null
                && !localPlayerId.isBlank()
                && name != null
                && !name.isBlank()) {

            app.getPlayerDirectory()
                    .setName(
                            localPlayerId,
                            name);
        }

        Platform.runLater(
                () -> {

                    app.getMainMenu()
                            .hideNameError();

                    app.getMainMenu()
                            .setPlayerName(
                                    name);

                    app.showMainMenu();
                });
    }

    // =====================================================
    // NAME ERROR
    // =====================================================

    private void handleNameError(
            String message) {

        String error =
                message.substring(
                        "NAME_ERROR|".length());

        Platform.runLater(
                () -> {

                    app.getMainMenu()
                            .showNameError(
                                    error);

                    app.getMainMenu()
                            .focusNameField();
                });
    }

    // =====================================================
    // ROOM CREATED
    // =====================================================

    private void handleRoomCreated(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 3) {

            System.err.println(
                    "Invalid ROOM_CREATED: "
                            + message);

            return;
        }

        String roomId =
                parts[1];

        String mapId =
                parts[2];

        app.setCurrentRoomId(
                roomId);

        app.setCurrentMapId(
                mapId);

        app.setCurrentHost(
                true);

        Platform.runLater(
                () ->
                        app.showLobby(
                                roomId,
                                true));
    }

    // =====================================================
    // ROOM JOINED
    // =====================================================

    private void handleRoomJoined(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 3) {

            System.err.println(
                    "Invalid ROOM_JOINED: "
                            + message);

            return;
        }

        String roomId =
                parts[1];

        String mapId =
                parts[2];

        app.setCurrentRoomId(
                roomId);

        app.setCurrentMapId(
                mapId);

        app.setCurrentHost(
                false);

        Platform.runLater(
                () ->
                        app.showLobby(
                                roomId,
                                false));
    }

    // =====================================================
    // ROOM STATE
    // =====================================================

    private void handleRoomState(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 6) {

            System.err.println(
                    "Invalid ROOM_STATE: "
                            + message);

            return;
        }

        String roomId =
                parts[1];

        int maxPlayers;

        String hostId =
                parts[4];

        String mapId =
                parts[5];

        try {

            Integer.valueOf(
                    parts[2]);

            maxPlayers =
                    Integer.parseInt(
                            parts[3]);

        } catch (
                NumberFormatException exception) {

            System.err.println(
                    "Invalid ROOM_STATE numbers: "
                            + message);

            return;
        }

        if (app.getCurrentRoomId() != null
                && !app.getCurrentRoomId().equals(
                        roomId)) {

            return;
        }

        /*
         * ROOM_STATE:
         *
         * ROOM_STATE
         * |roomId
         * |playerCount
         * |maxPlayers
         * |hostId
         * |mapId
         * |PLAYER
         * |playerId
         * |displayName
         * ...
         */

        List<String> playerIds =
                new ArrayList<>();

        int index = 6;

        while (index + 2 < parts.length) {

            if (!"PLAYER".equals(
                    parts[index])) {

                break;
            }

            String playerId =
                    parts[index + 1];

            String displayName =
                    parts[index + 2];

            if (playerId != null
                    && !playerId.isBlank()) {

                playerIds.add(
                        playerId);

                if (displayName != null
                        && !displayName.isBlank()) {

                    app.getPlayerDirectory()
                            .setName(
                                    playerId,
                                    displayName);
                }
            }

            index += 3;
        }

        app.setCurrentRoomId(
                roomId);

        app.setCurrentMapId(
                mapId);

        boolean currentHost =
                app.getLocalPlayerId() != null
                        && app.getLocalPlayerId()
                                .equals(hostId);

        app.setCurrentHost(
                currentHost);

        final String finalRoomId =
                roomId;

        final String finalMapId =
                mapId;

        final int finalMaxPlayers =
                maxPlayers;

        final String finalHostId =
                hostId;

        final boolean finalHost =
                currentHost;

        final List<String> finalPlayerIds =
                new ArrayList<>(
                        playerIds);

        /*
         * Nếu đang ở game thì vẫn xử lý
         * PlayerDirectory nhưng không
         * chuyển GameScene về Lobby.
         */
        if (app.getGameScene() != null
                && app.getScene().getRoot()
                        != app.getLobbyView()) {

            return;
        }

        Platform.runLater(
                () -> {

                    app.setCurrentMapId(
                            finalMapId);

                    app.showLobby(
                            finalRoomId,
                            finalHost);

                    app.getLobbyView()
                            .setPlayers(
                                    finalPlayerIds,
                                    finalHostId,
                                    finalMaxPlayers,
                                    app.getPlayerDirectory());

                    app.getLobbyView()
                            .setMap(
                                    finalMapId);

                    app.getLobbyView()
                            .setHost(
                                    finalHost);
                });
    }

    // =====================================================
    // ROOM LIST
    // =====================================================

    private void handleRoomList(
            String message) {

        if ("ROOM_LIST|EMPTY".equals(
                message)) {

            Platform.runLater(
                    () ->
                            app.getFindRoomView()
                                    .showEmpty());

            return;
        }

        String[] parts =
                message.split("\\|");

        if (parts.length < 6) {

            System.err.println(
                    "Invalid ROOM_LIST: "
                            + message);

            return;
        }

        String roomId =
                parts[1];

        int currentPlayers;
        int maxPlayers;

        try {

            currentPlayers =
                    Integer.parseInt(
                            parts[2]);

            maxPlayers =
                    Integer.parseInt(
                            parts[3]);

        } catch (
                NumberFormatException exception) {

            System.err.println(
                    "Invalid ROOM_LIST numbers: "
                            + message);

            return;
        }

        final int finalCurrentPlayers =
                currentPlayers;

        final int finalMaxPlayers =
                maxPlayers;

        Platform.runLater(
                () ->
                        app.getFindRoomView()
                                .addRoom(
                                        roomId,
                                        finalCurrentPlayers,
                                        finalMaxPlayers));
    }

    // =====================================================
    // ROOM ERROR
    // =====================================================

    private void handleRoomError(
            String message) {

        String error =
                message.substring(
                        "ROOM_ERROR|".length());

        System.err.println(
                "Room error: "
                        + error);

        Platform.runLater(
                () -> {

                    app.getJoinRoomView()
                            .setError(
                                    error);

                    if (app.isLobbyShowing()) {

                        app.getLobbyView()
                                .setError(
                                        error);
                    }

                    app.getFindRoomView()
                            .setError(
                                    error);
                });
    }

    // =====================================================
    // ROOM LEFT
    // =====================================================

    private void handleRoomLeft() {

        app.setCurrentRoomId(
                null);

        app.setCurrentMapId(
                null);

        app.setCurrentHost(
                false);

        app.getPlayerDirectory()
                .clear();

        app.hideDepartureToast();

        app.getNetwork()
                .setGameScene(
                        null);

        Platform.runLater(
                app::showMainMenu);
    }

    // =====================================================
    // PLAYER LEFT
    // =====================================================

    private void handlePlayerLeft(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 5) {
            return;
        }

        String roomId =
                parts[1];

        String playerId =
                parts[2];

        boolean wasHost =
                Boolean.parseBoolean(
                        parts[3]);

        String newHostId =
                parts[4];

        if (app.getCurrentRoomId() == null
                || !app.getCurrentRoomId().equals(
                        roomId)) {

            return;
        }

        if (app.getGameScene() != null
                && app.getScene().getRoot()
                        != app.getLobbyView()) {

            app.setCurrentHost(
                    app.getLocalPlayerId() != null
                            && app.getLocalPlayerId()
                                    .equals(newHostId));

            String playerName =
                    app.getPlayerDirectory()
                            .getNameOrFallback(
                                    playerId);

            final String notification;

            if (wasHost) {

                notification =
                        "Host "
                                + playerName
                                + " đã rời trận. "
                                + "Host mới: "
                                + app.getPlayerDirectory()
                                        .getNameOrFallback(
                                                newHostId);

            } else {

                notification =
                        "Người chơi "
                                + playerName
                                + " đã rời trận.";
            }

            app.showDepartureToast(
                    notification);

            /*
             * Không remove playerId khỏi
             * PlayerDirectory.
             *
             * GameScene vẫn cần tên của player
             * đã disconnect.
             */
            return;
        }

        app.setCurrentHost(
                app.getLocalPlayerId() != null
                        && app.getLocalPlayerId()
                                .equals(newHostId));
    }

    // =====================================================
    // GAME STARTED
    // =====================================================

    private void handleGameStarted(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 3) {
            return;
        }

        String roomId =
                parts[1];

        String mapId =
                parts[2];

        if (app.getCurrentRoomId() == null
                || !app.getCurrentRoomId().equals(
                        roomId)) {

            return;
        }

        app.setCurrentMapId(
                mapId);

        Platform.runLater(
                app::openGameScene);
    }
}