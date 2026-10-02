package com.test.core.scene;

import com.test.common.GameConfig;
import com.test.common.map.MapData;
import com.test.core.ClientPlayerController;
import com.test.core.GameWebSocketClient;
import com.test.core.Player;
import com.test.core.PlayerDirectory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class GameScene extends Pane {

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    private final GameWebSocketClient network;

    private final String mapId;

    private final PlayerDirectory playerDirectory;

    private final Pane world =
            new Pane();

    private final Player player;

    private final Label playerNameLabel =
            new Label();

    private final Rectangle jumpBarBackground =
            new Rectangle(40, 6);

    private final Rectangle jumpBarFill =
            new Rectangle(0, 6);

    private final ClientPlayerController controller;

    private final GameSceneInput input;
    private final GameSceneNetwork networkHandler;
    private final GameSceneSimulation simulation;
    private final GameSceneRemotePlayers remotePlayers;
    private final GameSceneMap map;
    private final GameSceneCamera camera;

    /*
     * =====================================================
     * PAUSE UI
     * =====================================================
     */

    private final StackPane pauseOverlay =
            new StackPane();

    private final VBox pausePanel =
            new VBox();

    private final Label pauseTitle =
            new Label("PAUSED");

    private final Label heightLabel =
            new Label();

    private final Button continueButton =
            new Button("Continue");

    private final Button backButton =
            new Button("Back");

    private boolean pauseVisible;

    public GameScene(
            GameWebSocketClient network,
            String mapId,
            PlayerDirectory playerDirectory) {

        this.network = network;

        if (mapId == null
                || mapId.isBlank()) {

            throw new IllegalArgumentException(
                    "mapId must not be blank");
        }

        if (playerDirectory == null) {

            throw new IllegalArgumentException(
                    "playerDirectory must not be null");
        }

        this.mapId = mapId;

        this.playerDirectory =
                playerDirectory;

        setPrefSize(
                VIEW_WIDTH,
                VIEW_HEIGHT);

        /*
         * World nằm bên dưới.
         */
        getChildren().add(
                world);

        /*
         * Player tạm thời tại 0,0.
         */
        player =
                new Player(
                        0,
                        0);

        controller =
                new ClientPlayerController(
                        "local-player");

        controller.getState().setX(
                player.getX());

        controller.getState().setY(
                player.getY());

        controller.getState().setOnGround(
                true);

        world.getChildren().add(
                player);

        /*
         * Tên của local player.
         *
         * Đặt sau player trong world để tên
         * nằm phía trên player về thứ tự render.
         */
        setupPlayerNameLabel();

        world.getChildren().add(
                playerNameLabel);

        /*
         * Jump bar.
         */
        jumpBarBackground.setFill(
                Color.GRAY);

        jumpBarFill.setFill(
                Color.ORANGE);

        world.getChildren().add(
                jumpBarBackground);

        world.getChildren().add(
                jumpBarFill);

        input =
                new GameSceneInput(this);

        networkHandler =
                new GameSceneNetwork(this);

        simulation =
                new GameSceneSimulation(this);

        remotePlayers =
                new GameSceneRemotePlayers(this);

        map =
                new GameSceneMap(this);

        camera =
                new GameSceneCamera(this);

        /*
         * Pause overlay phải nằm ngoài world.
         *
         * Vì world bị camera translateY,
         * overlay không được đặt bên trong world.
         */
        setupPauseOverlay();

        input.setup();

        map.loadAsync(
                mapId);

        updatePlayerNameLabel();

        camera.update();

        Platform.runLater(
                this::requestFocus);
    }

    // =====================================================
    // PLAYER NAME
    // =====================================================

    private void setupPlayerNameLabel() {

        playerNameLabel.setAlignment(
                Pos.CENTER);

        playerNameLabel.setPrefWidth(
                120);

        playerNameLabel.setMinWidth(
                120);

        playerNameLabel.setMaxWidth(
                120);

        playerNameLabel.setPrefHeight(
                24);

        playerNameLabel.setStyle(
                "-fx-text-fill: white;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-color: rgba(20,20,25,0.82);"
                        + "-fx-background-radius: 5;"
                        + "-fx-padding: 3 8;");

        playerNameLabel.setMouseTransparent(
                true);
    }

    public void updatePlayerNameLabel() {

        String playerId =
                getLocalPlayerId();

        String name =
                playerDirectory.getNameOrFallback(
                        playerId);

        playerNameLabel.setText(
                name);

        double labelWidth =
                playerNameLabel.getPrefWidth();

        double labelX =
                player.getX()
                        + player.getWidth() / 2
                        - labelWidth / 2;

        double labelY =
                player.getY() - 30;

        playerNameLabel.setLayoutX(
                labelX);

        playerNameLabel.setLayoutY(
                labelY);
    }

    // =====================================================
    // PAUSE OVERLAY
    // =====================================================

    private void setupPauseOverlay() {

        pauseOverlay.setPrefSize(
                VIEW_WIDTH,
                VIEW_HEIGHT);

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        /*
         * Overlay toàn màn hình.
         *
         * Background mờ để vẫn nhìn thấy
         * player và map phía sau.
         */
        Rectangle dim =
                new Rectangle(
                        VIEW_WIDTH,
                        VIEW_HEIGHT);

        dim.setFill(
                Color.rgb(
                        0,
                        0,
                        0,
                        0.55));

        pauseOverlay.getChildren().add(
                dim);

        /*
         * Panel.
         */
        pausePanel.setAlignment(
                Pos.CENTER);

        pausePanel.setSpacing(
                18);

        pausePanel.setPadding(
                new Insets(
                        30,
                        45,
                        30,
                        45));

        pausePanel.setMaxWidth(
                300);

        pausePanel.setStyle(
                "-fx-background-color: rgba(30, 30, 35, 0.96);"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-color: rgba(255,255,255,0.20);"
                        + "-fx-border-radius: 14;");

        /*
         * Title.
         */
        pauseTitle.setStyle(
                "-fx-text-fill: white;"
                        + "-fx-font-size: 28px;"
                        + "-fx-font-weight: bold;");

        /*
         * Height.
         */
        heightLabel.setStyle(
                "-fx-text-fill: white;"
                        + "-fx-font-size: 16px;");

        /*
         * Continue.
         */
        continueButton.setPrefWidth(
                190);

        continueButton.setPrefHeight(
                40);

        continueButton.setStyle(
                "-fx-font-size: 15px;");

        continueButton.setOnAction(
                event -> resumeGame());

        /*
         * Back.
         */
        backButton.setPrefWidth(
                190);

        backButton.setPrefHeight(
                40);

        backButton.setStyle(
                "-fx-font-size: 15px;");

        backButton.setOnAction(
                event -> backToMenu());

        pausePanel.getChildren().addAll(
                pauseTitle,
                heightLabel,
                continueButton,
                backButton);

        pauseOverlay.getChildren().add(
                pausePanel);

        StackPane.setAlignment(
                pausePanel,
                Pos.CENTER);

        getChildren().add(
                pauseOverlay);
    }

    // =====================================================
    // PAUSE
    // =====================================================

    public void togglePause() {

        if (pauseVisible) {

            resumeGame();

        } else {

            pauseGame();
        }
    }

    public void pauseGame() {

        if (pauseVisible) {
            return;
        }

        pauseVisible = true;

        input.resetKeys();

        simulation.setPaused(true);

        updateHeightLabel();

        pauseOverlay.setVisible(true);
        pauseOverlay.setManaged(true);

        pauseOverlay.toFront();

        continueButton.requestFocus();
    }

    // =====================================================
    // RESUME
    // =====================================================

    public void resumeGame() {

        if (!pauseVisible) {
            return;
        }

        pauseVisible = false;

        /*
         * Khi resume, simulation sẽ lấy
         * authoritative server state mới nhất
         * nếu snapshot đã đến trong lúc pause.
         */
        simulation.setPaused(false);

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        requestFocus();
    }

    // =====================================================
    // BACK
    // =====================================================

    private void backToMenu() {

        pauseVisible = false;

        input.resetKeys();

        simulation.setPaused(true);

        /*
         * Không tạo protocol mới.
         *
         * GameApp đã xử lý:
         *
         * ROOM_LEFT -> Main Menu
         */
        network.send(
                "LEAVE_ROOM");
    }

    // =====================================================
    // HEIGHT
    // =====================================================

    private void updateHeightLabel() {

        MapData mapData =
                getMapData();

        if (mapData == null) {

            heightLabel.setText(
                    "Height: --");

            return;
        }

        /*
         * Sàn map là mặt trên của
         * cell cuối cùng:
         *
         * height - cellSize
         */
        double floorY =
                mapData.getHeight()
                        - mapData.getCellSize();

        /*
         * Player Y là tọa độ top.
         *
         * Độ cao tính từ chân player
         * tới mặt sàn.
         */
        double playerBottom =
                player.getY()
                        + player.getHeight();

        double height =
                floorY
                        - playerBottom;

        if (height < 0) {
            height = 0;
        }

        heightLabel.setText(
                String.format(
                        "Height: %.0f px",
                        height));
    }

    public boolean isPaused() {
        return pauseVisible;
    }

    // =====================================================
    // BASIC GETTERS
    // =====================================================

    public String getMapId() {

        return mapId;
    }

    public void setMapSpawn(
            double x,
            double y) {

        player.setX(x);
        player.setY(y);

        controller.getState().setX(x);
        controller.getState().setY(y);

        controller.getState().setOnGround(
                true);

        controller.getState().setVelocityX(0);
        controller.getState().setVelocityY(0);

        updateJumpBar();
        updatePlayerNameLabel();

        camera.update();
    }

    public void setLocalPlayerId(
            String playerId) {

        controller.getState().setPlayerId(
                playerId);

        updatePlayerNameLabel();

        System.out.println(
                "Local player ID: "
                        + playerId);
    }

    public String getLocalPlayerId() {

        return controller
                .getState()
                .getPlayerId();
    }

    public GameWebSocketClient getNetwork() {
        return network;
    }

    public Pane getWorld() {
        return world;
    }

    public Player getPlayer() {
        return player;
    }

    public ClientPlayerController getController() {
        return controller;
    }

    public PlayerDirectory getPlayerDirectory() {
        return playerDirectory;
    }

    public MapData getMapData() {
        return map.getMapData();
    }

    public GameSceneInput getInput() {
        return input;
    }

    public GameSceneNetwork getNetworkHandler() {
        return networkHandler;
    }

    public GameSceneSimulation getSimulation() {
        return simulation;
    }

    public GameSceneRemotePlayers getRemotePlayers() {
        return remotePlayers;
    }

    public GameSceneMap getMap() {
        return map;
    }

    public GameSceneCamera getCamera() {
        return camera;
    }

    // =====================================================
    // NETWORK EVENTS
    // =====================================================

    public void handleWorldState(
            String message) {

        networkHandler.handleWorldState(
                message);
    }

    public void handlePlayerState(
            String message) {

        networkHandler.handlePlayerState(
                message);
    }

    public void handlePlayerLeft(
            String message) {

        remotePlayers.handlePlayerLeft(
                message);
    }

    // =====================================================
    // LOOP
    // =====================================================

    public void startLoop() {

        simulation.startLoop();
    }

    // =====================================================
    // JUMP BAR
    // =====================================================

    public void updateJumpBar() {

        double barWidth = 40;

        double barX =
                player.getX()
                        + player.getWidth() / 2
                        - barWidth / 2;

        double barY =
                player.getY() - 12;

        jumpBarBackground.setX(
                barX);

        jumpBarBackground.setY(
                barY);

        double jumpPower =
                controller
                        .getState()
                        .getJumpPower();

        double ratio =
                (jumpPower
                        - GameConfig.MIN_JUMP_POWER)
                        / (GameConfig.MAX_JUMP_POWER
                                - GameConfig.MIN_JUMP_POWER);

        if (ratio < 0) {
            ratio = 0;
        }

        if (ratio > 1) {
            ratio = 1;
        }

        jumpBarFill.setX(
                barX);

        jumpBarFill.setY(
                barY);

        jumpBarFill.setWidth(
                barWidth * ratio);

        updatePlayerNameLabel();
    }
}