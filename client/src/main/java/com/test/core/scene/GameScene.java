package com.test.core.scene;

import com.test.common.GameConfig;
import com.test.common.map.MapData;
import com.test.core.ClientPlayerController;
import com.test.core.GameWebSocketClient;
import com.test.core.Player;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class GameScene extends Pane {

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    private final GameWebSocketClient network;

    private final String mapId;

    private final Pane world =
            new Pane();

    private final Player player;

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

    public GameScene(
            GameWebSocketClient network,
            String mapId) {

        this.network = network;

        if (mapId == null
                || mapId.isBlank()) {

            throw new IllegalArgumentException(
                    "mapId must not be blank");
        }

        this.mapId =
                mapId;

        setPrefSize(
                VIEW_WIDTH,
                VIEW_HEIGHT);

        getChildren().add(
                world);

        /*
         * Player được tạo tạm thời tại 0, 0.
         *
         * Vị trí thật sẽ được lấy từ
         * map data sau khi load xong.
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

        input.setup();

        map.loadAsync(
                mapId);

        camera.update();

        Platform.runLater(
                this::requestFocus);
    }

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

        controller.getState().setOnGround(true);

        controller.getState().setVelocityX(0);
        controller.getState().setVelocityY(0);

        updateJumpBar();

        camera.update();
    }

    public void setLocalPlayerId(
            String playerId) {

        controller.getState().setPlayerId(
                playerId);

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

    public void startLoop() {

        simulation.startLoop();
    }

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
    }
}