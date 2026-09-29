package com.test;

import com.test.core.GameWebSocketClient;
import com.test.ui.CreateRoomView;
import com.test.ui.JoinRoomView;
import com.test.ui.LobbyView;
import com.test.ui.MainMenuView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameApp extends Application {

    private Stage stage;

    private GameWebSocketClient network;

    private MainMenuView mainMenu;
    private CreateRoomView createRoom;
    private JoinRoomView joinRoom;
    private LobbyView lobby;

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
         * Tất cả message lobby sẽ đi vào đây.
         */
        network.setMessageHandler(
                this::handleServerMessage);

        /*
         * Connect một lần duy nhất khi application start.
         *
         * Không connect lại mỗi khi chuyển scene.
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

        lobby =
                new LobbyView(this);

        // =================================================
        // START
        // =================================================

        showMainMenu();

        stage.show();
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

        stage.setScene(
                new Scene(mainMenu)
        );
    }

    // =====================================================
    // CREATE ROOM
    // =====================================================

    public void showCreateRoom() {

        stage.setScene(
                new Scene(createRoom)
        );

        /*
         * Không CREATE_ROOM ở đây.
         *
         * View sẽ gọi createRoom()
         */
    }

    // =====================================================
    // JOIN ROOM
    // =====================================================

    public void showJoinRoom() {

        stage.setScene(
                new Scene(joinRoom)
        );
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

        stage.setScene(
                new Scene(lobby)
        );
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
    }

    // =====================================================
    // LEAVE ROOM
    // =====================================================

    public void leaveRoom() {

        network.send(
                "LEAVE_ROOM");

        currentRoomId = null;
        currentHost = false;

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

        /*
         * Server đã tạo room thành công.
         *
         * Chuyển sang Lobby.
         */
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

        /*
         * Tạm thời chuyển vào lobby.
         */
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
         *
         * Ví dụ:
         *
         * ROOM_STATE|ABCDE|2|4|P001
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

        currentRoomId =
                roomId;

        currentHost =
                localPlayerId != null
                        && localPlayerId.equals(hostId);

        /*
         * Nếu chưa ở Lobby thì vào Lobby.
         */
        if (!isLobbyShowing()) {

            showLobby(
                    roomId,
                    currentHost);

        } else {

            lobby.setRoomId(
                    roomId);

            lobby.setPlayers(
                    playerCount,
                    maxPlayers);

            lobby.setHost(
                    currentHost);
        }
    }

    // =====================================================
    // ROOM ERROR
    // =====================================================

    private void handleRoomError(
            String message) {

        /*
         * Ví dụ:
         *
         * ROOM_ERROR|ROOM_NOT_FOUND
         * ROOM_ERROR|ROOM_FULL
         * ROOM_ERROR|NOT_HOST
         */

        String error =
                message.substring(
                        "ROOM_ERROR|".length());

        System.err.println(
                "Room error: "
                        + error);

        /*
         * Tạm thời hiển thị ra console.
         *
         * Sau khi Lobby/UI hoàn thiện,
         * chuyển thành label/dialog.
         */
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

        showMainMenu();
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
         * Chỉ chuyển game nếu đúng room hiện tại.
         */
        if (currentRoomId == null
                || !currentRoomId.equals(roomId)) {

            return;
        }

        System.out.println(
                "Game started: "
                        + roomId);

        /*
         * QUAN TRỌNG:
         *
         * Không thay đổi physics/gameplay ở đây.
         *
         * GameScene hiện tại sẽ được
         * khởi tạo ở bước lifecycle tiếp theo.
         */
        openGameScene();
    }

    // =====================================================
    // OPEN GAME
    // =====================================================

    private void openGameScene() {

        /*
         * Bước này cần khớp với constructor/lifecycle
         * hiện tại của GameScene.
         *
         * Vì gameplay của repo đang ổn định,
         * chưa tự ý đoán constructor ở đây.
         */
        System.out.println(
                "Opening GameScene...");
    }

    // =====================================================
    // CHECK CURRENT SCENE
    // =====================================================

    private boolean isLobbyShowing() {

        if (stage.getScene() == null) {

            return false;
        }

        return stage.getScene()
                .getRoot() == lobby;
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