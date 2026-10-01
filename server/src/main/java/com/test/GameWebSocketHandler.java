package com.test;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.test.common.InputCommand;
import com.test.common.map.MapData;
import com.test.common.map.MapInfo;

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

        // =================================================
        // ROOM COMMANDS
        // =================================================

        if ("CREATE_ROOM".equals(input)) {

            handleCreateRoom(
                    session,
                    player);

            return;
        }

        if (input.startsWith("CREATE_ROOM|")) {

            /*
             * Client không còn được phép
             * chọn map khi tạo room.
             */
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

        if (input.startsWith("CHANGE_MAP|")) {

            handleChangeMap(
                    session,
                    player,
                    input);

            return;
        }

        if (input.startsWith("JOIN_ROOM|")) {

            handleJoinRoom(
                    session,
                    player,
                    input);

            return;
        }

        // =================================================
        // GAME INPUT
        // =================================================

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

        /*
         * Map khởi đầu của room được quyết định
         * hoàn toàn ở server.
         *
         * Không còn phụ thuộc vào map01.
         */
        List<MapInfo> mapInfos =
                gameServer.getMapRepository()
                        .getMapInfos();

        if (mapInfos == null
                || mapInfos.isEmpty()) {

            sendError(
                    session,
                    "NO_MAP_AVAILABLE");

            return;
        }

        MapInfo firstMap =
                mapInfos.get(0);

        if (firstMap == null
                || firstMap.getId() == null
                || firstMap.getId().isBlank()) {

            sendError(
                    session,
                    "INVALID_DEFAULT_MAP");

            return;
        }

        String mapId =
                firstMap.getId();

        MapData map;

        try {

            map =
                    gameServer.getMapRepository()
                            .getMap(mapId);

        } catch (IllegalArgumentException e) {

            sendError(
                    session,
                    "MAP_NOT_FOUND");

            return;
        }

        Room room =
                gameServer.getRoomManager()
                        .createRoom(
                                player,
                                mapId);

        player.setSpawn(
                map.getSpawn());

        System.out.println(
                "Room created: "
                        + room.getRoomId()
                        + " | map="
                        + room.getMapId()
                        + " by "
                        + playerId);

        send(
                session,
                "ROOM_CREATED|"
                        + room.getRoomId()
                        + "|"
                        + room.getMapId());

        broadcastRoomState(room);
    }

    // =====================================================
    // CHANGE MAP
    // =====================================================

    private void handleChangeMap(
            WebSocketSession session,
            PlayerSession player,
            String message)
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

        String[] parts =
                message.split("\\|");

        if (parts.length != 2
                || parts[1].isBlank()) {

            sendError(
                    session,
                    "INVALID_MAP_ID");

            return;
        }

        String mapId =
                parts[1].trim();

        MapData map;

        try {

            map =
                    gameServer.getMapRepository()
                            .getMap(mapId);

        } catch (IllegalArgumentException e) {

            sendError(
                    session,
                    "MAP_NOT_FOUND");

            return;
        }

        boolean changed =
                room.setMapId(mapId);

        if (!changed) {

            sendError(
                    session,
                    "MAP_CHANGE_FAILED");

            return;
        }

        player.setSpawn(
                map.getSpawn());

        for (PlayerSession roomPlayer
                : room.getPlayers()) {

            roomPlayer.setSpawn(
                    map.getSpawn());
        }

        System.out.println(
                "Room "
                        + room.getRoomId()
                        + " changed map to "
                        + mapId
                        + " by "
                        + playerId);

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

        MapData map;

        try {

            map =
                    gameServer.getMapRepository()
                            .getMap(
                                    room.getMapId());

        } catch (IllegalArgumentException e) {

            sendError(
                    session,
                    "MAP_NOT_FOUND");

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

        player.setSpawn(
                map.getSpawn());

        System.out.println(
                "Player "
                        + playerId
                        + " joined room "
                        + roomId
                        + " | map="
                        + room.getMapId());

        send(
                session,
                "ROOM_JOINED|"
                        + room.getRoomId()
                        + "|"
                        + room.getMapId());

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

        if (rooms.isEmpty()) {

            send(
                    session,
                    "ROOM_LIST|EMPTY");

            send(
                    session,
                    "ROOM_LIST_END");

            return;
        }

        for (Room room : rooms) {

            send(
                    session,
                    "ROOM_LIST|"
                            + room.getRoomId()
                            + "|"
                            + room.getPlayerCount()
                            + "|"
                            + room.getMaxPlayers()
                            + "|OPEN|"
                            + room.getMapId());
        }

        send(
                session,
                "ROOM_LIST_END");
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

        boolean wasHost =
                room.isHost(playerId);

        Room remainingRoom =
                gameServer.getRoomManager()
                        .leaveRoom(
                                roomId,
                                playerId);

        send(
                session,
                "ROOM_LEFT|"
                        + roomId);

        if (remainingRoom != null) {

            String newHostId =
                    remainingRoom
                            .getHostPlayerId();

            broadcast(
                    remainingRoom,
                    "PLAYER_LEFT|"
                            + roomId
                            + "|"
                            + playerId
                            + "|"
                            + wasHost
                            + "|"
                            + newHostId);

            broadcastRoomState(
                    remainingRoom);
        }

        System.out.println(
                "Player "
                        + playerId
                        + " left room "
                        + roomId
                        + " | wasHost="
                        + wasHost);
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
                        + room.getRoomId()
                        + " | map="
                        + room.getMapId());

        broadcast(
                room,
                "GAME_STARTED|"
                        + room.getRoomId()
                        + "|"
                        + room.getMapId());
    }

    // =====================================================
    // INPUT
    // =====================================================

    private void handleInput(
            PlayerSession player,
            String input) {

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

        if (!room.isStarted()) {
            return;
        }

        String[] parts =
                input.split("\\|");

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
                        + room.getHostPlayerId()
                        + "|"
                        + room.getMapId();

        broadcast(
                room,
                message);
    }

    // =====================================================
    // BROADCAST
    // =====================================================

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

        if (player != null) {

            Room room =
                    gameServer.getRoomManager()
                            .findRoomByPlayer(
                                    playerId);

            if (room != null) {

                String roomId =
                        room.getRoomId();

                boolean wasHost =
                        room.isHost(playerId);

                Room remainingRoom =
                        gameServer.getRoomManager()
                                .leaveRoom(
                                        roomId,
                                        playerId);

                if (remainingRoom != null) {

                    try {

                        String newHostId =
                                remainingRoom
                                        .getHostPlayerId();

                        broadcast(
                                remainingRoom,
                                "PLAYER_LEFT|"
                                        + roomId
                                        + "|"
                                        + playerId
                                        + "|"
                                        + wasHost
                                        + "|"
                                        + newHostId);

                        broadcastRoomState(
                                remainingRoom);

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to broadcast "
                                        + "player departure: "
                                        + e.getMessage());
                    }
                }

                System.out.println(
                        "Player "
                                + playerId
                                + " removed from room "
                                + roomId
                                + " | wasHost="
                                + wasHost);
            }
        }

        gameServer.removePlayer(
                playerId);

        gameServer.removeSession(
                playerId);

        System.out.println(
                "Client disconnected: "
                        + playerId);
    }
}