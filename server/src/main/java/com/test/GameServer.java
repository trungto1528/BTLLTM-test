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
     * Quản lý toàn bộ room.
     */
    private final RoomManager roomManager =
            new RoomManager();

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

    // =====================================================
    // ROOM MANAGER
    // =====================================================

    public RoomManager getRoomManager() {

        return roomManager;
    }

    // =====================================================
    // SESSION
    // =====================================================

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

    public WebSocketSession getSession(
            String playerId) {

        return sessions.get(playerId);
    }

    // =====================================================
    // PLAYER
    // =====================================================

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

    // =====================================================
    // GAME LOOP
    // =====================================================

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
                     * Cách đó sẽ gây drift.
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
                             * Không chạy bù hàng loạt tick.
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

    // =====================================================
    // GAME TICK
    // =====================================================

    /**
     * Chạy một logical game tick.
     *
     * Quan trọng:
     *
     * Không còn xử lý toàn bộ players global.
     *
     * Thay vào đó:
     *
     *     GameServer
     *          |
     *          +-- Room A
     *          |     +-- Player
     *          |     +-- Player
     *          |
     *          +-- Room B
     *                +-- Player
     *
     * Chỉ room đã STARTED mới được simulation.
     */
    private void tick() {

        /*
         * Một global 60 TPS loop duy nhất.
         *
         * Không tạo thread riêng cho từng room.
         */
        for (Room room
                : roomManager.getRooms()) {

            if (!room.isStarted()) {

                continue;
            }

            /*
             * Room có logical timeline riêng.
             *
             * Room mới START_GAME sẽ bắt đầu:
             *
             *     tick = 1
             *
             * ở game tick đầu tiên.
             */
            long roomTick =
                    room.incrementTick();

            /*
             * =================================================
             * 1. PROCESS INPUT
             * =================================================
             */
            for (PlayerSession player
                    : room.getPlayers()) {

                player.processQueuedInputs();
            }

            /*
             * =================================================
             * 2. JUMP CHARGE
             * =================================================
             */
            for (PlayerSession player
                    : room.getPlayers()) {

                player.tickCharge();
            }

            /*
             * =================================================
             * 3. PHYSICS
             * =================================================
             *
             * Giữ nguyên PhysicsEngine hiện tại.
             */
            for (PlayerSession player
                    : room.getPlayers()) {

                physicsEngine.tick(
                        player.getPlayerState(),
                        platforms,
                        player.isMovingLeft(),
                        player.isMovingRight());
            }

            /*
             * =================================================
             * 4. NETWORK SNAPSHOT
             * =================================================
             *
             * 60 TPS simulation
             * 20 snapshots / second
             *
             * => mỗi 3 tick gửi snapshot.
             */
            if (roomTick
                    % GameConfig.SNAPSHOT_INTERVAL
                    == 0) {

                broadcastStates(room);
            }
        }
    }

    // =====================================================
    // MULTIPLAYER BROADCAST
    // =====================================================

    /**
     * Broadcast authoritative snapshot của MỘT room.
     *
     * Tuyệt đối không gửi player của room này
     * sang room khác.
     */
    private void broadcastStates(
            Room room) {

        /*
         * Không broadcast room đã empty.
         */
        if (room == null
                || room.isEmpty()) {

            return;
        }

        /*
         * Snapshot format:
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
         */
        StringBuilder message =
                new StringBuilder();

        message.append(
                "WORLD_STATE");

        message.append("|TICK|");

        message.append(
                room.getCurrentTick());

        /*
         * Chỉ lấy player thuộc room này.
         */
        for (PlayerSession player
                : room.getPlayers()) {

            PlayerState state =
                    player.getPlayerState();

            message.append("|PLAYER|");

            // playerId
            message.append(
                    state.getPlayerId());

            message.append("|");

            /*
             * lastProcessedInput
             *
             * Client dùng cho reconciliation.
             */
            message.append(
                    player.getLastProcessedInput());

            message.append("|");

            // Position
            message.append(
                    state.getX());

            message.append("|");

            message.append(
                    state.getY());

            message.append("|");

            // Velocity
            message.append(
                    state.getVelocityX());

            message.append("|");

            message.append(
                    state.getVelocityY());

            message.append("|");

            // Ground state
            message.append(
                    state.isOnGround());

            message.append("|");

            // Jump charge state
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

            // Thời gian đã giữ MAX
            message.append(
                    state.getMaxChargeTimer());

            message.append("|");

            /*
             * Người chơi đã chọn hướng
             * trong cú jump hiện tại chưa.
             */
            message.append(
                    state.hasSelectedDirection());

            message.append("|");

            // Current jump power
            message.append(
                    state.getJumpPower());

            message.append("|");

            // Facing direction
            message.append(
                    state.getFacingDirection());

            message.append("|");

            /*
             * Movement input state.
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
         * =====================================================
         * SEND ONLY TO THIS ROOM
         * =====================================================
         */
        for (PlayerSession player
                : room.getPlayers()) {

            String playerId =
                    player.getPlayerState()
                            .getPlayerId();

            WebSocketSession session =
                    sessions.get(playerId);

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
                        "Failed to send state to "
                                + playerId
                                + ": "
                                + e.getMessage());
            }
        }
    }
}