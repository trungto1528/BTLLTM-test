package com.test;

import com.test.core.GameScene;
import com.test.core.GameWebSocketClient;
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

    private Stage stage;

    /*
     * Chỉ sử dụng MỘT Scene duy nhất.
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
     * Container dùng khi đang chơi game.
     *
     * GameScene nằm dưới.
     * Toast thông báo nằm trên.
     */
    private StackPane gameContainer;

    /*
     * Toast thông báo người chơi rời trận.
     */
    private Label departureToast;

    /*
     * Timer tự ẩn toast.
     */
    private PauseTransition departureToastTimer;

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

        network.setMessageHandler(
                this::handleServerMessage);

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
         * Client không tự chuyển GameScene.
         *
         * Chờ GAME_STARTED từ server.
         */
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
        // PLAYER LEFT
        // =================================================

        if (message.startsWith(
                "PLAYER_LEFT|")) {

            handlePlayerLeft(
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
                        && localPlayerId.equals(
                                hostId);

        /*
         * QUAN TRỌNG:
         *
         * Nếu đang trong GameScene thì
         * KHÔNG được đưa client về Lobby.
         *
         * ROOM_STATE lúc này chỉ dùng để
         * cập nhật currentHost.
         */
        if (gameScene != null
                && scene.getRoot()
                        == gameContainer) {

            return;
        }

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

        /*
         * Chỉ client vừa gửi LEAVE_ROOM
         * mới nhận ROOM_LEFT.
         */
        currentRoomId =
                null;

        currentHost =
                false;

        gameScene =
                null;

        gameContainer =
                null;

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

        /*
         * Protocol:
         *
         * PLAYER_LEFT|
         * roomId|
         * playerId|
         * wasHost|
         * newHostId
         */

        String[] parts =
                message.split("\\|");

        if (parts.length < 5) {

            System.err.println(
                    "Invalid PLAYER_LEFT: "
                            + message);

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

        /*
         * =================================================
         * CHỈ XỬ LÝ NGƯỜI CÙNG ROOM
         * =================================================
         */
        if (currentRoomId == null
                || !currentRoomId.equals(roomId)) {

            return;
        }

        /*
         * Nếu đang trong game:
         *
         * - không về Lobby
         * - không dừng trận
         * - chỉ hiện thông báo
         */
        if (gameScene != null
                && scene.getRoot()
                        == gameContainer) {

            /*
             * Cập nhật host local.
             *
             * Nếu mình là host mới thì
             * currentHost = true.
             */
            currentHost =
                    localPlayerId != null
                            && localPlayerId.equals(
                                    newHostId);

            String playerName =
                    shortPlayerId(
                            playerId);

            final String notification;

            if (wasHost) {

                notification =
                        "Host "
                                + playerName
                                + " đã rời trận. "
                                + "Host mới: "
                                + shortPlayerId(
                                        newHostId);

            } else {

                notification =
                        "Người chơi "
                                + playerName
                                + " đã rời trận.";
            }

            showDepartureToast(
                    notification);

            return;
        }

        /*
         * Nếu chưa vào game thì chỉ cập nhật
         * host hiện tại.
         *
         * ROOM_STATE sẽ cập nhật số người.
         */
        currentHost =
                localPlayerId != null
                        && localPlayerId.equals(
                                newHostId);
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
                || !currentRoomId.equals(
                        roomId)) {

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
         * Tránh tạo GameScene nhiều lần.
         */
        if (gameScene != null) {
            return;
        }

        gameScene =
                new GameScene(
                        network);

        /*
         * Truyền player ID server cấp.
         */
        if (localPlayerId != null) {

            gameScene.setLocalPlayerId(
                    localPlayerId);
        }

        /*
         * WebSocket chuyển WORLD_STATE
         * vào GameScene.
         */
        network.setGameScene(
                gameScene);

        /*
         * =================================================
         * GAME CONTAINER
         * =================================================
         *
         * GameScene nằm dưới.
         * Toast nằm trên.
         */
        gameContainer =
                new StackPane();

        gameContainer.setPrefSize(
                800,
                600);

        gameContainer.getChildren().add(
                gameScene);

        // =================================================
        // DEPARTURE TOAST
        // =================================================

        departureToast =
                new Label();

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

        /*
         * Cách mép trái 20px,
         * cách mép dưới 20px.
         */
        StackPane.setMargin(
                departureToast,
                new javafx.geometry.Insets(
                        0,
                        0,
                        20,
                        20));

        gameContainer.getChildren().add(
                departureToast);

        departureToastTimer =
                new PauseTransition(
                        Duration.seconds(4));

        departureToastTimer.setOnFinished(
                event ->
                        hideDepartureToast());

        // =================================================
        // START LOOP
        // =================================================

        gameScene.startLoop();

        /*
         * Dùng Scene hiện tại.
         */
        scene.setRoot(
                gameContainer);

        /*
         * Nhận keyboard input.
         */
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
    // SHORT PLAYER ID
    // =====================================================

    private String shortPlayerId(
            String playerId) {

        if (playerId == null
                || playerId.isBlank()) {

            return "unknown";
        }

        /*
         * WebSocketSession ID thường khá dài.
         *
         * Chỉ hiển thị 6 ký tự đầu
         * cho dễ đọc.
         */
        if (playerId.length() <= 6) {

            return playerId;
        }

        return playerId.substring(
                0,
                6);
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