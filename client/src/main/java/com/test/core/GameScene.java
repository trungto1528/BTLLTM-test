package com.test.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.test.common.GameConfig;
import com.test.common.InputCommand;
import com.test.common.PlatformData;
import com.test.common.PlayerState;

import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class GameScene extends Pane {

    private final GameWebSocketClient network;

    private int inputSequence = 0;

    // =========================
    // VIEWPORT
    // =========================

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    // =========================
    // MAP
    // =========================

    private static final double MAP_WIDTH = 800;
    private static final double MAP_HEIGHT = 1800;

    private static final double FLOOR_HEIGHT = 600;

    // =========================
    // WORLD
    // =========================

    private final Pane world =
            new Pane();

    private final Player player;

    private final List<Platform> platforms =
            new ArrayList<>();

    // =========================
    // JUMP POWER BAR
    // =========================

    private final Rectangle jumpBarBackground =
            new Rectangle(40, 6);

    private final Rectangle jumpBarFill =
            new Rectangle(0, 6);

    // =========================
    // CAMERA
    // =========================

    private double cameraY = 1200;

    private int currentFloor = 0;

    // =========================
    // PHYSICS
    // =========================

    private final List<PlatformData> physicsPlatforms =
            new ArrayList<>();

    private final ClientPlayerController controller;

    // =========================
    // LOCAL RENDER INTERPOLATION
    // =========================
    //
    // Simulation chạy fixed 60 TPS.
    //
    // Không render trực tiếp state hiện tại
    // vì như vậy khi FPS > 60 sẽ dễ nhìn thấy
    // chuyển động theo từng bước simulation.
    //
    // Render sẽ nội suy:
    //
    // previous simulation state
    //          ↓
    //       alpha
    //          ↓
    // current simulation state
    //
    // alpha = tickAccumulator / TICK_DT
    //
    // =========================

    private double previousLocalX;
    private double previousLocalY;

    private double currentLocalX;
    private double currentLocalY;

    private boolean localRenderInitialized;

    // =========================
    // REMOTE PLAYERS
    // =========================

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

    // =========================
    // NETWORK
    // =========================

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

    // =========================
    // KEY STATE
    // =========================

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean spacePressed;

    // =========================
    // CONSTRUCTOR
    // =========================

    public GameScene(
            GameWebSocketClient network) {

        this.network = network;

        setPrefSize(
                VIEW_WIDTH,
                VIEW_HEIGHT);

        // =========================
        // WORLD
        // =========================

        world.setPrefSize(
                MAP_WIDTH,
                MAP_HEIGHT);

        getChildren().add(
                world);

        // =========================
        // PLAYER
        // =========================

        player =
                new Player(
                        180,
                        1620);

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

        // =========================
        // JUMP POWER BAR
        // =========================

        jumpBarBackground.setFill(
                Color.GRAY);

        jumpBarFill.setFill(
                Color.ORANGE);

        world.getChildren().add(
                jumpBarBackground);

        world.getChildren().add(
                jumpBarFill);

        // =========================
        // MAP
        // =========================

        createMap();

        // =========================
        // INPUT
        // =========================

        setupInput();

        // =========================
        // CAMERA
        // =========================

        updateCamera();

        // =========================
        // FOCUS
        // =========================

        javafx.application.Platform.runLater(
                this::requestFocus);
    }

    // =====================================================
    // LOCAL PLAYER ID
    // =====================================================

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

    // =====================================================
    // INPUT SEQUENCE
    // =====================================================

    private int nextInputSequence() {

        return ++inputSequence;
    }

    // =====================================================
    // WORLD STATE
    // =====================================================

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

    // =====================================================
    // SNAPSHOT DATA
    // =====================================================

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

    // =====================================================
    // LOCAL RECONCILIATION
    // =====================================================

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
                    physicsPlatforms);
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

    // =====================================================
    // RESET LOCAL RENDER INTERPOLATION
    // =====================================================

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

    // =====================================================
    // APPLY PENDING INPUT FOR CURRENT TICK
    // =====================================================

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

    // =====================================================
    // SIMULATION STATE BUFFER
    // =====================================================

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

    // =====================================================
    // REMOTE PLAYER
    // =====================================================

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

    // =====================================================
    // REMOTE INTERPOLATION
    // =====================================================

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

    // =====================================================
    // CREATE AND QUEUE INPUT
    // =====================================================

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

    // =====================================================
    // SEND NORMAL INPUT
    // =====================================================

    private int sendInput(
            String action) {

        return queueInput(action);
    }

    // =====================================================
    // SEND JUMP RELEASE
    // =====================================================

    private int sendJumpRelease() {

        /*
         * Server tự lấy jumpPower authoritative.
         */
        return queueInput(
                "JUMP_RELEASE");
    }

    // =====================================================
    // PLAYER STATE
    // =====================================================

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

    // =====================================================
    // SYNC LOCAL VISUAL
    // =====================================================

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

    // =====================================================
    // JUMP BAR
    // =====================================================

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

    // =====================================================
    // CREATE MAP
    // =====================================================

    private void createMap() {

        // =========================
        // TẦNG 2
        // =========================

        addPlatform(
                100,
                520,
                250,
                20);

        addPlatform(
                400,
                420,
                250,
                20);

        addPlatform(
                600,
                300,
                180,
                20);

        addPlatform(
                300,
                180,
                200,
                20);

        // =========================
        // TẦNG 1
        // =========================

        addPlatform(
                50,
                1050,
                250,
                20);

        addPlatform(
                300,
                950,
                220,
                20);

        addPlatform(
                500,
                820,
                220,
                20);

        addPlatform(
                650,
                700,
                120,
                20);

        // =========================
        // TẦNG 0
        // =========================

        addPlatform(
                100,
                1650,
                250,
                20);

        addPlatform(
                350,
                1550,
                220,
                20);

        addPlatform(
                550,
                1420,
                200,
                20);

        addPlatform(
                250,
                1300,
                180,
                20);

        // =========================
        // WALL LEFT
        // =========================

        addPlatform(
                0,
                0,
                20,
                MAP_HEIGHT);

        // =========================
        // WALL RIGHT
        // =========================

        addPlatform(
                MAP_WIDTH - 20,
                0,
                20,
                MAP_HEIGHT);

        // =========================
        // FLOOR
        // =========================

        addPlatform(
                0,
                MAP_HEIGHT - 20,
                MAP_WIDTH,
                20);
    }

    // =====================================================
    // ADD PLATFORM
    // =====================================================

    private void addPlatform(
            double x,
            double y,
            double width,
            double height) {

        Platform platform =
                new Platform(
                        x,
                        y,
                        width,
                        height);

        platforms.add(
                platform);

        physicsPlatforms.add(
                new PlatformData(
                        x,
                        y,
                        width,
                        height));

        world.getChildren().add(
                platform);
    }

    // =====================================================
    // CAMERA
    // =====================================================

    private void updateCamera() {

        int newFloor =
                2
                        - (int) (
                                player.getY()
                                        / FLOOR_HEIGHT);

        if (newFloor < 0) {
            newFloor = 0;
        }

        if (newFloor > 2) {
            newFloor = 2;
        }

        currentFloor =
                newFloor;

        cameraY =
                (2 - currentFloor)
                        * FLOOR_HEIGHT;

        world.setTranslateX(0);

        world.setTranslateY(
                -cameraY);
    }

    // =====================================================
    // INPUT
    // =====================================================

    private void setupInput() {

        setFocusTraversable(true);

        setOnKeyPressed(event -> {

            // =========================
            // LEFT
            // =========================

            if (event.getCode() == KeyCode.A
                    || event.getCode()
                    == KeyCode.LEFT) {

                if (!leftPressed) {

                    leftPressed = true;

                    sendInput(
                            "LEFT_PRESS");
                }
            }

            // =========================
            // RIGHT
            // =========================

            if (event.getCode() == KeyCode.D
                    || event.getCode()
                    == KeyCode.RIGHT) {

                if (!rightPressed) {

                    rightPressed = true;

                    sendInput(
                            "RIGHT_PRESS");
                }
            }

            // =========================
            // JUMP
            // =========================

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

            // =========================
            // LEFT
            // =========================

            if (event.getCode() == KeyCode.A
                    || event.getCode()
                    == KeyCode.LEFT) {

                if (leftPressed) {

                    leftPressed = false;

                    sendInput(
                            "LEFT_RELEASE");
                }
            }

            // =========================
            // RIGHT
            // =========================

            if (event.getCode() == KeyCode.D
                    || event.getCode()
                    == KeyCode.RIGHT) {

                if (rightPressed) {

                    rightPressed = false;

                    sendInput(
                            "RIGHT_RELEASE");
                }
            }

            // =========================
            // JUMP RELEASE
            // =========================

            if (event.getCode()
                    == KeyCode.SPACE) {

                if (spacePressed) {

                    spacePressed = false;

                    sendJumpRelease();
                }
            }
        });
    }

    // =====================================================
    // FIXED GAME LOOP
    // =====================================================

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

                        // =========================
                        // FIXED 60 TPS
                        // =========================

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
                                    physicsPlatforms);

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

                        // =========================
                        // LOCAL RENDER
                        // =========================

                        syncLocalVisual();

                        // =========================
                        // REMOTE RENDER
                        // =========================

                        updateRemotePlayers();

                        // =========================
                        // CAMERA
                        // =========================

                        updateCamera();

                        // =========================
                        // JUMP BAR
                        // =========================

                        updateJumpBar();
                    }
                };

        timer.start();
    }
}