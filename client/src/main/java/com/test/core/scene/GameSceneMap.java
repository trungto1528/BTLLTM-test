package com.test.core.scene;

import com.test.common.map.MapData;
import com.test.core.GameMapLoader;
import com.test.core.GameMapRenderer;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class GameSceneMap {

    private final GameScene scene;

    private final GameMapRenderer mapRenderer =
            new GameMapRenderer();

    private MapData mapData;

    public GameSceneMap(
            GameScene scene) {

        this.scene = scene;
    }

    public MapData getMapData() {
        return mapData;
    }

    public GameMapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void loadAsync() {

        Thread mapLoaderThread =
                new Thread(() -> {

                    try {

                        MapData loadedMap =
                                new GameMapLoader()
                                        .load("map01");

                        Platform.runLater(() -> {

                            mapData =
                                    loadedMap;

                            mapRenderer.setMap(
                                    mapData);

                            scene.getWorld()
                                    .setPrefSize(
                                            mapData.getWidth(),
                                            mapData.getHeight());

                            scene.getWorld()
                                    .getChildren()
                                    .addAll(
                                            0,
                                            mapRenderer
                                                    .getMapNodes());

                            scene.getCamera()
                                    .update();
                        });

                    } catch (Exception e) {

                        e.printStackTrace();

                        Platform.runLater(() -> {

                            Alert alert =
                                    new Alert(
                                            Alert.AlertType.ERROR);

                            alert.setTitle(
                                    "Map loading failed");

                            alert.setHeaderText(
                                    "Không thể tải bản đồ map01");

                            alert.setContentText(
                                    e.getMessage());

                            alert.showAndWait();
                        });
                    }

                }, "game-map-loader");

        mapLoaderThread.setDaemon(true);

        mapLoaderThread.start();
    }
}