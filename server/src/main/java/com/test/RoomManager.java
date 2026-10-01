package com.test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoomManager {

    private static final int ROOM_ID_LENGTH = 5;

    private static final String ROOM_ID_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final ConcurrentMap<String, Room> rooms =
            new ConcurrentHashMap<>();

    // =====================================================
    // CREATE
    // =====================================================

    public Room createRoom(
            PlayerSession host,
            String mapId) {

        if (host == null) {

            throw new IllegalArgumentException(
                    "host must not be null"
            );
        }

        if (mapId == null
                || mapId.isBlank()) {

            throw new IllegalArgumentException(
                    "mapId must not be blank"
            );
        }

        while (true) {

            String roomId =
                    generateRoomId();

            Room room =
                    new Room(
                            roomId,
                            Room.DEFAULT_MAX_PLAYERS,
                            mapId
                    );

            Room existing =
                    rooms.putIfAbsent(
                            roomId,
                            room
                    );

            if (existing == null) {

                boolean added =
                        room.addPlayer(host);

                if (!added) {

                    rooms.remove(
                            roomId,
                            room
                    );

                    throw new IllegalStateException(
                            "Failed to add host to new room"
                    );
                }

                return room;
            }
        }
    }

    // =====================================================
    // FIND
    // =====================================================

    public Room getRoom(
            String roomId) {

        if (roomId == null) {

            return null;
        }

        return rooms.get(
                normalizeRoomId(roomId)
        );
    }

    public List<Room> getAvailableRooms() {

        List<Room> result =
                new ArrayList<>();

        for (Room room : rooms.values()) {

            if (!room.isStarted()
                    && !room.isFull()) {

                result.add(room);
            }
        }

        result.sort(
                (a, b) ->
                        a.getRoomId()
                                .compareTo(
                                        b.getRoomId()
                                )
        );

        return result;
    }

    public Collection<Room> getRooms() {

        return rooms.values();
    }

    public int getRoomCount() {

        return rooms.size();
    }

    // =====================================================
    // JOIN
    // =====================================================

    public boolean joinRoom(
            String roomId,
            PlayerSession player) {

        if (roomId == null
                || player == null) {

            return false;
        }

        Room room =
                getRoom(roomId);

        if (room == null) {

            return false;
        }

        return room.addPlayer(player);
    }

    // =====================================================
    // LEAVE
    // =====================================================

    public Room leaveRoom(
            String roomId,
            String playerId) {

        if (roomId == null
                || playerId == null) {

            return null;
        }

        Room room =
                getRoom(roomId);

        if (room == null) {

            return null;
        }

        boolean wasHost =
                room.isHost(playerId);

        room.removePlayer(playerId);

        if (room.isEmpty()) {

            rooms.remove(
                    room.getRoomId(),
                    room
            );

            return null;
        }

        if (wasHost
                || room.getHostPlayerId() == null) {

            PlayerSession newHost =
                    room.getPlayers()
                            .stream()
                            .findFirst()
                            .orElse(null);

            if (newHost != null) {

                room.setHostPlayerId(
                        newHost
                                .getPlayerState()
                                .getPlayerId()
                );
            }
        }

        return room;
    }

    public Room findRoomByPlayer(
            String playerId) {

        if (playerId == null) {

            return null;
        }

        for (Room room : rooms.values()) {

            if (room.getPlayer(playerId) != null) {

                return room;
            }
        }

        return null;
    }

    // =====================================================
    // REMOVE
    // =====================================================

    public boolean removeRoom(
            String roomId) {

        if (roomId == null) {

            return false;
        }

        return rooms.remove(
                normalizeRoomId(roomId)
        ) != null;
    }

    // =====================================================
    // ROOM ID
    // =====================================================

    private String generateRoomId() {

        StringBuilder result =
                new StringBuilder(
                        ROOM_ID_LENGTH
                );

        for (int i = 0;
                i < ROOM_ID_LENGTH;
                i++) {

            int index =
                    RANDOM.nextInt(
                            ROOM_ID_CHARACTERS.length()
                    );

            result.append(
                    ROOM_ID_CHARACTERS.charAt(
                            index
                    )
            );
        }

        return result.toString();
    }

    private String normalizeRoomId(
            String roomId) {

        return roomId
                .trim()
                .toUpperCase();
    }
}