package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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

    private double snapshotTimer = 0;

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

    // =========================
    // GAME LOOP
    // =========================

    @PostConstruct
    public void start() {

        Thread gameThread =
                new Thread(() -> {

                    final double deltaTime =
                            1.0 / 60.0;

                    while (true) {

                        long start =
                                System.nanoTime();

                        update(deltaTime);

                        long elapsed =
                                System.nanoTime()
                                        - start;

                        long sleepNanos =
                                16_666_667L
                                        - elapsed;

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
                        }
                    }

                });

        gameThread.setName(
                "game-server-loop");

        gameThread.setDaemon(true);

        gameThread.start();

        System.out.println(
                "Game server loop started");
    }

    // =========================
    // UPDATE
    // =========================

    private void update(
            double deltaTime) {

        for (PlayerSession player
                : players.values()) {

            player.updateCharge(
                    deltaTime);

            physicsEngine.update(
                    player.getPlayerState(),
                    deltaTime,
                    platforms,
                    player.isMovingLeft(),
                    player.isMovingRight());
        }

        snapshotTimer += deltaTime;

        if (snapshotTimer
                >= 1.0 / 20.0) {

            snapshotTimer = 0;

            broadcastStates();
        }
    }

    // =========================
    // MULTIPLAYER BROADCAST
    // =========================

    private void broadcastStates() {

        /*
         * Tạo snapshot của TẤT CẢ player.
         */
        StringBuilder message =
                new StringBuilder();

        message.append(
                "WORLD_STATE");

        for (PlayerSession player
                : players.values()) {

            PlayerState state =
                    player.getPlayerState();

            message.append("|PLAYER|");

            message.append(
                    state.getPlayerId());

            message.append("|");

            message.append(
                    player.getLastProcessedInput());

            message.append("|");

            message.append(
                    state.getX());

            message.append("|");

            message.append(
                    state.getY());

            message.append("|");

            message.append(
                    state.getVelocityX());

            message.append("|");

            message.append(
                    state.getVelocityY());

            message.append("|");

            message.append(
                    state.isOnGround());

            message.append("|");

            message.append(
                    state.isChargingJump());

            message.append("|");

            message.append(
                    state.getJumpPower());

            message.append("|");

            message.append(
                    state.getFacingDirection());
        }

        String finalMessage =
                message.toString();

        /*
         * Gửi cùng một snapshot
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