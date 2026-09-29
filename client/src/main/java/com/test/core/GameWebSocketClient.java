package com.test.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class GameWebSocketClient
        implements WebSocket.Listener {

    private static final String SERVER_URI =
            "ws://localhost:8080/game";

    private WebSocket webSocket;

    private GameScene gameScene;

    public void setGameScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    // =========================
    // CONNECT
    // =========================

    public void connect() {

        HttpClient client =
                HttpClient.newHttpClient();

        client.newWebSocketBuilder()
                .buildAsync(
                        URI.create(SERVER_URI),
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

    // =========================
    // SEND
    // =========================

    public void send(String message) {

        WebSocket ws = webSocket;

        if (ws == null) {
            System.out.println(
                    "WebSocket not connected");
            return;
        }

        if (!ws.isOutputClosed()) {

            ws.sendText(
                    message,
                    true);
        }
    }

    public void sendInput(String input) {
        send(input);
    }

    // =========================
    // RECEIVE
    // =========================

    @Override
    public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last) {

        String message =
                data.toString();

        System.out.println(
                "[WS RECEIVE] " + message);

        GameScene scene = gameScene;

        if (scene != null) {

            if (message.startsWith("WELCOME|")) {

                String playerId =
                        message.substring(
                                "WELCOME|".length());

                javafx.application.Platform.runLater(
                        () -> scene.setLocalPlayerId(
                                playerId));

            } else if (message.startsWith(
                    "PLAYER_STATE|")) {

                scene.handlePlayerState(
                        message);

            } else if (message.startsWith(
                    "WORLD_STATE|")) {

                scene.handleWorldState(
                        message);
            }
        }

        return WebSocket.Listener.super.onText(
                webSocket,
                data,
                last);
    }

    // =========================
    // CLOSE
    // =========================

    @Override
    public CompletionStage<?> onClose(
            WebSocket webSocket,
            int statusCode,
            String reason) {

        System.out.println(
                "Disconnected: "
                        + reason);

        this.webSocket = null;

        return WebSocket.Listener.super.onClose(
                webSocket,
                statusCode,
                reason);
    }

    // =========================
    // ERROR
    // =========================

    @Override
    public void onError(
            WebSocket webSocket,
            Throwable error) {

        System.err.println(
                "WebSocket error: "
                        + error.getMessage());
    }
}