package com.test;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Room {

    public static final int DEFAULT_MAX_PLAYERS = 4;
    public static final String DEFAULT_MAP_ID = "map01";

    private final String roomId;
    private final int maxPlayers;

    private volatile String mapId;

    private final ConcurrentMap<String, PlayerSession> players =
            new ConcurrentHashMap<>();

    private volatile String hostPlayerId;
    private volatile boolean started;
    private long currentTick;

    public Room(String roomId) {

        this(
                roomId,
                DEFAULT_MAX_PLAYERS,
                DEFAULT_MAP_ID);
    }

    public Room(
            String roomId,
            int maxPlayers,
            String mapId) {

        if (roomId == null
                || roomId.isBlank()) {

            throw new IllegalArgumentException(
                    "roomId must not be blank");
        }

        if (maxPlayers <= 0) {

            throw new IllegalArgumentException(
                    "maxPlayers must be greater than 0");
        }

        if (mapId == null
                || mapId.isBlank()) {

            throw new IllegalArgumentException(
                    "mapId must not be blank");
        }

        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.mapId = mapId;

        this.hostPlayerId = null;
        this.started = false;
        this.currentTick = 0;
    }

    // =====================================================
    // ROOM INFO
    // =====================================================

    public String getRoomId() {

        return roomId;
    }

    public int getMaxPlayers() {

        return maxPlayers;
    }

    public String getMapId() {

        return mapId;
    }

    public int getPlayerCount() {

        return players.size();
    }

    public boolean isEmpty() {

        return players.isEmpty();
    }

    public boolean isFull() {

        return players.size() >= maxPlayers;
    }

    public boolean isStarted() {

        return started;
    }

    public long getCurrentTick() {

        return currentTick;
    }

    // =====================================================
    // MAP
    // =====================================================

    public synchronized boolean setMapId(
            String mapId) {

        if (started) {

            return false;
        }

        if (mapId == null
                || mapId.isBlank()) {

            return false;
        }

        this.mapId = mapId;

        return true;
    }

    // =====================================================
    // HOST
    // =====================================================

    public String getHostPlayerId() {

        return hostPlayerId;
    }

    public boolean isHost(String playerId) {

        if (playerId == null) {

            return false;
        }

        return playerId.equals(hostPlayerId);
    }

    public synchronized void setHostPlayerId(
            String playerId) {

        if (playerId == null
                || !players.containsKey(playerId)) {

            throw new IllegalArgumentException(
                    "Host must be a player in this room");
        }

        hostPlayerId = playerId;
    }

    // =====================================================
    // PLAYERS
    // =====================================================

    public PlayerSession getPlayer(
            String playerId) {

        if (playerId == null) {

            return null;
        }

        return players.get(playerId);
    }

    public Collection<PlayerSession> getPlayers() {

        return players.values();
    }

    public synchronized boolean addPlayer(
            PlayerSession player) {

        if (player == null) {

            return false;
        }

        if (started) {

            return false;
        }

        if (isFull()) {

            return false;
        }

        String playerId =
                player.getPlayerState()
                        .getPlayerId();

        if (playerId == null
                || playerId.isBlank()) {

            return false;
        }

        if (players.containsKey(playerId)) {

            return false;
        }

        players.put(
                playerId,
                player);

        if (hostPlayerId == null) {

            hostPlayerId = playerId;
        }

        return true;
    }

    public synchronized PlayerSession removePlayer(
            String playerId) {

        if (playerId == null) {

            return null;
        }

        PlayerSession removed =
                players.remove(playerId);

        if (playerId.equals(hostPlayerId)) {

            hostPlayerId = null;
        }

        return removed;
    }

    // =====================================================
    // GAME STATE
    // =====================================================

    public synchronized boolean startGame() {

        if (started) {

            return false;
        }

        if (players.isEmpty()) {

            return false;
        }

        started = true;
        currentTick = 0;

        return true;
    }

    public synchronized long incrementTick() {

        currentTick++;

        return currentTick;
    }

    public synchronized void resetGame() {

        started = false;
        currentTick = 0;
    }
}