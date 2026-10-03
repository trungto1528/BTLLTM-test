package com.test.game;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.common.map.MapInfo;

import javafx.application.Platform;

public class GameAppRooms {

    private static final String MAP_API_URL =
            "http://localhost:8080/api/maps";

    private final GameApp app;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public GameAppRooms(
            GameApp app) {

        this.app = app;
    }

    // =====================================================
    // SHOW LOBBY
    // =====================================================

    public void showLobby(
            String roomId,
            boolean host) {

        app.setCurrentRoomId(
                roomId);

        app.setCurrentHost(
                host);

        app.getLobbyView().setRoomId(
                roomId);

        app.getLobbyView().setHost(
                host);

        app.getScene().setRoot(
                app.getLobbyView());

        loadMaps();

        app.getLobbyView().requestFocus();
    }

    // =====================================================
    // LOAD MAPS
    // =====================================================

    private void loadMaps() {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        MAP_API_URL))
                        .GET()
                        .build();

        httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString())
                .thenAccept(
                        response -> {

                            if (response.statusCode()
                                    != 200) {

                                Platform.runLater(
                                        () -> app
                                                .getLobbyView()
                                                .setError(
                                                        "Cannot load maps: HTTP "
                                                                + response.statusCode()));

                                return;
                            }

                            try {

                                MapInfo[] maps =
                                        objectMapper.readValue(
                                                response.body(),
                                                MapInfo[].class);

                                List<MapInfo> mapList =
                                        Arrays.asList(
                                                maps);

                                Platform.runLater(
                                        () -> {

                                            app.getLobbyView()
                                                    .setMaps(
                                                            mapList);

                                            app.getLobbyView()
                                                    .setMap(
                                                            app.getCurrentMapId());

                                            app.getLobbyView()
                                                    .setHost(
                                                            app.isCurrentHost());
                                        });

                            } catch (
                                    JsonProcessingException exception) {

                                Platform.runLater(
                                        () -> app
                                                .getLobbyView()
                                                .setError(
                                                        "Cannot read map list"));
                            }
                        })
                .exceptionally(
                        exception -> {

                            Platform.runLater(
                                    () -> app
                                            .getLobbyView()
                                            .setError(
                                                    "Cannot connect to map server"));

                            return null;
                        });
    }

    // =====================================================
    // FIND ROOMS
    // =====================================================

    public void findRooms() {

        app.getNetwork().send(
                "FIND_ROOMS");
    }

    // =====================================================
    // CREATE ROOM
    // =====================================================

    public void createRoom() {

        if (!hasPlayerName()) {
            return;
        }

        app.getNetwork().send(
                "CREATE_ROOM");
    }

    // =====================================================
    // CHANGE MAP
    // =====================================================

    public void changeMap(
            String mapId) {

        if (!app.isCurrentHost()) {
            return;
        }

        if (app.getCurrentRoomId() == null
                || app.getCurrentRoomId().isBlank()) {

            return;
        }

        if (mapId == null
                || mapId.isBlank()) {

            return;
        }

        app.getNetwork().send(
                "CHANGE_MAP|"
                        + mapId);
    }

    // =====================================================
    // JOIN ROOM
    // =====================================================

    public void joinRoom(
            String roomId) {

        if (!hasPlayerName()) {
            return;
        }

        if (roomId == null
                || roomId.isBlank()) {

            return;
        }

        String normalizedRoomId =
                roomId.trim()
                        .toUpperCase();

        app.getNetwork().send(
                "JOIN_ROOM|"
                        + normalizedRoomId);
    }

    // =====================================================
    // START GAME
    // =====================================================

    public void startGame() {

        app.getNetwork().send(
                "START_GAME");

        app.getLobbyView()
                .setStartingStatus();
    }

    // =====================================================
    // LEAVE ROOM
    // =====================================================

    public void leaveRoom() {

        app.getNetwork().send(
                "LEAVE_ROOM");

        app.setCurrentRoomId(
                null);

        app.setCurrentMapId(
                null);

        app.setCurrentHost(
                false);

        app.getPlayerDirectory()
                .clear();

        app.showMainMenu();
    }

    // =====================================================
    // CHECK PLAYER NAME
    // =====================================================

    private boolean hasPlayerName() {

        String name =
                app.getCurrentPlayerName();

        if (name == null
                || name.isBlank()) {

            app.getMainMenu().showNameError(
                    "Please enter your name first.");

            app.getMainMenu()
                    .focusNameField();

            return false;
        }

        return true;
    }
}