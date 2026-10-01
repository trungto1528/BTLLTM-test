package com.test.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.test.common.GameConfig;
import com.test.common.InputCommand;
import com.test.common.PlayerState;
import com.test.common.map.MapData;

import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class GameScene extends Pane {

    private final GameWebSocketClient network;

    private int inputSequence = 0;

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    private static final double MAP_WIDTH = 800;
    private static final double MAP_HEIGHT = 6000;

    private final Pane world =
            new Pane();

    private final Player player;

    private final Rectangle jumpBarBackground =
            new Rectangle(40, 6);

    private final Rectangle jumpBarFill =
            new Rectangle(0, 6);

    private double cameraY = 0;

    private MapData mapData;

    private final GameMapRenderer mapRenderer =
            new GameMapRenderer();

    private final ClientPlayerController controller;

    private double previousLocalX;
    private double previousLocalY;

    private double currentLocalX;
    private double currentLocalY;

    private boolean localRenderInitialized;

    private final Map<String, Player> remotePlayers =
            new HashMap<>();

    private static class RemoteState {

        private final PlayerState state;

        private double previousX;
        private double previousY;

        private double targetX;
        private double targetY;

        private boolean previousOnGround;
        private boolean targetOnGround;

        private boolean initialized;

        private long snapshotTimeNanos;

        private RemoteState(String playerId) {

            state =
                    new PlayerState(playerId);
        }
    }

    private final Map<String, RemoteState> remoteStates =
            new HashMap<>();

    private String localPlayerId;

    /*
     * Sequence của input cuối cùng mà
     * WORLD_STATE đã authoritative ACK.
     */
    private int lastServerSequence = 0;

    /*
     * Tick server cuối cùng đã nhận.
     */
    private long lastServerTick = 0;

    /*
     * Logical client tick.
     */
    private long clientTick = 0;

    /*
     * Accumulator cho fixed simulation.
     */
    private double tickAccumulator = 0;

    /*
     * Không cho render frame chạy quá nhiều
     * simulation tick liên tiếp.
     */
    private static final int MAX_TICKS_PER_FRAME = 5;

    /*
     * Input chưa được server ACK.
     */
    private final List<PendingInput> pendingInputs =
            new ArrayList<>();

    private static class PendingInput {

        private final InputCommand command;

        /*
         * Logical client tick mà input
         * có hiệu lực.
         */
        private final long tick;

        private PendingInput(
                InputCommand command,
                long tick) {

            this.command = command;
            this.tick = tick;
        }
    }

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean spacePressed;

    public GameScene(
            GameWebSocketClient network) {

        this.network = network;

        setPrefSize(
                VIEW_WIDTH,
                VIEW_HEIGHT);

        world.setPrefSize(
                MAP_WIDTH,
                MAP_HEIGHT);

        getChildren().add(
                world);

        player =
                new Player(
                        180,
                        5930);

        controller =
                new ClientPlayerController(
                        "local-player");

        controller.getState().setX(
                player.getX());

        controller.getState().setY(
                player.getY());

        controller.getState().setOnGround(
                true);

        /*
         * Khởi tạo render buffer.
         */
        previousLocalX =
                player.getX();

        previousLocalY =
                player.getY();

        currentLocalX =
                player.getX();

        currentLocalY =
                player.getY();

        localRenderInitialized =
                true;

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

        loadMapAsync();

        setupInput();

        updateCamera();

        javafx.application.Platform.runLater(
                this::requestFocus);
    }

    public void setLocalPlayerId(
            String playerId) {

        this.localPlayerId =
                playerId;

        controller.getState().setPlayerId(
                playerId);

        System.out.println(
                "Local player ID: "
                        + playerId);
    }

    private int nextInputSequence() {

        return ++inputSequence;
    }

    public void handleWorldState(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 3) {
            return;
        }

        if (!"WORLD_STATE".equals(parts[0])) {
            return;
        }

        if (!"TICK".equals(parts[1])) {

            System.err.println(
                    "WORLD_STATE missing TICK: "
                            + message);

            return;
        }

        final long serverTick;

        try {

            serverTick =
                    Long.parseLong(
                            parts[2]);

        } catch (NumberFormatException e) {

            System.err.println(
                    "Invalid server tick: "
                            + parts[2]);

            return;
        }

        /*
         * Parse network data trước.
         *
         * Không đụng JavaFX Node ở WebSocket thread.
         */
        List<RemoteSnapshot> snapshots =
                new ArrayList<>();

        int index = 3;

        while (index < parts.length) {

            if (!"PLAYER".equals(parts[index])) {

                index++;

                continue;
            }

            /*
             * PLAYER
             *
             * + playerId
             * + sequence
             * + x
             * + y
             * + velocityX
             * + velocityY
             * + onGround
             * + chargingJump
             * + chargingUp
             * + maxChargeTimer
             * + hasSelectedDirection
             * + jumpPower
             * + facingDirection
             * + movingLeft
             * + movingRight
             *
             * = 15 fields sau PLAYER.
             */

            if (index + 15 >= parts.length) {

                System.err.println(
                        "Incomplete PLAYER snapshot");

                break;
            }

            try {

                String playerId =
                        parts[index + 1];

                int sequence =
                        Integer.parseInt(
                                parts[index + 2]);

                double x =
                        Double.parseDouble(
                                parts[index + 3]);

                double y =
                        Double.parseDouble(
                                parts[index + 4]);

                double velocityX =
                        Double.parseDouble(
                                parts[index + 5]);

                double velocityY =
                        Double.parseDouble(
                                parts[index + 6]);

                boolean onGround =
                        Boolean.parseBoolean(
                                parts[index + 7]);

                boolean chargingJump =
                        Boolean.parseBoolean(
                                parts[index + 8]);

                boolean chargingUp =
                        Boolean.parseBoolean(
                                parts[index + 9]);

                double maxChargeTimer =
                        Double.parseDouble(
                                parts[index + 10]);

                boolean hasSelectedDirection =
                        Boolean.parseBoolean(
                                parts[index + 11]);

                double jumpPower =
                        Double.parseDouble(
                                parts[index + 12]);

                int facingDirection =
                        Integer.parseInt(
                                parts[index + 13]);

                boolean movingLeft =
                        Boolean.parseBoolean(
                                parts[index + 14]);

                boolean movingRight =
                        Boolean.parseBoolean(
                                parts[index + 15]);

                snapshots.add(
                        new RemoteSnapshot(
                                playerId,
                                sequence,
                                x,
                                y,
                                velocityX,
                                velocityY,
                                onGround,
                                chargingJump,
                                chargingUp,
                                maxChargeTimer,
                                hasSelectedDirection,
                                jumpPower,
                                facingDirection,
                                movingLeft,
                                movingRight));

                index += 16;

            } catch (Exception e) {

                System.err.println(
                        "Invalid WORLD_STATE player: "
                                + e.getMessage());

                break;
            }
        }

        /*
         * Chuyển sang JavaFX thread.
         */
        javafx.application.Platform.runLater(() -> {

            /*
             * Bỏ snapshot cũ.
             */
            if (serverTick < lastServerTick) {
                return;
            }

            lastServerTick =
                    serverTick;

            for (RemoteSnapshot snapshot
                    : snapshots) {

                if (snapshot.playerId.equals(
                        localPlayerId)) {

                    reconcileLocalPlayer(
                            serverTick,
                            snapshot);

                } else {

                    updateRemotePlayer(
                            snapshot);
                }
            }
        });
    }

    private static class RemoteSnapshot {

        private final String playerId;
        private final int sequence;

        private final double x;
        private final double y;

        private final double velocityX;
        private final double velocityY;

        private final boolean onGround;

        private final boolean chargingJump;
        private final boolean chargingUp;

        private final double maxChargeTimer;

        private final boolean hasSelectedDirection;

        private final double jumpPower;

        private final int facingDirection;

        private final boolean movingLeft;
        private final boolean movingRight;

        private RemoteSnapshot(
                String playerId,
                int sequence,
                double x,
                double y,
                double velocityX,
                double velocityY,
                boolean onGround,
                boolean chargingJump,
                boolean chargingUp,
                double maxChargeTimer,
                boolean hasSelectedDirection,
                double jumpPower,
                int facingDirection,
                boolean movingLeft,
                boolean movingRight) {

            this.playerId =
                    playerId;

            this.sequence =
                    sequence;

            this.x =
                    x;

            this.y =
                    y;

            this.velocityX =
                    velocityX;

            this.velocityY =
                    velocityY;

            this.onGround =
                    onGround;

            this.chargingJump =
                    chargingJump;

            this.chargingUp =
                    chargingUp;

            this.maxChargeTimer =
                    maxChargeTimer;

            this.hasSelectedDirection =
                    hasSelectedDirection;

            this.jumpPower =
                    jumpPower;

            this.facingDirection =
                    facingDirection;

            this.movingLeft =
                    movingLeft;

            this.movingRight =
                    movingRight;
        }
    }

    private void reconcileLocalPlayer(
            long serverTick,
            RemoteSnapshot snapshot) {

        /*
         * Chỉ reconciliation khi server ACK
         * một input sequence mới.
         *
         * Nếu snapshot chỉ là snapshot mới
         * nhưng sequence không đổi thì không
         * được restore state local.
         */
        if (snapshot.sequence
                <= lastServerSequence) {

            return;
        }

        long targetClientTick =
                clientTick;

        /*
         * ACK mới.
         */
        lastServerSequence =
                snapshot.sequence;

        /*
         * Xóa những input server đã xử lý.
         */
        pendingInputs.removeIf(
                input ->
                        input.command.getSequence()
                                <= lastServerSequence);

        /*
         * Restore authoritative state.
         */
        controller.applyServerState(
                snapshot.x,
                snapshot.y,
                snapshot.velocityX,
                snapshot.velocityY,
                snapshot.onGround,
                snapshot.chargingJump,
                snapshot.chargingUp,
                snapshot.maxChargeTimer,
                snapshot.hasSelectedDirection,
                snapshot.jumpPower,
                snapshot.facingDirection,
                snapshot.movingLeft,
                snapshot.movingRight);

        /*
         * Server đã đi tới hoặc vượt client.
         */
        if (targetClientTick
                <= serverTick) {

            if (clientTick < serverTick) {

                clientTick =
                        serverTick;
            }

            resetLocalRenderInterpolation();

            return;
        }

        /*
         * Replay input chưa được server ACK.
         */
        for (long tick =
                     serverTick + 1;
             tick <= targetClientTick;
             tick++) {

            for (PendingInput pending
                    : pendingInputs) {

                if (pending.tick == tick) {

                    controller.applyInput(
                            pending.command);
                }
            }

            controller.tick(
                    mapData);
        }

        /*
         * Sau reconciliation/replay,
         * không nội suy từ state cũ trước
         * reconciliation sang state mới.
         *
         * Nếu không reset buffer, render có thể
         * tạo ra một cú kéo ngược rất nhỏ.
         */
        resetLocalRenderInterpolation();
    }

    private void resetLocalRenderInterpolation() {

        PlayerState state =
                controller.getState();

        previousLocalX =
                state.getX();

        previousLocalY =
                state.getY();

        currentLocalX =
                state.getX();

        currentLocalY =
                state.getY();

        localRenderInitialized =
                true;

        /*
         * Render ngay authoritative/predicted
         * state mới sau reconciliation.
         */
        player.setX(
                state.getX());

        player.setY(
                state.getY());

        player.setOnGround(
                state.isOnGround());
    }

    private void applyInputsForTick(
            long tick) {

        for (PendingInput pending
                : pendingInputs) {

            if (pending.tick == tick) {

                controller.applyInput(
                        pending.command);
            }
        }
    }

    private void beginLocalSimulationTick() {

        PlayerState state =
                controller.getState();

        previousLocalX =
                state.getX();

        previousLocalY =
                state.getY();

        if (!localRenderInitialized) {

            currentLocalX =
                    state.getX();

            currentLocalY =
                    state.getY();

            localRenderInitialized =
                    true;
        }
    }

    private void finishLocalSimulationTick() {

        PlayerState state =
                controller.getState();

        currentLocalX =
                state.getX();

        currentLocalY =
                state.getY();
    }

    private void updateRemotePlayer(
            RemoteSnapshot snapshot) {

        String playerId =
                snapshot.playerId;

        /*
         * Local player tuyệt đối không được
         * đưa vào remotePlayers.
         */
        if (playerId.equals(
                localPlayerId)) {

            return;
        }

        Player remote =
                remotePlayers.get(
                        playerId);

        if (remote == null) {

            remote =
                    new Player(
                            snapshot.x,
                            snapshot.y);

            remotePlayers.put(
                    playerId,
                    remote);

            world.getChildren().add(
                    remote);
        }

        RemoteState remoteState =
                remoteStates.computeIfAbsent(
                        playerId,
                        RemoteState::new);

        if (!remoteState.initialized) {

            remoteState.previousX =
                    snapshot.x;

            remoteState.previousY =
                    snapshot.y;

            remoteState.targetX =
                    snapshot.x;

            remoteState.targetY =
                    snapshot.y;

            remoteState.previousOnGround =
                    snapshot.onGround;

            remoteState.targetOnGround =
                    snapshot.onGround;

            remoteState.initialized =
                    true;

        } else {

            /*
             * Snapshot trước -> previous
             * Snapshot mới  -> target
             */
            remoteState.previousX =
                    remoteState.targetX;

            remoteState.previousY =
                    remoteState.targetY;

            remoteState.previousOnGround =
                    remoteState.targetOnGround;

            remoteState.targetX =
                    snapshot.x;

            remoteState.targetY =
                    snapshot.y;

            remoteState.targetOnGround =
                    snapshot.onGround;
        }

        remoteState.state.setX(
                snapshot.x);

        remoteState.state.setY(
                snapshot.y);

        remoteState.state.setVelocityX(
                snapshot.velocityX);

        remoteState.state.setVelocityY(
                snapshot.velocityY);

        remoteState.state.setOnGround(
                snapshot.onGround);

        remoteState.state.setChargingJump(
                snapshot.chargingJump);

        remoteState.state.setChargingUp(
                snapshot.chargingUp);

        remoteState.state.setMaxChargeTimer(
                snapshot.maxChargeTimer);

        remoteState.state.setHasSelectedDirection(
                snapshot.hasSelectedDirection);

        remoteState.state.setJumpPower(
                snapshot.jumpPower);

        remoteState.state.setFacingDirection(
                snapshot.facingDirection);

        remoteState.snapshotTimeNanos =
                System.nanoTime();
    }

    private void updateRemotePlayers() {

        long now =
                System.nanoTime();

        final double snapshotIntervalNanos =
                GameConfig.TICK_NANOS
                        * GameConfig.SNAPSHOT_INTERVAL;

        for (Map.Entry<String, Player> entry
                : remotePlayers.entrySet()) {

            String playerId =
                    entry.getKey();

            Player remote =
                    entry.getValue();

            RemoteState remoteState =
                    remoteStates.get(
                            playerId);

            if (remoteState == null
                    || !remoteState.initialized) {

                continue;
            }

            double alpha =
                    (now
                            - remoteState.snapshotTimeNanos)
                            / snapshotIntervalNanos;

            if (alpha < 0) {
                alpha = 0;
            }

            if (alpha > 1) {
                alpha = 1;
            }

            double x =
                    remoteState.previousX
                            + (remoteState.targetX
                                    - remoteState.previousX)
                            * alpha;

            double y =
                    remoteState.previousY
                            + (remoteState.targetY
                                    - remoteState.previousY)
                            * alpha;

            remote.setX(x);
            remote.setY(y);

            remote.setOnGround(
                    remoteState.targetOnGround);
        }
    }

    private int queueInput(
            String action) {

        int sequence =
                nextInputSequence();

        InputCommand command =
                new InputCommand(
                        sequence,
                        action);

        /*
         * Input có hiệu lực từ fixed tick kế tiếp.
         */
        long inputTick =
                clientTick + 1;

        pendingInputs.add(
                new PendingInput(
                        command,
                        inputTick));

        network.sendInput(
                "INPUT|"
                        + sequence
                        + "|"
                        + action);

        return sequence;
    }

    private int sendInput(
            String action) {

        return queueInput(action);
    }

    private int sendJumpRelease() {

        /*
         * Server tự lấy jumpPower authoritative.
         */
        return queueInput(
                "JUMP_RELEASE");
    }

    public synchronized void handlePlayerState(
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 7) {
            return;
        }

        try {

            /*
             * PLAYER_STATE có thể chứa sequence,
             * nhưng KHÔNG được tự cập nhật
             * lastServerSequence ở đây.
             *
             * WORLD_STATE mới là nguồn authoritative
             * đầy đủ để reconciliation.
             */
            Integer.parseInt(
                    parts[2]);

        } catch (Exception e) {

            System.err.println(
                    "Invalid PLAYER_STATE: "
                            + message);

            e.printStackTrace();
        }
    }

    private void syncLocalVisual() {

        if (!localRenderInitialized) {

            PlayerState state =
                    controller.getState();

            player.setX(
                    state.getX());

            player.setY(
                    state.getY());

            player.setOnGround(
                    state.isOnGround());

            return;
        }

        /*
         * Alpha biểu diễn phần thời gian đã đi
         * vào simulation tick tiếp theo.
         *
         * Render giữa previous và current.
         */
        double alpha =
                tickAccumulator
                        / GameConfig.TICK_DT;

        if (alpha < 0) {
            alpha = 0;
        }

        if (alpha > 1) {
            alpha = 1;
        }

        double renderX =
                previousLocalX
                        + (currentLocalX
                                - previousLocalX)
                                * alpha;

        double renderY =
                previousLocalY
                        + (currentLocalY
                                - previousLocalY)
                                * alpha;

        player.setX(
                renderX);

        player.setY(
                renderY);

        player.setOnGround(
                controller.getState()
                        .isOnGround());
    }

    private void updateJumpBar() {

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
                controller.getState()
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

    private void loadMapAsync() {

        Thread mapLoaderThread =
                new Thread(() -> {

                    try {

                        MapData loadedMap =
                                new GameMapLoader().load(
                                        "map01");

                        javafx.application.Platform.runLater(() -> {

                            mapData =
                                    loadedMap;

                            mapRenderer.setMap(
                                    mapData);

                            world.setPrefSize(
                                    mapData.getWidth(),
                                    mapData.getHeight());

                            world.getChildren().addAll(
                                    0,
                                    mapRenderer.getMapNodes());

                            updateCamera();
                        });

                    } catch (Exception e) {

                        e.printStackTrace();

                        javafx.application.Platform.runLater(() -> {

                            javafx.scene.control.Alert alert =
                                    new javafx.scene.control.Alert(
                                            javafx.scene.control.Alert.AlertType.ERROR);

                            alert.setTitle(
                                    "Map loading failed");

                            alert.setHeaderText(
                                    "Không thể tải bản đồ map01");

                            alert.setContentText(
                                    e.getMessage());

                            alert.showAndWait();
                        });
                    }

                }, "game-map-loader");

        mapLoaderThread.setDaemon(true);

        mapLoaderThread.start();
    }

    private void updateCamera() {

        if (mapData == null) {
            return;
        }

        double desiredCameraY =
                player.getY()
                        - VIEW_HEIGHT / 2.0;

        double maxCameraY =
                Math.max(
                        0,
                        mapData.getHeight()
                                - VIEW_HEIGHT);

        cameraY =
                Math.max(
                        0,
                        Math.min(
                                desiredCameraY,
                                maxCameraY));

        world.setTranslateX(0);

        world.setTranslateY(
                -cameraY);
    }

    private void setupInput() {

        setFocusTraversable(true);

        setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.A
                    || event.getCode()
                    == KeyCode.LEFT) {

                if (!leftPressed) {

                    leftPressed = true;

                    sendInput(
                            "LEFT_PRESS");
                }
            }

            if (event.getCode() == KeyCode.D
                    || event.getCode()
                    == KeyCode.RIGHT) {

                if (!rightPressed) {

                    rightPressed = true;

                    sendInput(
                            "RIGHT_PRESS");
                }
            }

            if (event.getCode()
                    == KeyCode.SPACE) {

                if (!spacePressed) {

                    spacePressed = true;

                    sendInput(
                            "JUMP_START");
                }
            }
        });

        setOnKeyReleased(event -> {

            if (event.getCode() == KeyCode.A
                    || event.getCode()
                    == KeyCode.LEFT) {

                if (leftPressed) {

                    leftPressed = false;

                    sendInput(
                            "LEFT_RELEASE");
                }
            }

            if (event.getCode() == KeyCode.D
                    || event.getCode()
                    == KeyCode.RIGHT) {

                if (rightPressed) {

                    rightPressed = false;

                    sendInput(
                            "RIGHT_RELEASE");
                }
            }

            if (event.getCode()
                    == KeyCode.SPACE) {

                if (spacePressed) {

                    spacePressed = false;

                    sendJumpRelease();
                }
            }
        });
    }

    public void startLoop() {

        AnimationTimer timer =
                new AnimationTimer() {

                    private long lastTime = 0;

                    @Override
                    public void handle(
                            long now) {

                        if (lastTime == 0) {

                            lastTime = now;

                            return;
                        }

                        double frameDelta =
                                (now - lastTime)
                                        / 1_000_000_000.0;

                        lastTime = now;

                        /*
                         * Không cho accumulator tăng
                         * quá lớn khi window bị treo.
                         */
                        if (frameDelta > 0.25) {

                            frameDelta = 0.25;
                        }

                        tickAccumulator +=
                                frameDelta;

                        int ticksThisFrame = 0;

                        while (tickAccumulator
                                        >= GameConfig.TICK_DT
                                && ticksThisFrame
                                        < MAX_TICKS_PER_FRAME) {

                            /*
                             * Tick mới.
                             */
                            clientTick++;

                            /*
                             * Lưu state trước simulation.
                             */
                            beginLocalSimulationTick();

                            /*
                             * Apply input trước physics.
                             */
                            applyInputsForTick(
                                    clientTick);

                            /*
                             * Chạy đúng một
                             * simulation tick.
                             */
                            controller.tick(
                                    mapData);

                            /*
                             * Lưu state sau simulation.
                             */
                            finishLocalSimulationTick();

                            tickAccumulator -=
                                    GameConfig.TICK_DT;

                            ticksThisFrame++;
                        }

                        /*
                         * Nếu render thread quá chậm,
                         * bỏ phần dư để tránh spiral
                         * of death.
                         */
                        if (ticksThisFrame
                                >= MAX_TICKS_PER_FRAME
                                && tickAccumulator
                                >= GameConfig.TICK_DT) {

                            tickAccumulator =
                                    0;
                        }

                        syncLocalVisual();

                        updateRemotePlayers();

                        updateCamera();

                        updateJumpBar();
                    }
                };

        timer.start();
    }
}