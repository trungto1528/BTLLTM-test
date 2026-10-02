package com.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.common.map.MapInfo;
import com.test.core.GameWebSocketClient;
import com.test.core.PlayerDirectory;
import com.test.core.scene.GameScene;
import com.test.ui.CreateRoomView;
import com.test.ui.FindRoomView;
import com.test.ui.JoinRoomView;
import com.test.ui.LobbyView;
import com.test.ui.MainMenuView;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameApp extends Application {

        private static final String MAP_API_URL = "http://localhost:8080/api/maps";

        private final HttpClient httpClient = HttpClient.newHttpClient();

        private final ObjectMapper objectMapper = new ObjectMapper();

        private final PlayerDirectory playerDirectory = new PlayerDirectory();

        private Stage stage;

        private Scene scene;

        private GameWebSocketClient network;

        private MainMenuView mainMenu;

        private CreateRoomView createRoom;

        private JoinRoomView joinRoom;

        private FindRoomView findRoom;

        private LobbyView lobby;

        private GameScene gameScene;

        private StackPane gameContainer;

        private Label departureToast;

        private PauseTransition departureToastTimer;

        private String localPlayerId;

        private String currentPlayerName;

        private String currentRoomId;

        private String currentMapId;

        private boolean currentHost;

        // =====================================================
        // START
        // =====================================================

        @Override
        public void start(
                        Stage stage) {

                this.stage = stage;

                stage.setTitle(
                                "Jump King Multiplayer");

                stage.setWidth(
                                1000);

                stage.setHeight(
                                700);

                stage.setResizable(
                                false);

                // =================================================
                // NETWORK
                // =================================================

                network = new GameWebSocketClient();

                network.setMessageHandler(
                                this::handleServerMessage);

                network.connect();

                // =================================================
                // UI
                // =================================================

                mainMenu = new MainMenuView(
                                this);

                createRoom = new CreateRoomView(
                                this);

                joinRoom = new JoinRoomView(
                                this);

                findRoom = new FindRoomView(
                                this);

                lobby = new LobbyView(
                                this);

                // =================================================
                // SINGLE SCENE
                // =================================================

                scene = new Scene(
                                mainMenu);

                stage.setScene(
                                scene);

                stage.show();

                mainMenu.focusNameField();
        }

        // =====================================================
        // GETTERS
        // =====================================================

        public Stage getStage() {
                return stage;
        }

        public GameWebSocketClient getNetwork() {
                return network;
        }

        public String getLocalPlayerId() {
                return localPlayerId;
        }

        public String getCurrentPlayerName() {
                return currentPlayerName;
        }

        public String getCurrentRoomId() {
                return currentRoomId;
        }

        public String getCurrentMapId() {
                return currentMapId;
        }

        public boolean isCurrentHost() {
                return currentHost;
        }

        public PlayerDirectory getPlayerDirectory() {
                return playerDirectory;
        }

        // =====================================================
        // PLAYER NAME
        // =====================================================

        public void submitPlayerName(
                        String name) {

                if (name == null) {
                        return;
                }

                String normalizedName = name.trim();

                if (normalizedName.isBlank()) {
                        mainMenu.showNameError(
                                        "Please enter your name.");

                        return;
                }

                /*
                 * Không cho đổi tên khi đang ở room.
                 *
                 * Server cũng kiểm tra điều này.
                 * Check ở client để tránh gửi request
                 * không hợp lệ.
                 */
                if (currentRoomId != null
                                && !currentRoomId.isBlank()) {

                        mainMenu.showNameError(
                                        "You cannot change your name while in a room.");

                        return;
                }

                network.send(
                                "SET_NAME|"
                                                + normalizedName);
        }

        // =====================================================
        // SHOW MAIN MENU
        // =====================================================

        public void showMainMenu() {

                if (scene == null) {
                        return;
                }

                mainMenu.setPlayerName(
                                currentPlayerName);

                scene.setRoot(
                                mainMenu);

                mainMenu.focusNameField();
        }

        // =====================================================
        // CREATE ROOM
        // =====================================================

        public void showCreateRoom() {

                if (!hasPlayerName()) {
                        return;
                }

                createRoom.reset();

                scene.setRoot(
                                createRoom);

                createRoom.requestFocus();
        }

        // =====================================================
        // JOIN ROOM
        // =====================================================

        public void showJoinRoom() {

                if (!hasPlayerName()) {
                        return;
                }

                joinRoom.reset();

                scene.setRoot(
                                joinRoom);

                joinRoom.requestFocus();
        }

        // =====================================================
        // FIND ROOM
        // =====================================================

        public void showFindRoom() {

                if (!hasPlayerName()) {
                        return;
                }

                scene.setRoot(
                                findRoom);

                findRoom.onShow();

                findRoom.requestFocus();
        }

        // =====================================================
        // CHECK PLAYER NAME
        // =====================================================

        private boolean hasPlayerName() {

                if (currentPlayerName == null
                                || currentPlayerName.isBlank()) {

                        mainMenu.showNameError(
                                        "Please enter your name first.");

                        mainMenu.focusNameField();

                        return false;
                }

                return true;
        }

        // =====================================================
        // LOBBY
        // =====================================================

        public void showLobby(
                        String roomId,
                        boolean host) {

                currentRoomId = roomId;

                currentHost = host;

                lobby.setRoomId(
                                roomId);

                lobby.setHost(
                                host);

                scene.setRoot(
                                lobby);

                /*
                 * Lobby cần danh sách map.
                 *
                 * Gọi REST API ngay khi vào Lobby.
                 */
                loadMaps();

                lobby.requestFocus();
        }

        // =====================================================
        // LOAD MAPS
        // =====================================================

        private void loadMaps() {

                HttpRequest request = HttpRequest.newBuilder()
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

                                                        if (response.statusCode() != 200) {

                                                                Platform.runLater(
                                                                                () -> lobby.setError(
                                                                                                "Cannot load maps: HTTP "
                                                                                                                + response.statusCode()));

                                                                return;
                                                        }

                                                        try {

                                                                MapInfo[] maps = objectMapper.readValue(
                                                                                response.body(),
                                                                                MapInfo[].class);

                                                                List<MapInfo> mapList = Arrays.asList(
                                                                                maps);

                                                                Platform.runLater(
                                                                                () -> {

                                                                                        lobby.setMaps(
                                                                                                        mapList);

                                                                                        lobby.setMap(
                                                                                                        currentMapId);

                                                                                        lobby.setHost(
                                                                                                        currentHost);
                                                                                });

                                                        } catch (JsonProcessingException exception) {

                                                                Platform.runLater(
                                                                                () -> lobby.setError(
                                                                                                "Cannot read map list"));
                                                        }
                                                })
                                .exceptionally(
                                                exception -> {

                                                        Platform.runLater(
                                                                        () -> lobby.setError(
                                                                                        "Cannot connect to map server"));

                                                        return null;
                                                });
        }

        // =====================================================
        // FIND ROOMS
        // =====================================================

        public void findRooms() {

                network.send(
                                "FIND_ROOMS");
        }

        // =====================================================
        // CREATE
        // =====================================================

        public void createRoom() {

                if (!hasPlayerName()) {
                        return;
                }

                network.send(
                                "CREATE_ROOM");
        }

        // =====================================================
        // CHANGE MAP
        // =====================================================

        public void changeMap(
                        String mapId) {

                if (!currentHost) {
                        return;
                }

                if (currentRoomId == null
                                || currentRoomId.isBlank()) {

                        return;
                }

                if (mapId == null
                                || mapId.isBlank()) {

                        return;
                }

                network.send(
                                "CHANGE_MAP|"
                                                + mapId);
        }

        // =====================================================
        // JOIN
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

                String normalizedRoomId = roomId.trim()
                                .toUpperCase();

                network.send(
                                "JOIN_ROOM|"
                                                + normalizedRoomId);
        }

        // =====================================================
        // START GAME
        // =====================================================

        public void startGame() {

                network.send(
                                "START_GAME");

                lobby.setStartingStatus();
        }

        // =====================================================
        // LEAVE ROOM
        // =====================================================

        public void leaveRoom() {

                network.send(
                                "LEAVE_ROOM");

                currentRoomId = null;

                currentMapId = null;

                currentHost = false;

                playerDirectory.clear();

                showMainMenu();
        }

        // =====================================================
        // SERVER MESSAGE
        // =====================================================

        private void handleServerMessage(
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
                                        () -> findRoom.finishLoading());

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

                localPlayerId = message.substring(
                                "WELCOME|".length());

                System.out.println(
                                "Local player ID: "
                                                + localPlayerId);

                Platform.runLater(
                                () -> {

                                        currentPlayerName = null;

                                        playerDirectory.clear();

                                        showMainMenu();
                                });
        }

        // =====================================================
        // NAME SET
        // =====================================================

        private void handleNameSet(
                        String message) {

                String name = message.substring(
                                "NAME_SET|".length())
                                .trim();

                currentPlayerName = name;

                if (localPlayerId != null
                                && !localPlayerId.isBlank()) {

                        playerDirectory.setName(
                                        localPlayerId,
                                        name);
                }

                Platform.runLater(
                                () -> {

                                        mainMenu.hideNameError();

                                        mainMenu.setPlayerName(
                                                        currentPlayerName);

                                        /*
                                         * Nếu đang ở Home thì chỉ cập nhật
                                         * ô tên, không cần chuyển scene.
                                         *
                                         * showMainMenu() vẫn an toàn nếu
                                         * NAME_SET đến từ một trạng thái
                                         * Home khác.
                                         */
                                        showMainMenu();
                                });
        }

        // =====================================================
        // NAME ERROR
        // =====================================================

        private void handleNameError(
                        String message) {

                String error = message.substring(
                                "NAME_ERROR|".length());

                Platform.runLater(
                                () -> {

                                        /*
                                         * NAME_ERROR hiện được hiển thị
                                         * trực tiếp trên Home.
                                         */
                                        mainMenu.showNameError(
                                                        error);

                                        mainMenu.focusNameField();
                                });
        }

        // =====================================================
        // ROOM CREATED
        // =====================================================

        private void handleRoomCreated(
                        String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 3) {

                        System.err.println(
                                        "Invalid ROOM_CREATED: "
                                                        + message);

                        return;
                }

                currentRoomId = parts[1];

                currentMapId = parts[2];

                currentHost = true;

                showLobby(
                                currentRoomId,
                                true);
        }

        // =====================================================
        // ROOM JOINED
        // =====================================================

        private void handleRoomJoined(
                        String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 3) {

                        System.err.println(
                                        "Invalid ROOM_JOINED: "
                                                        + message);

                        return;
                }

                currentRoomId = parts[1];

                currentMapId = parts[2];

                currentHost = false;

                showLobby(
                                currentRoomId,
                                false);
        }

        // =====================================================
        // ROOM STATE
        // =====================================================

        private void handleRoomState(
                        String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 6) {

                        System.err.println(
                                        "Invalid ROOM_STATE: "
                                                        + message);

                        return;
                }

                String roomId = parts[1];

                int maxPlayers;

                String hostId = parts[4];

                String mapId = parts[5];

                try {

                        Integer.valueOf(
                                        parts[2]);

                        maxPlayers = Integer.parseInt(
                                        parts[3]);

                } catch (NumberFormatException exception) {

                        System.err.println(
                                        "Invalid ROOM_STATE numbers: "
                                                        + message);

                        return;
                }

                if (currentRoomId != null
                                && !currentRoomId.equals(
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
                 * |PLAYER
                 * |playerId
                 * |displayName
                 * ...
                 */

                List<String> playerIds = new ArrayList<>();

                int index = 6;

                while (index + 2 < parts.length) {

                        if (!"PLAYER".equals(
                                        parts[index])) {

                                break;
                        }

                        String playerId = parts[index + 1];

                        String displayName = parts[index + 2];

                        if (playerId != null
                                        && !playerId.isBlank()) {

                                playerIds.add(
                                                playerId);

                                if (displayName != null
                                                && !displayName.isBlank()) {

                                        playerDirectory.setName(
                                                        playerId,
                                                        displayName);
                                }
                        }

                        index += 3;
                }

                currentRoomId = roomId;

                currentMapId = mapId;

                currentHost = localPlayerId != null
                                && localPlayerId.equals(
                                                hostId);

                final String finalRoomId = roomId;

                final String finalMapId = mapId;

                final int finalMaxPlayers = maxPlayers;

                final String finalHostId = hostId;

                final boolean finalHost = currentHost;

                final List<String> finalPlayerIds = new ArrayList<>(
                                playerIds);

                /*
                 * Nếu đang ở game thì vẫn phải xử lý
                 * PLAYER_DIRECTORY ở phía trên, nhưng
                 * không được chuyển GameScene về Lobby.
                 */
                if (gameScene != null
                                && scene.getRoot() == gameContainer) {

                        return;
                }

                Platform.runLater(
                                () -> {

                                        currentMapId = finalMapId;

                                        showLobby(
                                                        finalRoomId,
                                                        finalHost);

                                        lobby.setPlayers(
                                                        finalPlayerIds,
                                                        finalHostId,
                                                        finalMaxPlayers,
                                                        playerDirectory);

                                        lobby.setMap(
                                                        finalMapId);

                                        lobby.setHost(
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
                                        () -> findRoom.showEmpty());

                        return;
                }

                String[] parts = message.split("\\|");

                if (parts.length < 6) {

                        System.err.println(
                                        "Invalid ROOM_LIST: "
                                                        + message);

                        return;
                }

                String roomId = parts[1];

                int currentPlayers;

                int maxPlayers;

                try {

                        currentPlayers = Integer.parseInt(
                                        parts[2]);

                        maxPlayers = Integer.parseInt(
                                        parts[3]);

                } catch (NumberFormatException exception) {

                        System.err.println(
                                        "Invalid ROOM_LIST numbers: "
                                                        + message);

                        return;
                }

                final int finalCurrentPlayers = currentPlayers;

                final int finalMaxPlayers = maxPlayers;

                Platform.runLater(
                                () -> findRoom.addRoom(
                                                roomId,
                                                finalCurrentPlayers,
                                                finalMaxPlayers));
        }

        // =====================================================
        // ROOM ERROR
        // =====================================================

        private void handleRoomError(
                        String message) {

                String error = message.substring(
                                "ROOM_ERROR|".length());

                System.err.println(
                                "Room error: "
                                                + error);

                Platform.runLater(
                                () -> {

                                        joinRoom.setError(
                                                        error);

                                        if (isLobbyShowing()) {

                                                lobby.setError(
                                                                error);
                                        }

                                        findRoom.setError(
                                                        error);
                                });
        }

        // =====================================================
        // ROOM LEFT
        // =====================================================

        private void handleRoomLeft() {

                currentRoomId = null;

                currentMapId = null;

                currentHost = false;

                gameScene = null;

                gameContainer = null;

                playerDirectory.clear();

                hideDepartureToast();

                network.setGameScene(
                                null);

                Platform.runLater(
                                this::showMainMenu);
        }

        // =====================================================
        // PLAYER LEFT
        // =====================================================

        private void handlePlayerLeft(
                        String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 5) {
                        return;
                }

                String roomId = parts[1];

                String playerId = parts[2];

                boolean wasHost = Boolean.parseBoolean(
                                parts[3]);

                String newHostId = parts[4];

                if (currentRoomId == null
                                || !currentRoomId.equals(
                                                roomId)) {

                        return;
                }

                if (gameScene != null
                                && scene.getRoot() == gameContainer) {

                        currentHost = localPlayerId != null
                                        && localPlayerId.equals(
                                                        newHostId);

                        String playerName = playerDirectory
                                        .getNameOrFallback(
                                                        playerId);

                        final String notification;

                        if (wasHost) {

                                notification = "Host "
                                                + playerName
                                                + " đã rời trận. "
                                                + "Host mới: "
                                                + playerDirectory
                                                                .getNameOrFallback(
                                                                                newHostId);

                        } else {

                                notification = "Người chơi "
                                                + playerName
                                                + " đã rời trận.";
                        }

                        showDepartureToast(
                                        notification);

                        /*
                         * Không remove playerId khỏi
                         * PlayerDirectory ở đây.
                         *
                         * GameScene vẫn cần tên của player
                         * đã disconnect để hiển thị:
                         *
                         * Name
                         * Disconnect icon
                         * Player
                         */
                        return;
                }

                currentHost = localPlayerId != null
                                && localPlayerId.equals(
                                                newHostId);
        }

        // =====================================================
        // GAME STARTED
        // =====================================================

        private void handleGameStarted(
                        String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 3) {
                        return;
                }

                String roomId = parts[1];

                String mapId = parts[2];

                if (currentRoomId == null
                                || !currentRoomId.equals(
                                                roomId)) {

                        return;
                }

                currentMapId = mapId;

                Platform.runLater(
                                this::openGameScene);
        }

        // =====================================================
        // OPEN GAME
        // =====================================================

        private void openGameScene() {

                if (gameScene != null) {
                        return;
                }

                if (currentMapId == null
                                || currentMapId.isBlank()) {

                        System.err.println(
                                        "Cannot open game: mapId is missing");

                        return;
                }

                gameScene = new GameScene(
                                network,
                                currentMapId,
                                playerDirectory);

                if (localPlayerId != null) {

                        gameScene.setLocalPlayerId(
                                        localPlayerId);
                }

                network.setGameScene(
                                gameScene);

                gameContainer = new StackPane();

                gameContainer.setPrefSize(
                                800,
                                600);

                gameContainer.getChildren().add(
                                gameScene);

                departureToast = new Label();

                departureToast.setVisible(
                                false);

                departureToast.setManaged(
                                false);

                departureToast.setMouseTransparent(
                                true);

                departureToast.setWrapText(
                                true);

                departureToast.setMaxWidth(
                                420);

                departureToast.setAlignment(
                                Pos.CENTER_LEFT);

                departureToast.setStyle(
                                "-fx-background-color: rgba(25, 29, 36, 0.94);"
                                                + "-fx-text-fill: white;"
                                                + "-fx-padding: 10 14;"
                                                + "-fx-background-radius: 8;"
                                                + "-fx-border-color: rgba(255,255,255,0.18);"
                                                + "-fx-border-radius: 8;"
                                                + "-fx-font-size: 14px;");

                StackPane.setAlignment(
                                departureToast,
                                Pos.BOTTOM_LEFT);

                StackPane.setMargin(
                                departureToast,
                                new javafx.geometry.Insets(
                                                0,
                                                0,
                                                20,
                                                20));

                gameContainer.getChildren().add(
                                departureToast);

                departureToastTimer = new PauseTransition(
                                Duration.seconds(4));

                departureToastTimer.setOnFinished(
                                event -> hideDepartureToast());

                scene.setRoot(
                                gameContainer);

                gameScene.requestFocus();
        }

        // =====================================================
        // SHOW DEPARTURE TOAST
        // =====================================================

        private void showDepartureToast(
                        String message) {

                if (departureToast == null) {
                        return;
                }

                Platform.runLater(
                                () -> {

                                        departureToast.setText(
                                                        message);

                                        departureToast.setVisible(
                                                        true);

                                        departureToast.toFront();

                                        if (departureToastTimer != null) {

                                                departureToastTimer.playFromStart();
                                        }
                                });
        }

        // =====================================================
        // HIDE DEPARTURE TOAST
        // =====================================================

        private void hideDepartureToast() {

                if (departureToastTimer != null) {

                        departureToastTimer.stop();
                }

                if (departureToast != null) {

                        departureToast.setVisible(
                                        false);
                }
        }

        // =====================================================
        // CHECK CURRENT SCENE
        // =====================================================

        private boolean isLobbyShowing() {

                if (scene == null) {
                        return false;
                }

                return scene.getRoot() == lobby;
        }

        // =====================================================
        // STOP
        // =====================================================

        @Override
        public void stop() {
        }

        // =====================================================
        // MAIN
        // =====================================================

        public static void main(
                        String[] args) {

                launch(args);
        }
}