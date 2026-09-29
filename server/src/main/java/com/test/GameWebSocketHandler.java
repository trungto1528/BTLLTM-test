package com.test;

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

    // =========================
    // CONNECTION
    // =========================

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

        session.sendMessage(
                new TextMessage(
                        "WELCOME|"
                                + playerId));
    }

    // =========================
    // INPUT
    // =========================

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

        String[] parts =
                input.split("\\|");

        /*
         * Format:
         *
         * INPUT|sequence|action
         */
        if (parts.length != 3
                || !"INPUT".equals(parts[0])) {

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

        /*
         * Chỉ chấp nhận những action mà
         * game thực sự hỗ trợ.
         */
        if (!isValidAction(action)) {

            System.out.println(
                    "Unknown action: "
                            + action);

            return;
        }

        /*
         * =====================================================
         * IMPORTANT
         * =====================================================
         *
         * WebSocket thread KHÔNG được trực tiếp
         * thay đổi gameplay state.
         *
         * Không gọi:
         *
         *     setMovingLeft()
         *     setMovingRight()
         *     startCharging()
         *     releaseJump()
         *
         * ở đây.
         *
         * Input được đưa vào queue.
         *
         * GameServer.tick() ở 60 TPS sẽ lấy
         * input từ queue và xử lý tại tick boundary.
         */
        player.queueInput(
                new InputCommand(
                        sequence,
                        action));
    }

    // =========================
    // INPUT VALIDATION
    // =========================

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

    // =========================
    // DISCONNECT
    // =========================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        String playerId =
                session.getId();

        gameServer.removePlayer(
                playerId);

        gameServer.removeSession(
                playerId);

        System.out.println(
                "Client disconnected: "
                        + playerId);
    }
}