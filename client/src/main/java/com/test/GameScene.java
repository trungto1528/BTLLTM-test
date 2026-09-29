package com.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.test.common.PhysicsEngine;
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

        private final Pane world = new Pane();

        private final Player player;

        private final List<Platform> platforms = new ArrayList<>();

        // =========================
        // JUMP POWER BAR
        // =========================

        private final Rectangle jumpBarBackground = new Rectangle(40, 6);

        private final Rectangle jumpBarFill = new Rectangle(0, 6);

        // =========================
        // CAMERA
        // =========================

        private double cameraY = 1200;

        private int currentFloor = 0;

        // =========================
        // NETWORK / PHYSICS
        // =========================

        private final List<InputCommand> pendingInputs = new ArrayList<>();

        private final List<PlatformData> physicsPlatforms = new ArrayList<>();

        private final ClientPlayerController controller;
        private final Map<String, Player> remotePlayers = new HashMap<>();

        private static class RemoteState {

                PlayerState state;

                double serverX;
                double serverY;

                boolean initialized;
        }

        private final Map<String, RemoteState> remoteStates = new HashMap<>();

        private String localPlayerId;

        public void setLocalPlayerId(String playerId) {

                this.localPlayerId = playerId;

                System.out.println(
                                "Local player ID: " + playerId);
        }

        /*
         * Lưu sequence cuối cùng server đã ACK.
         *
         * Tạm thời chưa dùng để rollback player.
         * Prediction client vẫn chạy liên tục.
         */
        private int lastServerSequence = 0;

        // =========================
        // CONSTRUCTOR
        // =========================

        public GameScene(GameWebSocketClient network) {

                this.network = network;

                setPrefSize(
                                VIEW_WIDTH,
                                VIEW_HEIGHT);

                /*
                 * WORLD
                 */

                world.setPrefSize(
                                MAP_WIDTH,
                                MAP_HEIGHT);

                getChildren().add(world);

                /*
                 * PLAYER
                 */

                player = new Player(
                                180,
                                1620);

                controller = new ClientPlayerController(
                                "local-player");

                controller.getState().setX(
                                player.getX());

                controller.getState().setY(
                                player.getY());

                controller.getState().setOnGround(true);

                world.getChildren().add(player);

                /*
                 * JUMP POWER BAR
                 */

                jumpBarBackground.setFill(Color.GRAY);
                jumpBarFill.setFill(Color.ORANGE);

                world.getChildren().add(
                                jumpBarBackground);

                world.getChildren().add(
                                jumpBarFill);

                /*
                 * MAP
                 */

                createMap();

                /*
                 * INPUT
                 */

                setupInput();

                /*
                 * CAMERA
                 */

                updateCamera();

                /*
                 * FOCUS
                 */

                javafx.application.Platform.runLater(
                                this::requestFocus);
        }

        // =====================================================
        // INPUT SEQUENCE
        // =====================================================

        private int nextInputSequence() {

                return ++inputSequence;
        }

        public void handleWorldState(String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 2) {
                        return;
                }

                int index = 1;

                while (index < parts.length) {

                        if (!parts[index].equals("PLAYER")) {
                                index++;
                                continue;
                        }

                        if (index + 10 >= parts.length) {
                                break;
                        }

                        try {

                                String playerId = parts[index + 1];

                                int sequence = Integer.parseInt(parts[index + 2]);

                                double x = Double.parseDouble(parts[index + 3]);

                                double y = Double.parseDouble(parts[index + 4]);

                                double velocityX = Double.parseDouble(parts[index + 5]);

                                double velocityY = Double.parseDouble(parts[index + 6]);

                                boolean onGround = Boolean.parseBoolean(parts[index + 7]);

                                boolean chargingJump = Boolean.parseBoolean(parts[index + 8]);

                                double jumpPower = Double.parseDouble(parts[index + 9]);

                                int facingDirection = Integer.parseInt(parts[index + 10]);

                                /*
                                 * Player của chính mình.
                                 *
                                 * Không lấy x/y server để overwrite
                                 * prediction local.
                                 */
                                if (playerId.equals(localPlayerId)) {

                                        lastServerSequence = Math.max(
                                                        lastServerSequence,
                                                        sequence);

                                        pendingInputs.removeIf(
                                                        input -> input.getSequence() <= lastServerSequence);

                                } else {

                                        javafx.application.Platform.runLater(() -> {

                                                updateRemotePlayer(
                                                                playerId,
                                                                x,
                                                                y,
                                                                velocityX,
                                                                velocityY,
                                                                onGround,
                                                                chargingJump,
                                                                jumpPower,
                                                                facingDirection);
                                        });
                                }

                                index += 11;

                        } catch (Exception e) {

                                System.err.println(
                                                "Invalid WORLD_STATE player: "
                                                                + e.getMessage());

                                break;
                        }
                }
        }

        private void updateRemotePlayer(
                        String playerId,
                        double x,
                        double y,
                        double velocityX,
                        double velocityY,
                        boolean onGround,
                        boolean chargingJump,
                        double jumpPower,
                        int facingDirection) {

                Player remote = remotePlayers.get(playerId);

                if (remote == null) {
                        remote = new Player(x, y);

                        remotePlayers.put(playerId, remote);
                        world.getChildren().add(remote);
                }

                RemoteState remoteState = remoteStates.computeIfAbsent(
                                playerId,
                                id -> {
                                        RemoteState state = new RemoteState();
                                        state.state = new PlayerState(id);
                                        return state;
                                });

                PlayerState state = remoteState.state;

                state.setX(x);
                state.setY(y);

                state.setVelocityX(velocityX);
                state.setVelocityY(velocityY);

                state.setOnGround(onGround);
                state.setChargingJump(false);

                state.setJumpPower(0);
                state.setFacingDirection(facingDirection);

                remoteState.serverX = x;
                remoteState.serverY = y;

                if (!remoteState.initialized) {
                        remote.setX(x);
                        remote.setY(y);

                        remoteState.initialized = true;
                }

                remote.setOnGround(onGround);
        }

        private void updateRemotePlayers(double deltaTime) {

                for (Map.Entry<String, Player> entry : remotePlayers.entrySet()) {

                        String playerId = entry.getKey();
                        Player remote = entry.getValue();

                        RemoteState remoteState = remoteStates.get(playerId);

                        if (remoteState == null
                                        || !remoteState.initialized) {
                                continue;
                        }

                        PlayerState state = remoteState.state;

                        /*
                         * =========================================
                         * 1. CLIENT-SIDE PHYSICS
                         * =========================================
                         *
                         * Remote player tự chạy physics ở client.
                         *
                         * Không dùng serverX/serverY để set trực tiếp
                         * mỗi frame.
                         */

                        PhysicsEngine.updateRemotePlayer(
                                        state,
                                        physicsPlatforms,
                                        deltaTime);

                        /*
                         * =========================================
                         * 2. SERVER CORRECTION
                         * =========================================
                         */

                        double predictedX = state.getX();
                        double predictedY = state.getY();

                        double errorX = remoteState.serverX - predictedX;

                        double errorY = remoteState.serverY - predictedY;

                        /*
                         * Sai số nhỏ:
                         *
                         * Không kéo quá mạnh.
                         *
                         * Nếu correction quá lớn ở đây,
                         * remote sẽ có cảm giác rung.
                         */
                        double correctionStrength = 0.04;

                        if (Math.abs(errorX) < 80) {
                                predictedX += errorX * correctionStrength;
                        }

                        if (Math.abs(errorY) < 80) {
                                predictedY += errorY * correctionStrength;
                        }

                        /*
                         * =========================================
                         * 3. DESYNC QUÁ LỚN
                         * =========================================
                         *
                         * Nếu remote lệch rất xa,
                         * server phải thắng.
                         */

                        if (Math.abs(errorX) >= 80
                                        || Math.abs(errorY) >= 80) {

                                predictedX = remoteState.serverX;
                                predictedY = remoteState.serverY;

                                state.setVelocityX(
                                                state.getVelocityX());

                                state.setVelocityY(
                                                state.getVelocityY());
                        }

                        /*
                         * =========================================
                         * 4. APPLY VISUAL
                         * =========================================
                         */

                        state.setX(predictedX);
                        state.setY(predictedY);

                        remote.setX(predictedX);
                        remote.setY(predictedY);

                        remote.setOnGround(
                                        state.isOnGround());
                }
        }

        // =====================================================
        // SEND NORMAL INPUT
        // =====================================================

        private int sendInput(String action) {

                int sequence = nextInputSequence();

                pendingInputs.add(
                                new InputCommand(
                                                sequence,
                                                action));

                network.sendInput(
                                "INPUT|"
                                                + sequence
                                                + "|"
                                                + action);

                return sequence;
        }

        // =====================================================
        // SEND JUMP RELEASE
        // =====================================================

        private int sendJumpRelease(
                        double jumpPower) {

                int sequence = nextInputSequence();

                pendingInputs.add(
                                new InputCommand(
                                                sequence,
                                                "JUMP_RELEASE",
                                                jumpPower));

                network.sendInput(
                                "INPUT|"
                                                + sequence
                                                + "|JUMP_RELEASE");

                return sequence;
        }

        // =====================================================
        // SERVER STATE
        // =====================================================

        public synchronized void handlePlayerState(
                        String message) {

                String[] parts = message.split("\\|");

                if (parts.length < 7) {
                        return;
                }

                try {

                        int serverSequence = Integer.parseInt(parts[2]);

                        /*
                         * Server vẫn ACK input.
                         *
                         * Ta chỉ xóa những input mà server
                         * đã xử lý.
                         */
                        if (serverSequence > lastServerSequence) {

                                lastServerSequence = serverSequence;

                                pendingInputs.removeIf(
                                                input -> input.getSequence() <= serverSequence);
                        }

                        /*
                         * QUAN TRỌNG:
                         *
                         * Không set X/Y của controller
                         * về server snapshot ở đây.
                         *
                         * Client prediction vẫn được phép
                         * chạy liên tục trong AnimationTimer.
                         *
                         * Nếu set X/Y tại đây thì server snapshot
                         * 20Hz sẽ kéo player ngược lại.
                         */

                } catch (Exception e) {

                        System.err.println(
                                        "Invalid PLAYER_STATE: "
                                                        + message);

                        e.printStackTrace();
                }
        }

        // =====================================================
        // JUMP BAR
        // =====================================================

        private void updateJumpBar() {

                double barWidth = 40;

                double barX = player.getX()
                                + player.getWidth() / 2
                                - barWidth / 2;

                double barY = player.getY() - 12;

                jumpBarBackground.setX(barX);
                jumpBarBackground.setY(barY);

                double ratio = player.getJumpPowerRatio();

                jumpBarFill.setX(barX);
                jumpBarFill.setY(barY);

                jumpBarFill.setWidth(
                                barWidth * ratio);
        }

        // =====================================================
        // CREATE MAP
        // =====================================================

        private void createMap() {

                /*
                 * TẦNG 2
                 */

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

                /*
                 * TẦNG 1
                 */

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

                /*
                 * TẦNG 0
                 */

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

                /*
                 * TƯỜNG TRÁI
                 */

                addPlatform(
                                0,
                                0,
                                20,
                                MAP_HEIGHT);

                /*
                 * TƯỜNG PHẢI
                 */

                addPlatform(
                                MAP_WIDTH - 20,
                                0,
                                20,
                                MAP_HEIGHT);

                /*
                 * SÀN ĐÁY
                 */

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

                Platform platform = new Platform(
                                x,
                                y,
                                width,
                                height);

                platforms.add(platform);

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

                int newFloor = 2
                                - (int) (player.getY()
                                                / FLOOR_HEIGHT);

                if (newFloor < 0) {
                        newFloor = 0;
                }

                if (newFloor > 2) {
                        newFloor = 2;
                }

                currentFloor = newFloor;

                cameraY = (2 - currentFloor)
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

                        /*
                         * LEFT
                         */

                        if (event.getCode() == KeyCode.A
                                        || event.getCode() == KeyCode.LEFT) {

                                player.setMovingLeft(true);

                                controller.setMovingLeft(true);

                                sendInput(
                                                "LEFT_PRESS");
                        }

                        /*
                         * RIGHT
                         */

                        if (event.getCode() == KeyCode.D
                                        || event.getCode() == KeyCode.RIGHT) {

                                player.setMovingRight(true);

                                controller.setMovingRight(true);

                                sendInput(
                                                "RIGHT_PRESS");
                        }

                        /*
                         * JUMP
                         */

                        if (event.getCode() == KeyCode.SPACE) {

                                player.startCharging();

                                controller.startJump();

                                sendInput(
                                                "JUMP_START");
                        }
                });

                setOnKeyReleased(event -> {

                        /*
                         * LEFT
                         */

                        if (event.getCode() == KeyCode.A
                                        || event.getCode() == KeyCode.LEFT) {

                                player.setMovingLeft(false);

                                controller.setMovingLeft(false);

                                sendInput(
                                                "LEFT_RELEASE");
                        }

                        /*
                         * RIGHT
                         */

                        if (event.getCode() == KeyCode.D
                                        || event.getCode() == KeyCode.RIGHT) {

                                player.setMovingRight(false);

                                controller.setMovingRight(false);

                                sendInput(
                                                "RIGHT_RELEASE");
                        }

                        /*
                         * JUMP RELEASE
                         */

                        if (event.getCode() == KeyCode.SPACE) {

                                double jumpPower = player.getJumpPower();

                                player.releaseJump();

                                controller.releaseJump(
                                                jumpPower);

                                sendJumpRelease(
                                                jumpPower);
                        }
                });
        }

        // =====================================================
        // GAME LOOP
        // =====================================================

        protected void startLoop() {

                AnimationTimer timer = new AnimationTimer() {

                        private long lastTime = 0;

                        @Override
                        public void handle(
                                        long now) {

                                if (lastTime == 0) {

                                        lastTime = now;

                                        return;
                                }

                                double deltaTime = (now - lastTime)
                                                / 1_000_000_000.0;

                                if (deltaTime > 0.05) {
                                        deltaTime = 0.05;
                                }

                                lastTime = now;

                                /*
                                 * =========================
                                 * CHARGE JUMP
                                 * =========================
                                 *
                                 * Giữ nguyên Player.java
                                 * để thanh lực hoạt động.
                                 */

                                player.chargeJump(
                                                deltaTime);

                                /*
                                 * =========================
                                 * CLIENT PREDICTION
                                 * =========================
                                 *
                                 * Đây là nơi block thực sự
                                 * được di chuyển mỗi frame.
                                 */

                                controller.update(
                                                deltaTime,
                                                physicsPlatforms);

                                /*
                                 * =========================
                                 * SYNC PLAYER VISUAL
                                 * =========================
                                 */

                                player.setX(
                                                controller
                                                                .getState()
                                                                .getX());

                                player.setY(
                                                controller
                                                                .getState()
                                                                .getY());

                                player.setOnGround(
                                                controller
                                                                .getState()
                                                                .isOnGround());
                                updateRemotePlayers(deltaTime);
                                /*
                                 * =========================
                                 * CAMERA
                                 * =========================
                                 */

                                updateCamera();

                                /*
                                 * =========================
                                 * JUMP BAR
                                 * =========================
                                 */

                                updateJumpBar();
                        }
                };

                timer.start();
        }
}