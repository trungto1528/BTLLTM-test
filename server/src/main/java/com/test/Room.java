package com.test;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Đại diện cho một phòng multiplayer.
 *
 * Room chỉ quản lý:
 *
 * - room ID
 * - host
 * - players
 * - trạng thái STARTED / WAITING
 * - game tick của room
 *
 * Room KHÔNG chứa physics hoặc network logic.
 */
public class Room {

    public static final int DEFAULT_MAX_PLAYERS = 4;

    private final String roomId;

    private final int maxPlayers;

    private final ConcurrentMap<String, PlayerSession> players =
            new ConcurrentHashMap<>();

    /*
     * Host hiện tại.
     *
     * volatile để các thread WebSocket / game loop
     * luôn nhìn thấy giá trị mới nhất.
     */
    private volatile String hostPlayerId;

    /*
     * false = đang ở lobby
     * true  = game đã bắt đầu
     */
    private volatile boolean started;

    /*
     * Logical tick riêng của room.
     *
     * Mỗi room bắt đầu từ tick 0 khi START_GAME.
     *
     * Điều này giúp client mới tạo GameScene có thể
     * reconciliation theo timeline của chính room đó.
     */
    private long currentTick;

    public Room(String roomId) {

        this(
                roomId,
                DEFAULT_MAX_PLAYERS
        );
    }

    public Room(
            String roomId,
            int maxPlayers) {

        if (roomId == null
                || roomId.isBlank()) {

            throw new IllegalArgumentException(
                    "roomId must not be blank"
            );
        }

        if (maxPlayers <= 0) {

            throw new IllegalArgumentException(
                    "maxPlayers must be greater than 0"
            );
        }

        this.roomId = roomId;
        this.maxPlayers = maxPlayers;

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

    /**
     * Chuyển host cho player khác.
     *
     * Method này chỉ đổi host.
     * Việc chọn host mới do RoomManager quyết định.
     */
    public synchronized void setHostPlayerId(
            String playerId) {

        if (playerId == null
                || !players.containsKey(playerId)) {

            throw new IllegalArgumentException(
                    "Host must be a player in this room"
            );
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

    /**
     * Thêm player vào room.
     *
     * Player đầu tiên tự động trở thành host.
     *
     * Không cho join nếu:
     *
     * - room đã STARTED
     * - room đã FULL
     * - player đã tồn tại
     */
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

        /*
         * Không cho một player join cùng room
         * nhiều lần.
         */
        if (players.containsKey(playerId)) {

            return false;
        }

        players.put(
                playerId,
                player
        );

        /*
         * Player đầu tiên là host.
         */
        if (hostPlayerId == null) {

            hostPlayerId = playerId;
        }

        return true;
    }

    /**
     * Xóa player khỏi room.
     *
     * Method này không tự chọn host mới.
     * RoomManager sẽ xử lý việc chuyển host.
     */
    public synchronized PlayerSession removePlayer(
            String playerId) {

        if (playerId == null) {

            return null;
        }

        PlayerSession removed =
                players.remove(playerId);

        /*
         * Nếu host bị remove và room vẫn còn player,
         * host sẽ được RoomManager chọn lại.
         */
        if (playerId.equals(hostPlayerId)) {

            hostPlayerId = null;
        }

        return removed;
    }

    // =====================================================
    // GAME STATE
    // =====================================================

    /**
     * Bắt đầu game.
     *
     * Chỉ chuyển trạng thái.
     * Không chạy physics ở đây.
     */
    public synchronized boolean startGame() {

        if (started) {

            return false;
        }

        if (players.isEmpty()) {

            return false;
        }

        started = true;

        /*
         * Mỗi room có timeline riêng.
         */
        currentTick = 0;

        return true;
    }

    /**
     * Tăng logical tick của room.
     *
     * Chỉ GameServer game thread nên gọi.
     */
    public synchronized long incrementTick() {

        currentTick++;

        return currentTick;
    }

    /**
     * Reset room về trạng thái lobby.
     *
     * Hiện tại chưa sử dụng trong flow chính,
     * nhưng giữ method để sau này hỗ trợ restart game.
     */
    public synchronized void resetGame() {

        started = false;

        currentTick = 0;
    }
}