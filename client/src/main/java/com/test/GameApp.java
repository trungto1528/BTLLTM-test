package com.test;

import com.test.core.GameScene;
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

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        stage.setTitle("Jump King Multiplayer");
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.setResizable(false);

        // =========================
        // NETWORK
        // =========================

        network = new GameWebSocketClient();

        // =========================
        // UI
        // =========================

        mainMenu =
                new MainMenuView(this);

        createRoom =
                new CreateRoomView(this);

        joinRoom =
                new JoinRoomView(this);

        lobby =
                new LobbyView(this);

        // =========================
        // START
        // =========================

        showMainMenu();

        stage.show();
    }

    // =========================
    // GETTERS
    // =========================

    public Stage getStage() {
        return stage;
    }

    public GameWebSocketClient getNetwork() {
        return network;
    }

    // =========================
    // MAIN MENU
    // =========================

    public void showMainMenu() {

        stage.setScene(
                new Scene(mainMenu)
        );
    }

    // =========================
    // CREATE ROOM
    // =========================

    public void showCreateRoom() {

        stage.setScene(
                new Scene(createRoom)
        );

        /*
         * Sau này:
         *
         * network.connect();
         *
         * rồi:
         *
         * network.send("CREATE_ROOM");
         */
    }

    // =========================
    // JOIN ROOM
    // =========================

    public void showJoinRoom() {

        stage.setScene(
                new Scene(joinRoom)
        );
    }

    // =========================
    // LOBBY
    // =========================

    public void showLobby(
            String roomId,
            boolean host) {

        lobby.setRoomId(roomId);
        lobby.setHost(host);

        stage.setScene(
                new Scene(lobby)
        );
    }

    // =========================
    // ROOM
    // =========================

    public void createRoom() {

        /*
         * Sau này:
         *
         * network.send("CREATE_ROOM");
         *
         * Không tự tạo roomId ở client.
         *
         * Server phải tạo room ID.
         */
    }

    public void joinRoom(
            String roomId) {

        /*
         * Sau này:
         *
         * network.send(
         *     "JOIN_ROOM|" + roomId
         * );
         */
    }

    // =========================
    // GAME
    // =========================

    public void startGame() {

        /*
         * Chưa chuyển game ở đây.
         *
         * Phải đợi:
         *
         * GAME_STARTED
         *
         * từ server.
         */

        System.out.println(
                "Starting game...");
    }

    // =========================
    // MAIN
    // =========================

    public static void main(
            String[] args) {

        launch(args);
    }
}