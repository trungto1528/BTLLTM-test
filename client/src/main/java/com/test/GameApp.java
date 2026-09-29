package com.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameApp extends Application {

    private GameWebSocketClient network;

    @Override
    public void start(Stage stage) {

        network = new GameWebSocketClient();

        GameScene gameScene = new GameScene(network);

        // Cho WebSocket biết GameScene để cập nhật state từ server
        network.setGameScene(gameScene);

        // Sau khi đã thiết lập liên kết
        network.connect();

        Scene scene = new Scene(gameScene, 800, 600);

        stage.setTitle("Jump King Multiplayer");
        stage.setScene(scene);
        stage.show();

        gameScene.requestFocus();
        gameScene.startLoop();
    }
}