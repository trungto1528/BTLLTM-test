package com.test;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.test.common.InputCommand;

@Component
public class GameWebSocketHandler
        extends TextWebSocketHandler {

    private final GameServer gameServer;

    public GameWebSocketHandler(
            GameServer gameServer) {

        this.gameServer = gameServer;
    }

    // =====================================================
    // CONNECTION
    // =====================================================

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session)
            throws Exception {

        String playerId =
                session.getId();

        PlayerSession player =
                new PlayerSession(playerId);

        /*
         * Player tồn tại trên server ngay khi
         * WebSocket kết nối.
         *
         * Nhưng CHƯA thuộc room nào.
         */
        gameServer.addPlayer(player);

        gameServer.addSession(
                playerId,
                session);

        System.out.println(
                "Client connected: "
                        + playerId);

        send(
                session,
                "WELCOME|"
                        + playerId);
    }

    // =====================================================
    // MESSAGE ROUTER
    // =====================================================

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message)
            throws Exception {

        PlayerSession player =
                gameServer.getPlayer(
                        session.getId());

        if (player == null) {

            return;
        }

        String input =
                message.getPayload();

        if (input == null
                || input.isBlank()) {

            return;
        }

        /*
         * =================================================
         * ROOM COMMANDS
         * =================================================
         */

        if ("CREATE_ROOM".equals(input)) {

            handleCreateRoom(
                    session,
                    player);

            return;
        }

        if ("FIND_ROOMS".equals(input)) {

            handleFindRooms(
                    session);

            return;
        }

        if ("LEAVE_ROOM".equals(input)) {

            handleLeaveRoom(
                    session,
                    player);

            return;
        }

        if ("START_GAME".equals(input)) {

            handleStartGame(
                    session,
                    player);

            return;
        }

        /*
         * JOIN_ROOM|ABCDE
         */
        if (input.startsWith("JOIN_ROOM|")) {

            handleJoinRoom(
                    session,
                    player,
                    input);

            return;
        }

        /*
         * =================================================
         * GAME INPUT
         * =================================================
         */

        if (input.startsWith("INPUT|")) {

            handleInput(
                    player,
                    input);

            return;
        }

        System.out.println(
                "Unknown message: "
                        + input);
    }

    // =====================================================
    // CREATE ROOM
    // =====================================================

    private void handleCreateRoom(
            WebSocketSession session,
            PlayerSession player)
            throws Exception {

        String playerId =
                player.getPlayerState()
                        .getPlayerId();

        /*
         * Không cho player đang ở room
         * tạo room thứ hai.
         */
        Room existingRoom =
                gameServer.getRoomManager()
                        .findRoomByPlayer(
                                playerId);

        if (existingRoom != null) {

            sendError(
                    session,
                    "ALREADY_IN_ROOM");

            return;
        }

        Room room =
                gameServer.getRoomManager()
                        .createRoom(player);

        System.out.println(
                "Room created: "
                        + room.getRoomId()
                        + " by "
                        + playerId);

        /*
         * Thông báo cho chính client.
         */
        send(
                session,
                "ROOM_CREATED|"
                        + room.getRoomId());

        /*
         * Gửi trạng thái lobby.
         */
        broadcastRoomState(room);
    }

    // =====================================================
    // JOIN ROOM
    // =====================================================

    private void handleJoinRoom(
            WebSocketSession session,
            PlayerSession player,
            String message)
            throws Exception {

        String[] parts =
                message.split("\\|");

        /*
         * Format:
         *
         * JOIN_ROOM|ABCDE
         */
        if (parts.length != 2
                || parts[1].isBlank()) {

            sendError(
                    session,
                    "INVALID_ROOM_ID");

            return;
        }

        String roomId =
                parts[1]
                        .trim()
                        .toUpperCase();

        String playerId =
                player.getPlayerState()
                        .getPlayerId();

        /*
         * Không cho join khi đã ở room.
         */
        Room currentRoom =
                gameServer.getRoomManager()
                        .findRoomByPlayer(
                                playerId);

        if (currentRoom != null) {

            sendError(
                    session,
                    "ALREADY_IN_ROOM");

            return;
        }

        Room room =
                gameServer.getRoomManager()
                        .getRoom(roomId);

        if (room == null) {

            sendError(
                    session,
                    "ROOM_NOT_FOUND");

            return;
        }

        if (room.isStarted()) {

            sendError(
                    session,
                    "ROOM_STARTED");

            return;
        }

        if (room.isFull()) {

            sendError(
                    session,
                    "ROOM_FULL");

            return;
        }

        boolean joined =
                gameServer.getRoomManager()
                        .joinRoom(
                                roomId,
                                player);

        if (!joined) {

            sendError(
                    session,
                    "JOIN_FAILED");

            return;
        }

        System.out.println(
                "Player "
                        + playerId
                        + " joined room "
                        + roomId);

        /*
         * Client vừa join nhận ROOM_JOINED.
         */
        send(
                session,
                "ROOM_JOINED|"
                        + room.getRoomId());

        /*
         * Tất cả player trong room cập nhật
         * danh sách người chơi.
         */
        broadcastRoomState(room);
    }

    // =====================================================
    // FIND ROOMS
    // =====================================================

    private void handleFindRooms(
            WebSocketSession session)
            throws Exception {

        List<Room> rooms =
                gameServer.getRoomManager()
                        .getAvailableRooms();

        /*
         * Không có room.
         */
        if (rooms.isEmpty()) {

            send(
                    session,
                    "ROOM_LIST|EMPTY");

            return;
        }

        /*
         * Mỗi room là một message:
         *
         * ROOM_LIST|ABCDE|1|4|OPEN
         *
         * ROOM_LIST|XYZ12|3|4|OPEN
         *
         * Cách này đơn giản hơn việc nhét
         * toàn bộ danh sách vào một message.
         */
        for (Room room : rooms) {

            send(
                    session,
                    "ROOM_LIST|"
                            + room.getRoomId()
                            + "|"
                            + room.getPlayerCount()
                            + "|"
                            + room.getMaxPlayers()
                            + "|OPEN");
        }
    }

    // =====================================================
    // LEAVE ROOM
    // =====================================================

    private void handleLeaveRoom(
            WebSocketSession session,
            PlayerSession player)
            throws Exception {

        String playerId =
                player.getPlayerState()
                        .getPlayerId();

        Room room =
                gameServer.getRoomManager()
                        .findRoomByPlayer(
                                playerId);

        if (room == null) {

            sendError(
                    session,
                    "NOT_IN_ROOM");

            return;
        }

        String roomId =
                room.getRoomId();

        Room remainingRoom =
                gameServer.getRoomManager()
                        .leaveRoom(
                                roomId,
                                playerId);

        /*
         * Xác nhận với player vừa rời.
         */
        send(
                session,
                "ROOM_LEFT|"
                        + roomId);

        /*
         * Nếu vẫn còn người trong room,
         * cập nhật lobby cho họ.
         */
        if (remainingRoom != null) {

            broadcastRoomState(
                    remainingRoom);
        }

        System.out.println(
                "Player "
                        + playerId
                        + " left room "
                        + roomId);
    }

    // =====================================================
    // START GAME
    // =====================================================

    private void handleStartGame(
            WebSocketSession session,
            PlayerSession player)
            throws Exception {

        String playerId =
                player.getPlayerState()
                        .getPlayerId();

        Room room =
                gameServer.getRoomManager()
                        .findRoomByPlayer(
                                playerId);

        if (room == null) {

            sendError(
                    session,
                    "NOT_IN_ROOM");

            return;
        }

        /*
         * Chỉ host được START.
         */
        if (!room.isHost(playerId)) {

            sendError(
                    session,
                    "NOT_HOST");

            return;
        }

        if (room.isStarted()) {

            sendError(
                    session,
                    "GAME_ALREADY_STARTED");

            return;
        }

        /*
         * Room bắt đầu timeline riêng từ tick 0.
         */
        boolean started =
                room.startGame();

        if (!started) {

            sendError(
                    session,
                    "START_FAILED");

            return;
        }

        System.out.println(
                "Game started in room "
                        + room.getRoomId());

        /*
         * Quan trọng:
         *
         * Chỉ player trong room này nhận
         * GAME_STARTED.
         */
        broadcast(
                room,
                "GAME_STARTED|"
                        + room.getRoomId());
    }

    // =====================================================
    // INPUT
    // =====================================================

    private void handleInput(
            PlayerSession player,
            String input) {

        /*
         * Player phải thuộc một room.
         */
        String playerId =
                player.getPlayerState()
                        .getPlayerId();

        Room room =
                gameServer.getRoomManager()
                        .findRoomByPlayer(
                                playerId);

        if (room == null) {

            return;
        }

        /*
         * Chưa START thì không nhận gameplay input.
         */
        if (!room.isStarted()) {

            return;
        }

        String[] parts =
                input.split("\\|");

        /*
         * Format:
         *
         * INPUT|sequence|action
         */
        if (parts.length != 3) {

            System.out.println(
                    "Invalid input: "
                            + input);

            return;
        }

        int sequence;

        try {

            sequence =
                    Integer.parseInt(
                            parts[1]);

        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid input sequence: "
                            + input);

            return;
        }

        /*
         * Sequence không được âm.
         */
        if (sequence < 0) {

            System.out.println(
                    "Invalid negative input sequence: "
                            + input);

            return;
        }

        String action =
                parts[2];

        if (!isValidAction(action)) {

            System.out.println(
                    "Unknown action: "
                            + action);

            return;
        }

        /*
         * KHÔNG xử lý gameplay trực tiếp ở
         * WebSocket thread.
         *
         * Chỉ đưa input vào queue.
         *
         * GameServer 60 TPS sẽ xử lý.
         */
        player.queueInput(
                new InputCommand(
                        sequence,
                        action));
    }

    // =====================================================
    // INPUT VALIDATION
    // =====================================================

    private boolean isValidAction(
            String action) {

        return switch (action) {

            case "LEFT_PRESS",
                 "LEFT_RELEASE",
                 "RIGHT_PRESS",
                 "RIGHT_RELEASE",
                 "JUMP_START",
                 "JUMP_RELEASE" -> true;

            default -> false;
        };
    }

    // =====================================================
    // ROOM STATE
    // =====================================================

    /**
     * Gửi trạng thái lobby cho toàn bộ
     * player trong room.
     *
     * Format:
     *
     * ROOM_STATE|roomId|currentPlayers|maxPlayers|hostId
     */
    private void broadcastRoomState(
            Room room)
            throws Exception {

        if (room == null
                || room.isEmpty()) {

            return;
        }

        String message =
                "ROOM_STATE|"
                        + room.getRoomId()
                        + "|"
                        + room.getPlayerCount()
                        + "|"
                        + room.getMaxPlayers()
                        + "|"
                        + room.getHostPlayerId();

        broadcast(
                room,
                message);
    }

    // =====================================================
    // BROADCAST TO ROOM
    // =====================================================

    /**
     * Gửi message chỉ cho các player
     * thuộc room.
     */
    private void broadcast(
            Room room,
            String message)
            throws Exception {

        if (room == null) {

            return;
        }

        for (PlayerSession player
                : room.getPlayers()) {

            String playerId =
                    player.getPlayerState()
                            .getPlayerId();

            WebSocketSession target =
                    gameServer.getSession(
                            playerId);

            if (target == null
                    || !target.isOpen()) {

                continue;
            }

            send(
                    target,
                    message);
        }
    }

    // =====================================================
    // SEND
    // =====================================================

    private void send(
            WebSocketSession session,
            String message)
            throws Exception {

        if (session == null
                || !session.isOpen()) {

            return;
        }

        session.sendMessage(
                new TextMessage(
                        message));
    }

    private void sendError(
            WebSocketSession session,
            String error)
            throws Exception {

        send(
                session,
                "ROOM_ERROR|"
                        + error);
    }

    // =====================================================
    // DISCONNECT
    // =====================================================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        String playerId =
                session.getId();

        PlayerSession player =
                gameServer.getPlayer(
                        playerId);

        /*
         * Phải remove player khỏi room TRƯỚC
         * khi remove khỏi GameServer.
         */
        if (player != null) {

            Room room =
                    gameServer.getRoomManager()
                            .findRoomByPlayer(
                                    playerId);

            if (room != null) {

                String roomId =
                        room.getRoomId();

                Room remainingRoom =
                        gameServer.getRoomManager()
                                .leaveRoom(
                                        roomId,
                                        playerId);

                /*
                 * Nếu room vẫn còn người,
                 * broadcast host/player count mới.
                 */
                if (remainingRoom != null) {

                    try {

                        broadcastRoomState(
                                remainingRoom);

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to broadcast "
                                        + "room state after "
                                        + "disconnect: "
                                        + e.getMessage());
                    }
                }

                System.out.println(
                        "Player "
                                + playerId
                                + " removed from room "
                                + roomId);
            }
        }

        /*
         * Sau đó mới remove global player/session.
         */
        gameServer.removePlayer(
                playerId);

        gameServer.removeSession(
                playerId);

        System.out.println(
                "Client disconnected: "
                        + playerId);
    }
}