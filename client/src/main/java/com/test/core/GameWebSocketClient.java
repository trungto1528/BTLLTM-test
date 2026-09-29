package com.test.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import javafx.application.Platform;

public class GameWebSocketClient
        implements WebSocket.Listener {

    private static final String SERVER_URI =
            "ws://localhost:8080/game";

    private WebSocket webSocket;

    private GameScene gameScene;

    /*
     * Nhận các message liên quan đến lobby
     * và lifecycle của game.
     *
     * GameApp sẽ đăng ký listener này.
     */
    private Consumer<String> messageHandler;

    /*
     * Message gửi trước khi WebSocket kết nối xong
     * sẽ được giữ lại ở đây.
     */
    private final Queue<String> pendingMessages =
            new ConcurrentLinkedQueue<>();

    /*
     * Tránh gọi connect() nhiều lần.
     */
    private volatile boolean connecting;

    // =====================================================
    // SETTERS
    // =====================================================

    public void setGameScene(
            GameScene gameScene) {

        this.gameScene =
                gameScene;
    }

    public void setMessageHandler(
            Consumer<String> messageHandler) {

        this.messageHandler =
                messageHandler;
    }

    // =====================================================
    // CONNECT
    // =====================================================

    public synchronized void connect() {

        /*
         * Đã connected hoặc đang connecting
         * thì không tạo WebSocket thứ hai.
         */
        if (webSocket != null
                && !webSocket.isOutputClosed()) {

            return;
        }

        if (connecting) {
            return;
        }

        connecting = true;

        HttpClient client =
                HttpClient.newHttpClient();

        client.newWebSocketBuilder()
                .buildAsync(
                        URI.create(SERVER_URI),
                        this)
                .thenAccept(ws -> {

                    System.out.println(
                            "WebSocket connection established");

                })
                .exceptionally(error -> {

                    connecting = false;

                    System.err.println(
                            "WebSocket connection failed: "
                                    + error.getMessage());

                    notifyMessage(
                            "CONNECTION_ERROR|"
                                    + error.getMessage());

                    return null;
                });
    }

    // =====================================================
    // SEND
    // =====================================================

    public void send(
            String message) {

        if (message == null
                || message.isBlank()) {

            return;
        }

        WebSocket ws =
                webSocket;

        /*
         * Socket chưa sẵn sàng:
         * giữ message lại.
         */
        if (ws == null
                || ws.isOutputClosed()) {

            pendingMessages.offer(
                    message);

            connect();

            return;
        }

        sendNow(
                ws,
                message);
    }

    private void sendNow(
            WebSocket ws,
            String message) {

        if (ws == null
                || ws.isOutputClosed()) {

            pendingMessages.offer(
                    message);

            return;
        }

        ws.sendText(
                message,
                true)
                .exceptionally(error -> {

                    System.err.println(
                            "WebSocket send failed: "
                                    + error.getMessage());

                    if (webSocket == null
                            || webSocket.isOutputClosed()) {

                        pendingMessages.offer(
                                message);
                    }

                    return null;
                });
    }

    public void sendInput(
            String input) {

        send(input);
    }

    // =====================================================
    // FLUSH PENDING MESSAGES
    // =====================================================

    private void flushPendingMessages(
            WebSocket ws) {

        String message;

        while ((message =
                pendingMessages.poll()) != null) {

            sendNow(
                    ws,
                    message);
        }
    }

    // =====================================================
    // RECEIVE
    // =====================================================

    @Override
    public void onOpen(
            WebSocket webSocket) {

        this.webSocket =
                webSocket;

        this.connecting =
                false;

        System.out.println(
                "WebSocket connected");

        flushPendingMessages(
                webSocket);

        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last) {

        String message =
                data.toString();

        System.out.println(
                "[WS RECEIVE] "
                        + message);

        /*
         * =================================================
         * GAMEPLAY
         * =================================================
         *
         * Giữ nguyên đường đi hiện tại để không
         * ảnh hưởng prediction/reconciliation.
         */
        GameScene scene =
                gameScene;

        if (scene != null) {

            if (message.startsWith(
                    "WELCOME|")) {

                String playerId =
                        message.substring(
                                "WELCOME|".length());

                Platform.runLater(
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

        /*
         * =================================================
         * LOBBY / APP
         * =================================================
         */
        if (isLobbyMessage(message)) {

            notifyMessage(
                    message);
        }

        webSocket.request(1);

        return WebSocket.Listener.super.onText(
                webSocket,
                data,
                last);
    }

    // =====================================================
    // LOBBY MESSAGE CHECK
    // =====================================================

    private boolean isLobbyMessage(
            String message) {

        return message.startsWith(
                    "WELCOME|")

                || message.startsWith(
                    "ROOM_CREATED|")

                || message.startsWith(
                    "ROOM_JOINED|")

                || message.startsWith(
                    "ROOM_STATE|")

                || message.startsWith(
                    "ROOM_LIST|")

                || message.startsWith(
                    "ROOM_ERROR|")

                || message.startsWith(
                    "ROOM_LEFT|")

                || message.startsWith(
                    "PLAYER_LEFT|")

                || message.startsWith(
                    "GAME_STARTED|")

                || message.startsWith(
                    "CONNECTION_ERROR|");
    }

    // =====================================================
    // MESSAGE HANDLER
    // =====================================================

    private void notifyMessage(
            String message) {

        Consumer<String> handler =
                messageHandler;

        if (handler == null) {
            return;
        }

        Platform.runLater(
                () -> handler.accept(message));
    }

    // =====================================================
    // CLOSE
    // =====================================================

    @Override
    public CompletionStage<?> onClose(
            WebSocket webSocket,
            int statusCode,
            String reason) {

        System.out.println(
                "Disconnected: "
                        + reason);

        if (this.webSocket == webSocket) {

            this.webSocket = null;
        }

        connecting = false;

        notifyMessage(
                "CONNECTION_CLOSED|"
                        + statusCode
                        + "|"
                        + reason);

        return WebSocket.Listener.super.onClose(
                webSocket,
                statusCode,
                reason);
    }

    // =====================================================
    // ERROR
    // =====================================================

    @Override
    public void onError(
            WebSocket webSocket,
            Throwable error) {

        System.err.println(
                "WebSocket error: "
                        + error.getMessage());

        notifyMessage(
                "CONNECTION_ERROR|"
                        + error.getMessage());
    }
}