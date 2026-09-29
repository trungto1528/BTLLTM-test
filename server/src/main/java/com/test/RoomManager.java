package com.test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Quản lý toàn bộ room trên server.
 *
 * RoomManager KHÔNG xử lý:
 *
 * - WebSocket
 * - physics
 * - game tick
 * - broadcast
 *
 * Nó chỉ chịu trách nhiệm lifecycle của room.
 */
public class RoomManager {

    private static final int ROOM_ID_LENGTH = 5;

    /*
     * Không dùng các ký tự dễ nhầm:
     *
     * 0 / O
     * 1 / I
     *
     * để người dùng đọc room ID dễ hơn.
     */
    private static final String ROOM_ID_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final ConcurrentMap<String, Room> rooms =
            new ConcurrentHashMap<>();

    // =====================================================
    // CREATE
    // =====================================================

    /**
     * Tạo một room mới.
     *
     * Room ID được tạo hoàn toàn ở server.
     */
    public Room createRoom(
            PlayerSession host) {

        if (host == null) {

            throw new IllegalArgumentException(
                    "host must not be null"
            );
        }

        /*
         * Tạo ID cho đến khi tìm được ID chưa tồn tại.
         */
        while (true) {

            String roomId =
                    generateRoomId();

            Room room =
                    new Room(roomId);

            /*
             * putIfAbsent đảm bảo atomic.
             *
             * Nếu có collision thì thử ID khác.
             */
            Room existing =
                    rooms.putIfAbsent(
                            roomId,
                            room
                    );

            if (existing == null) {

                /*
                 * Room vừa được tạo nên host
                 * phải join ngay.
                 */
                boolean added =
                        room.addPlayer(host);

                if (!added) {

                    /*
                     * Không nên xảy ra, nhưng nếu xảy ra
                     * thì rollback room.
                     */
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

    /**
     * Trả về các room đang chờ người chơi.
     *
     * Không trả về:
     *
     * - room đã STARTED
     * - room FULL
     */
    public List<Room> getAvailableRooms() {

        List<Room> result =
                new ArrayList<>();

        for (Room room : rooms.values()) {

            if (!room.isStarted()
                    && !room.isFull()) {

                result.add(room);
            }
        }

        /*
         * Sắp xếp để danh sách ổn định hơn
         * khi client gọi FIND_ROOMS nhiều lần.
         */
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

    /**
     * Thử cho player join room.
     *
     * Kết quả:
     *
     * true  = join thành công
     * false = room không tồn tại / full /
     *         started / player đã ở trong room
     */
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

    /**
     * Remove player khỏi room.
     *
     * Nếu player là host và room vẫn còn người,
     * tự động chuyển host cho người tiếp theo.
     *
     * Nếu room trở thành empty,
     * room sẽ bị xóa khỏi manager.
     */
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

        /*
         * Room empty => xóa room.
         */
        if (room.isEmpty()) {

            rooms.remove(
                    room.getRoomId(),
                    room
            );

            return null;
        }

        /*
         * Host rời room.
         *
         * Chọn player đầu tiên còn lại
         * làm host mới.
         */
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

    /**
     * Tìm room mà player đang ở.
     *
     * Dùng cho:
     *
     * - INPUT
     * - START_GAME
     * - LEAVE_ROOM
     * - disconnect
     */
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