package com.test;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler
        extends TextWebSocketHandler {

    private final GameServer gameServer;

    public GameWebSocketHandler(
            GameServer gameServer) {

        this.gameServer = gameServer;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session)
            throws Exception {

        PlayerSession player = new PlayerSession(session.getId());

        gameServer.addPlayer(player);

        gameServer.addSession(
                session.getId(),
                session);

        System.out.println(
                "Client connected: "
                        + session.getId());

        session.sendMessage(
                new TextMessage(
                        "WELCOME|" + session.getId()));
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message)
            throws Exception {

        PlayerSession player = gameServer.getPlayer(
                session.getId());

        if (player == null) {
            return;
        }

        String input = message.getPayload();

        String[] parts = input.split("\\|");

        if (parts.length != 3
                || !parts[0].equals("INPUT")) {

            System.out.println(
                    "Invalid input: "
                            + input);

            return;
        }

        int sequence;

        try {

            sequence = Integer.parseInt(parts[1]);

        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid input sequence: "
                            + input);

            return;
        }

        String action = parts[2];

        switch (action) {

            case "LEFT_PRESS" -> {

                player.setMovingLeft(true);

                player.processInputSequence(
                        sequence);
            }

            case "LEFT_RELEASE" -> {

                player.setMovingLeft(false);

                player.processInputSequence(
                        sequence);
            }

            case "RIGHT_PRESS" -> {

                player.setMovingRight(true);

                player.processInputSequence(
                        sequence);
            }

            case "RIGHT_RELEASE" -> {

                player.setMovingRight(false);

                player.processInputSequence(
                        sequence);
            }

            case "JUMP_START" -> {

                player.startCharging();

                player.processInputSequence(
                        sequence);
            }

            case "JUMP_RELEASE" -> {

                player.releaseJump();

                player.processInputSequence(
                        sequence);
            }

            default -> {

                System.out.println(
                        "Unknown action: "
                                + action);
            }
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            org.springframework.web.socket.CloseStatus status) {

        gameServer.removePlayer(
                session.getId());

        gameServer.removeSession(
                session.getId());

        System.out.println(
                "Client disconnected: "
                        + session.getId());
    }
}