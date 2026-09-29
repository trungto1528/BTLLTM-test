package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.test.common.GameConfig;
import com.test.common.PhysicsEngine;
import com.test.common.PlatformData;
import com.test.common.PlayerState;

import jakarta.annotation.PostConstruct;

@Component
public class GameServer {

    private final ConcurrentMap<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, PlayerSession> players =
            new ConcurrentHashMap<>();

    private final PhysicsEngine physicsEngine =
            new PhysicsEngine();

    private final List<PlatformData> platforms =
            new ArrayList<>();

    /*
     * Logical game tick.
     *
     * Game simulation chạy đúng:
     *
     *     60 tick / second
     *
     * Không phụ thuộc FPS hoặc network rate.
     */
    private long currentTick = 0;

    public GameServer() {
        createMap();
    }

    private void createMap() {

        // =========================
        // FLOOR 2
        // =========================

        platforms.add(
                new PlatformData(
                        100, 520, 250, 20));

        platforms.add(
                new PlatformData(
                        400, 420, 250, 20));

        platforms.add(
                new PlatformData(
                        600, 300, 180, 20));

        platforms.add(
                new PlatformData(
                        300, 180, 200, 20));

        // =========================
        // FLOOR 1
        // =========================

        platforms.add(
                new PlatformData(
                        50, 1050, 250, 20));

        platforms.add(
                new PlatformData(
                        300, 950, 220, 20));

        platforms.add(
                new PlatformData(
                        500, 820, 220, 20));

        platforms.add(
                new PlatformData(
                        650, 700, 120, 20));

        // =========================
        // FLOOR 0
        // =========================

        platforms.add(
                new PlatformData(
                        100, 1650, 250, 20));

        platforms.add(
                new PlatformData(
                        350, 1550, 220, 20));

        platforms.add(
                new PlatformData(
                        550, 1420, 200, 20));

        platforms.add(
                new PlatformData(
                        250, 1300, 180, 20));

        // =========================
        // WALLS
        // =========================

        platforms.add(
                new PlatformData(
                        0, 0, 20, 1800));

        platforms.add(
                new PlatformData(
                        780, 0, 20, 1800));

        // =========================
        // BOTTOM FLOOR
        // =========================

        platforms.add(
                new PlatformData(
                        0, 1780, 800, 20));
    }

    // =========================
    // SESSION
    // =========================

    public void addSession(
            String playerId,
            WebSocketSession session) {

        sessions.put(
                playerId,
                session);
    }

    public void removeSession(
            String playerId) {

        sessions.remove(playerId);
    }

    // =========================
    // PLAYER
    // =========================

    public void addPlayer(
            PlayerSession player) {

        players.put(
                player.getPlayerState().getPlayerId(),
                player);
    }

    public void removePlayer(
            String playerId) {

        players.remove(playerId);
    }

    public PlayerSession getPlayer(
            String playerId) {

        return players.get(playerId);
    }

    public ConcurrentMap<String, PlayerSession> getPlayers() {

        return players;
    }

    /**
     * Logical server tick hiện tại.
     *
     * Dùng cho network snapshot / reconciliation.
     */
    public long getCurrentTick() {

        return currentTick;
    }

    // =========================
    // GAME LOOP
    // =========================

    @PostConstruct
    public void start() {

        Thread gameThread =
                new Thread(() -> {

                    /*
                     * Dùng deadline tuyệt đối thay vì:
                     *
                     *     tick();
                     *     sleep(16.666ms);
                     *
                     * Cách đó sẽ gây drift vì thời gian
                     * thực hiện tick cũng được cộng vào
                     * chu kỳ tiếp theo.
                     */
                    long nextTickTime =
                            System.nanoTime();

                    while (!Thread.currentThread()
                            .isInterrupted()) {

                        nextTickTime +=
                                GameConfig.TICK_NANOS;

                        tick();

                        long sleepNanos =
                                nextTickTime
                                        - System.nanoTime();

                        if (sleepNanos > 0) {

                            try {

                                Thread.sleep(
                                        sleepNanos / 1_000_000,
                                        (int)
                                                (sleepNanos
                                                        % 1_000_000));

                            } catch (InterruptedException e) {

                                Thread.currentThread()
                                        .interrupt();

                                break;
                            }

                        } else {

                            /*
                             * Server đang chậm hơn timeline
                             * thực tế.
                             *
                             * Không chạy bù hàng loạt tick,
                             * tránh tạo vòng lặp quá tải.
                             */
                            nextTickTime =
                                    System.nanoTime();
                        }
                    }

                });

        gameThread.setName(
                "game-server-loop");

        gameThread.setDaemon(true);

        gameThread.start();

        System.out.println(
                "Game server loop started at "
                        + GameConfig.TICK_RATE
                        + " TPS");
    }

    // =========================
    // GAME TICK
    // =========================

    /**
     * Chạy đúng một logical game tick.
     *
     * Thứ tự rất quan trọng:
     *
     * 1. Tăng tick
     * 2. Process input queue
     * 3. Update jump charge
     * 4. Update physics
     * 5. Broadcast snapshot nếu đến thời điểm
     *
     * Tất cả simulation sử dụng:
     *
     *     GameConfig.TICK_DT
     */
    private void tick() {

        currentTick++;

        for (PlayerSession player
                : players.values()) {

            /*
             * =================================================
             * 1. PROCESS INPUT
             * =================================================
             *
             * Input từ WebSocket không được áp dụng
             * ngay khi packet đến.
             *
             * Nó được xử lý tại đây, ở tick boundary.
             */
            player.processQueuedInputs();

            /*
             * =================================================
             * 2. JUMP CHARGE
             * =================================================
             *
             * Charge sử dụng fixed 1/60 second.
             */
            player.tickCharge();

            /*
             * =================================================
             * 3. PHYSICS
             * =================================================
             *
             * PhysicsEngine tự sử dụng:
             *
             *     GameConfig.TICK_DT
             */
            physicsEngine.tick(
                    player.getPlayerState(),
                    platforms,
                    player.isMovingLeft(),
                    player.isMovingRight());
        }

        /*
         * =====================================================
         * NETWORK SNAPSHOT
         * =====================================================
         *
         * Simulation:
         *
         *     60 TPS
         *
         * Network:
         *
         *     20 snapshots / second
         *
         * 60 / 20 = 3
         *
         * => mỗi 3 game tick gửi một snapshot.
         */
        if (currentTick
                % GameConfig.SNAPSHOT_INTERVAL
                == 0) {

            broadcastStates();
        }
    }

    // =========================
    // MULTIPLAYER BROADCAST
    // =========================

    private void broadcastStates() {

        /*
         * Snapshot có server tick để client biết
         * chính xác state thuộc logical tick nào.
         *
         * Format:
         *
         * WORLD_STATE
         * |TICK|123
         *
         * |PLAYER|
         * id
         * sequence
         * x
         * y
         * velocityX
         * velocityY
         * onGround
         * chargingJump
         * chargingUp
         * maxChargeTimer
         * hasSelectedDirection
         * jumpPower
         * facingDirection
         * movingLeft
         * movingRight
         *
         * Các field gameplay state đều được gửi
         * để client có thể reconciliation chính xác.
         */
        StringBuilder message =
                new StringBuilder();

        message.append(
                "WORLD_STATE");

        message.append("|TICK|");

        message.append(
                currentTick);

        /*
         * Tạo snapshot của tất cả player.
         */
        for (PlayerSession player
                : players.values()) {

            PlayerState state =
                    player.getPlayerState();

            message.append("|PLAYER|");

            /*
             * playerId
             */
            message.append(
                    state.getPlayerId());

            message.append("|");

            /*
             * lastProcessedInput
             *
             * Client dùng sequence này để
             * reconciliation.
             */
            message.append(
                    player.getLastProcessedInput());

            message.append("|");

            /*
             * Position
             */
            message.append(
                    state.getX());

            message.append("|");

            message.append(
                    state.getY());

            message.append("|");

            /*
             * Velocity
             */
            message.append(
                    state.getVelocityX());

            message.append("|");

            message.append(
                    state.getVelocityY());

            message.append("|");

            /*
             * Ground state
             */
            message.append(
                    state.isOnGround());

            message.append("|");

            /*
             * Jump charge state
             */
            message.append(
                    state.isChargingJump());

            message.append("|");

            /*
             * Charge direction:
             *
             * true  = đang tăng jumpPower
             * false = đang giữ MAX / giảm
             */
            message.append(
                    state.isChargingUp());

            message.append("|");

            /*
             * Thời gian đã giữ MAX.
             */
            message.append(
                    state.getMaxChargeTimer());

            message.append("|");

            /*
             * Người chơi đã chọn hướng A/D
             * trong cú jump hiện tại chưa.
             */
            message.append(
                    state.hasSelectedDirection());

            message.append("|");

            /*
             * Current jump power.
             */
            message.append(
                    state.getJumpPower());

            message.append("|");

            /*
             * Facing direction.
             */
            message.append(
                    state.getFacingDirection());

            message.append("|");

            /*
             * Current movement input state.
             *
             * Đây là state cần thiết cho
             * reconciliation chính xác.
             */
            message.append(
                    player.isMovingLeft());

            message.append("|");

            message.append(
                    player.isMovingRight());
        }

        String finalMessage =
                message.toString();

        /*
         * Gửi cùng một authoritative snapshot
         * cho tất cả client.
         */
        for (WebSocketSession session
                : sessions.values()) {

            if (session == null
                    || !session.isOpen()) {

                continue;
            }

            try {

                session.sendMessage(
                        new TextMessage(
                                finalMessage));

            } catch (Exception e) {

                System.err.println(
                        "Failed to broadcast state: "
                                + e.getMessage());
            }
        }
    }
}