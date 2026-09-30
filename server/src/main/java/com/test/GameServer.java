package com.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.test.common.GameConfig;
import com.test.common.PhysicsEngine;
import com.test.common.PlayerState;
import com.test.common.map.MapData;
import com.test.map.MapRepository;

import jakarta.annotation.PostConstruct;

@Component
public class GameServer {

    private final ConcurrentMap<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, PlayerSession> players =
            new ConcurrentHashMap<>();

    private final PhysicsEngine physicsEngine =
            new PhysicsEngine();

    private final MapRepository mapRepository;

    private final RoomManager roomManager =
            new RoomManager();

    public GameServer(
            MapRepository mapRepository) {

        this.mapRepository = mapRepository;
    }

    public MapRepository getMapRepository() {

        return mapRepository;
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

    private void tick() {

        for (Room room
                : roomManager.getRooms()) {

            if (!room.isStarted()) {

                continue;
            }

            MapData map;

            try {

                map =
                        mapRepository.getMap(
                                room.getMapId());

            } catch (IllegalArgumentException e) {

                System.err.println(
                        "Failed to load map for room "
                                + room.getRoomId()
                                + ": "
                                + e.getMessage());

                continue;
            }

            long roomTick =
                    room.incrementTick();

            for (PlayerSession player
                    : room.getPlayers()) {

                player.processQueuedInputs();
            }

            for (PlayerSession player
                    : room.getPlayers()) {

                player.tickCharge();
            }

            for (PlayerSession player
                    : room.getPlayers()) {

                physicsEngine.tick(
                        player.getPlayerState(),
                        map,
                        player.isMovingLeft(),
                        player.isMovingRight());
            }

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

    private void broadcastStates(
            Room room) {

        if (room == null
                || room.isEmpty()) {

            return;
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                "WORLD_STATE");

        message.append("|TICK|");

        message.append(
                room.getCurrentTick());

        for (PlayerSession player
                : room.getPlayers()) {

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
                    state.isChargingUp());

            message.append("|");

            message.append(
                    state.getMaxChargeTimer());

            message.append("|");

            message.append(
                    state.hasSelectedDirection());

            message.append("|");

            message.append(
                    state.getJumpPower());

            message.append("|");

            message.append(
                    state.getFacingDirection());

            message.append("|");

            message.append(
                    player.isMovingLeft());

            message.append("|");

            message.append(
                    player.isMovingRight());
        }

        String finalMessage =
                message.toString();

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