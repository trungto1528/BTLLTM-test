package com.test;

import com.test.core.GameScene;
import com.test.core.GameWebSocketClient;
import com.test.ui.CreateRoomView;
import com.test.ui.FindRoomView;
import com.test.ui.JoinRoomView;
import com.test.ui.LobbyView;
import com.test.ui.MainMenuView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameApp extends Application {

    private Stage stage;

    /*
     * Chỉ sử dụng MỘT Scene duy nhất.
     *
     * Các màn hình sẽ được thay bằng:
     *
     * scene.setRoot(...)
     */
    private Scene scene;

    private GameWebSocketClient network;

    private MainMenuView mainMenu;
    private CreateRoomView createRoom;
    private JoinRoomView joinRoom;
    private FindRoomView findRoom;
    private LobbyView lobby;

    /*
     * GameScene chỉ được tạo khi server gửi
     * GAME_STARTED.
     */
    private GameScene gameScene;

    /*
     * Player ID do server cấp.
     */
    private String localPlayerId;

    /*
     * Room hiện tại.
     */
    private String currentRoomId;

    /*
     * Có phải host hay không.
     */
    private boolean currentHost;

    // =====================================================
    // START
    // =====================================================

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        stage.setTitle(
                "Jump King Multiplayer");

        stage.setWidth(1000);
        stage.setHeight(700);
        stage.setResizable(false);

        // =================================================
        // NETWORK
        // =================================================

        network =
                new GameWebSocketClient();

        /*
         * Tất cả message lobby / lifecycle
         * sẽ đi vào đây.
         */
        network.setMessageHandler(
                this::handleServerMessage);

        /*
         * Connect một lần duy nhất khi
         * application start.
         *
         * Không connect lại mỗi khi
         * chuyển scene.
         */
        network.connect();

        // =================================================
        // UI
        // =================================================

        mainMenu =
                new MainMenuView(this);

        createRoom =
                new CreateRoomView(this);

        joinRoom =
                new JoinRoomView(this);

        findRoom =
                new FindRoomView(this);

        lobby =
                new LobbyView(this);

        // =================================================
        // SINGLE SCENE
        // =================================================

        scene =
                new Scene(mainMenu);

        stage.setScene(scene);

        stage.show();

        mainMenu.requestFocus();
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

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public boolean isCurrentHost() {
        return currentHost;
    }

    // =====================================================
    // MAIN MENU
    // =====================================================

    public void showMainMenu() {

        if (scene == null) {
            return;
        }

        scene.setRoot(mainMenu);

        mainMenu.requestFocus();
    }

    // =====================================================
    // CREATE ROOM
    // =====================================================

    public void showCreateRoom() {

        createRoom.reset();

        scene.setRoot(createRoom);

        createRoom.requestFocus();
    }

    // =====================================================
    // JOIN ROOM
    // =====================================================

    public void showJoinRoom() {

        joinRoom.reset();

        scene.setRoot(joinRoom);

        joinRoom.requestFocus();
    }

    // =====================================================
    // FIND ROOM
    // =====================================================

    public void showFindRoom() {

        scene.setRoot(findRoom);

        findRoom.onShow();

        findRoom.requestFocus();
    }

    // =====================================================
    // LOBBY
    // =====================================================

    public void showLobby(
            String roomId,
            boolean host) {

        currentRoomId =
                roomId;

        currentHost =
                host;

        lobby.setRoomId(
                roomId);

        lobby.setHost(
                host);

        scene.setRoot(lobby);

        lobby.requestFocus();
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

        /*
         * Server chịu trách nhiệm tạo roomId.
         */
        network.send(
                "CREATE_ROOM");
    }

    // =====================================================
    // JOIN
    // =====================================================

    public void joinRoom(
            String roomId) {

        if (roomId == null
                || roomId.isBlank()) {

            return;
        }

        String normalizedRoomId =
                roomId.trim()
                        .toUpperCase();

        network.send(
                "JOIN_ROOM|"
                        + normalizedRoomId);
    }

    // =====================================================
    // START GAME
    // =====================================================

    public void startGame() {

        /*
         * Client KHÔNG tự chuyển sang GameScene.
         *
         * Chỉ host được phép gửi START_GAME.
         *
         * Sau đó chờ:
         *
         * GAME_STARTED|ABCDE
         *
         * từ server.
         */
        network.send(
                "START_GAME");

        /*
         * UI hiển thị trạng thái
         * đang bắt đầu game.
         */
        lobby.setStartingStatus();
    }

    // =====================================================
    // LEAVE ROOM
    // =====================================================

    public void leaveRoom() {

        network.send(
                "LEAVE_ROOM");

        currentRoomId =
                null;

        currentHost =
                false;

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

        // =================================================
        // WELCOME
        // =================================================

        if (message.startsWith(
                "WELCOME|")) {

            handleWelcome(
                    message);

            return;
        }

        // =================================================
        // ROOM CREATED
        // =================================================

        if (message.startsWith(
                "ROOM_CREATED|")) {

            handleRoomCreated(
                    message);

            return;
        }

        // =================================================
        // ROOM JOINED
        // =================================================

        if (message.startsWith(
                "ROOM_JOINED|")) {

            handleRoomJoined(
                    message);

            return;
        }

        // =================================================
        // ROOM STATE
        // =================================================

        if (message.startsWith(
                "ROOM_STATE|")) {

            handleRoomState(
                    message);

            return;
        }

        // =================================================
        // ROOM LIST
        // =================================================

        if (message.startsWith(
                "ROOM_LIST|")) {

            handleRoomList(
                    message);

            return;
        }

        // =================================================
        // ROOM LIST END
        // =================================================

        if ("ROOM_LIST_END".equals(message)) {

            Platform.runLater(
                    () -> findRoom.finishLoading());

            return;
        }

        // =================================================
        // ROOM ERROR
        // =================================================

        if (message.startsWith(
                "ROOM_ERROR|")) {

            handleRoomError(
                    message);

            return;
        }

        // =================================================
        // ROOM LEFT
        // =================================================

        if (message.startsWith(
                "ROOM_LEFT|")) {

            handleRoomLeft(
                    message);

            return;
        }

        // =================================================
        // GAME STARTED
        // =================================================

        if (message.startsWith(
                "GAME_STARTED|")) {

            handleGameStarted(
                    message);

            return;
        }

        // =================================================
        // CONNECTION ERROR
        // =================================================

        if (message.startsWith(
                "CONNECTION_ERROR|")) {

            System.err.println(
                    message);

            return;
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

        localPlayerId =
                playerId;

        System.out.println(
                "Local player ID: "
                        + localPlayerId);
    }

    // =====================================================
    // ROOM CREATED
    // =====================================================

    private void handleRoomCreated(
            String message) {

        String roomId =
                message.substring(
                        "ROOM_CREATED|".length());

        currentRoomId =
                roomId;

        currentHost =
                true;

        showLobby(
                roomId,
                true);
    }

    // =====================================================
    // ROOM JOINED
    // =====================================================

    private void handleRoomJoined(
            String message) {

        String roomId =
                message.substring(
                        "ROOM_JOINED|".length());

        currentRoomId =
                roomId;

        /*
         * Chưa tự đoán host.
         *
         * ROOM_STATE sẽ cung cấp HOST_ID.
         */
        currentHost =
                false;

        showLobby(
                roomId,
                false);
    }

    // =====================================================
    // ROOM STATE
    // =====================================================

    private void handleRoomState(
            String message) {

        /*
         * Protocol:
         *
         * ROOM_STATE|roomId|count|max|hostId
         */

        String[] parts =
                message.split("\\|");

        if (parts.length < 5) {

            System.err.println(
                    "Invalid ROOM_STATE: "
                            + message);

            return;
        }

        String roomId =
                parts[1];

        int playerCount;

        int maxPlayers;

        String hostId =
                parts[4];

        try {

            playerCount =
                    Integer.parseInt(
                            parts[2]);

            maxPlayers =
                    Integer.parseInt(
                            parts[3]);

        } catch (NumberFormatException e) {

            System.err.println(
                    "Invalid ROOM_STATE numbers: "
                            + message);

            return;
        }

        /*
         * Nếu đang ở một room khác thì
         * không nhận ROOM_STATE đó.
         */
        if (currentRoomId != null
                && !currentRoomId.equals(roomId)) {

            return;
        }

        currentRoomId =
                roomId;

        currentHost =
                localPlayerId != null
                        && localPlayerId.equals(hostId);

        /*
         * Nếu chưa ở Lobby thì vào Lobby.
         */
        if (!isLobbyShowing()) {

            final String finalRoomId =
                    roomId;

            final int finalPlayerCount =
                    playerCount;

            final int finalMaxPlayers =
                    maxPlayers;

            final boolean finalHost =
                    currentHost;

            Platform.runLater(
                    () -> {

                        showLobby(
                                finalRoomId,
                                finalHost);

                        lobby.setPlayers(
                                finalPlayerCount,
                                finalMaxPlayers);

                        lobby.setHost(
                                finalHost);
                    });

        } else {

            final int finalPlayerCount =
                    playerCount;

            final int finalMaxPlayers =
                    maxPlayers;

            final boolean finalHost =
                    currentHost;

            Platform.runLater(
                    () -> {

                        lobby.setRoomId(
                                roomId);

                        lobby.setPlayers(
                                finalPlayerCount,
                                finalMaxPlayers);

                        lobby.setHost(
                                finalHost);
                    });
        }
    }

    // =====================================================
    // ROOM LIST
    // =====================================================

    private void handleRoomList(
            String message) {

        /*
         * ROOM_LIST|EMPTY
         */
        if ("ROOM_LIST|EMPTY".equals(message)) {

            Platform.runLater(
                    () -> findRoom.showEmpty());

            return;
        }

        /*
         * Protocol:
         *
         * ROOM_LIST|roomId|currentPlayers|maxPlayers|OPEN
         */
        String[] parts =
                message.split("\\|");

        if (parts.length < 5) {

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

        } catch (NumberFormatException e) {

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

        String error =
                message.substring(
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

    private void handleRoomLeft(
            String message) {

        currentRoomId =
                null;

        currentHost =
                false;

        Platform.runLater(
                this::showMainMenu);
    }

    // =====================================================
    // GAME STARTED
    // =====================================================

    private void handleGameStarted(
            String message) {

        /*
         * Protocol:
         *
         * GAME_STARTED|ABCDE
         */

        String roomId =
                message.substring(
                        "GAME_STARTED|".length());

        /*
         * Chỉ chuyển game nếu đúng room
         * hiện tại.
         */
        if (currentRoomId == null
                || !currentRoomId.equals(roomId)) {

            return;
        }

        System.out.println(
                "Game started: "
                        + roomId);

        Platform.runLater(
                this::openGameScene);
    }

    // =====================================================
    // OPEN GAME
    // =====================================================

    private void openGameScene() {

        /*
         * Tránh tạo GameScene nhiều lần nếu
         * server gửi GAME_STARTED nhiều hơn một lần.
         */
        if (gameScene != null) {

            return;
        }

        /*
         * Constructor hiện tại của GameScene:
         *
         * GameScene(GameWebSocketClient network)
         */
        gameScene =
                new GameScene(
                        network);

        /*
         * =================================================
         * QUAN TRỌNG
         * =================================================
         *
         * GameApp đã nhận WELCOME trước đó:
         *
         * WELCOME|playerId
         *
         * Nhưng GameScene được tạo SAU KHI
         * GAME_STARTED.
         *
         * Vì vậy phải truyền playerId vào GameScene
         * tại đây.
         *
         * Nếu thiếu dòng này:
         *
         * localPlayerId == null
         *
         * => player của chính mình trong WORLD_STATE
         * bị coi là REMOTE PLAYER
         *
         * => xuất hiện player ảo / phân thân.
         */
        if (localPlayerId != null) {

            gameScene.setLocalPlayerId(
                    localPlayerId);
        }

        /*
         * GameWebSocketClient cần biết
         * GameScene hiện tại để chuyển
         * WORLD_STATE / PLAYER_STATE vào game.
         */
        network.setGameScene(
                gameScene);

        /*
         * Start game loop hiện tại.
         *
         * KHÔNG thay đổi gameplay.
         */
        gameScene.startLoop();

        /*
         * Dùng Scene hiện tại, không tạo Scene mới.
         */
        scene.setRoot(
                gameScene);

        /*
         * Nhận keyboard input.
         */
        gameScene.requestFocus();
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

        /*
         * Không gửi LEAVE_ROOM ở đây.
         *
         * Khi application đóng,
         * WebSocket disconnect sẽ khiến server
         * tự cleanup PlayerSession / Room.
         */
    }

    // =====================================================
    // MAIN
    // =====================================================

    public static void main(
            String[] args) {

        launch(args);
    }
}