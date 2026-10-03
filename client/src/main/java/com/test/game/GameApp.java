package com.test.game;

import com.test.core.GameWebSocketClient;
import com.test.core.PlayerDirectory;
import com.test.core.scene.GameScene;
import com.test.ui.CreateRoomView;
import com.test.ui.FindRoomView;
import com.test.ui.JoinRoomView;
import com.test.ui.LobbyView;
import com.test.ui.MainMenuView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameApp extends Application {

    private Stage stage;
    private Scene scene;

    private GameWebSocketClient network;
    private GameAppNetwork appNetwork;
    private GameAppRooms appRooms;
    private GameAppGame appGame;

    private MainMenuView mainMenu;
    private CreateRoomView createRoom;
    private JoinRoomView joinRoom;
    private FindRoomView findRoom;
    private LobbyView lobby;

    private final PlayerDirectory playerDirectory =
            new PlayerDirectory();

    private String localPlayerId;
    private String currentPlayerName;
    private String currentRoomId;
    private String currentMapId;
    private boolean currentHost;

    // =====================================================
    // START
    // =====================================================

    @Override
    public void start(Stage stage) {

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
        // NETWORK
        // =================================================

        network = new GameWebSocketClient();

        // =================================================
        // APP MODULES
        // =================================================

        appNetwork = new GameAppNetwork(
                this);

        appRooms = new GameAppRooms(
                this);

        appGame = new GameAppGame(
                this);

        network.setMessageHandler(
                appNetwork::handleServerMessage);

        network.connect();

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

    public Scene getScene() {
        return scene;
    }

    public GameWebSocketClient getNetwork() {
        return network;
    }

    public PlayerDirectory getPlayerDirectory() {
        return playerDirectory;
    }

    public MainMenuView getMainMenu() {
        return mainMenu;
    }

    public CreateRoomView getCreateRoomView() {
        return createRoom;
    }

    public JoinRoomView getJoinRoomView() {
        return joinRoom;
    }

    public FindRoomView getFindRoomView() {
        return findRoom;
    }

    public LobbyView getLobbyView() {
        return lobby;
    }

    public GameScene getGameScene() {
        return appGame.getGameScene();
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

    // =====================================================
    // INTERNAL STATE SETTERS
    // =====================================================

    void setLocalPlayerId(
            String localPlayerId) {

        this.localPlayerId =
                localPlayerId;
    }

    void setCurrentPlayerName(
            String currentPlayerName) {

        this.currentPlayerName =
                currentPlayerName;
    }

    void setCurrentRoomId(
            String currentRoomId) {

        this.currentRoomId =
                currentRoomId;
    }

    void setCurrentMapId(
            String currentMapId) {

        this.currentMapId =
                currentMapId;
    }

    void setCurrentHost(
            boolean currentHost) {

        this.currentHost =
                currentHost;
    }

    // =====================================================
    // PLAYER NAME
    // =====================================================

    public void submitPlayerName(
            String name) {

        if (name == null) {
            return;
        }

        String normalizedName =
                name.trim();

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
    // CREATE ROOM VIEW
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
    // JOIN ROOM VIEW
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
    // FIND ROOM VIEW
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
    // ROOM API
    // =====================================================

    public void showLobby(
            String roomId,
            boolean host) {

        appRooms.showLobby(
                roomId,
                host);
    }

    public void findRooms() {

        appRooms.findRooms();
    }

    public void createRoom() {

        appRooms.createRoom();
    }

    public void changeMap(
            String mapId) {

        appRooms.changeMap(
                mapId);
    }

    public void joinRoom(
            String roomId) {

        appRooms.joinRoom(
                roomId);
    }

    public void startGame() {

        appRooms.startGame();
    }

    public void leaveRoom() {

        appRooms.leaveRoom();
    }

    // =====================================================
    // GAME API
    // =====================================================

    public void openGameScene() {

        appGame.openGameScene();
    }

    public void showDepartureToast(
            String message) {

        appGame.showDepartureToast(
                message);
    }

    public void hideDepartureToast() {

        appGame.hideDepartureToast();
    }

    public boolean isLobbyShowing() {

        return appGame.isLobbyShowing();
    }

    // =====================================================
    // STOP
    // =====================================================

    @Override
    public void stop() {

        if (appGame != null) {
            appGame.dispose();
        }
    }

    // =====================================================
    // MAIN
    // =====================================================

    public static void main(
            String[] args) {

        launch(args);
    }
}