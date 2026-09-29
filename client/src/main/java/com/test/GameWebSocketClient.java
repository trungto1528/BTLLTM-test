package com.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class GameWebSocketClient
        implements WebSocket.Listener {

    private WebSocket webSocket;

    private GameScene gameScene;

    public void setGameScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    public void connect() {

        HttpClient client = HttpClient.newHttpClient();

        client.newWebSocketBuilder()
                .buildAsync(
                        URI.create(
                                "ws://localhost:8080/game"),
                        this)
                .thenAccept(ws -> {

                    this.webSocket = ws;

                    System.out.println(
                            "WebSocket connected");

                })
                .exceptionally(error -> {

                    System.err.println(
                            "WebSocket connection failed: "
                                    + error.getMessage());

                    return null;
                });
    }

    public void send(String message) {

        if (webSocket == null) {
            System.out.println(
                    "WebSocket not connected");
            return;
        }

        webSocket.sendText(
                message,
                true);
    }

    public void sendInput(String input) {
        send(input);
    }

    @Override
    public void onOpen(
            WebSocket webSocket) {

        this.webSocket = webSocket;

        System.out.println(
                "Connected to server");

        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last) {

        String message = data.toString();

        System.out.println(
                "Server: " + message);

        if (gameScene != null) {

            if (message.startsWith("WELCOME|")) {

                String playerId = message.substring("WELCOME|".length());

                gameScene.setLocalPlayerId(
                        playerId);

            } else if (message.startsWith("PLAYER_STATE|")) {

                gameScene.handlePlayerState(
                        message);

            } else if (message.startsWith("WORLD_STATE|")) {

                gameScene.handleWorldState(
                        message);
            }
        }

        return WebSocket.Listener.super.onText(
                webSocket,
                data,
                last);
    }

    @Override
    public CompletionStage<?> onClose(
            WebSocket webSocket,
            int statusCode,
            String reason) {

        System.out.println(
                "Disconnected: "
                        + reason);

        return WebSocket.Listener.super.onClose(
                webSocket,
                statusCode,
                reason);
    }

    @Override
    public void onError(
            WebSocket webSocket,
            Throwable error) {

        System.err.println(
                "WebSocket error: "
                        + error.getMessage());
    }
}