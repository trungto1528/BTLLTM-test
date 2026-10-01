package com.test.core.scene;

import java.io.IOException;

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

    public void loadAsync(
            String mapId) {

        if (mapId == null
                || mapId.isBlank()) {

            throw new IllegalArgumentException(
                    "mapId must not be blank");
        }

        Thread mapLoaderThread =
                new Thread(() -> {

                    try {

                        MapData loadedMap =
                                new GameMapLoader()
                                        .load(mapId);

                        if (loadedMap == null) {

                            throw new IllegalStateException(
                                    "Loaded map is null: "
                                            + mapId);
                        }

                        if (loadedMap.getSpawn()
                                == null) {

                            throw new IllegalStateException(
                                    "Map spawn is missing: "
                                            + mapId);
                        }

                        Platform.runLater(() -> {

                            mapData =
                                    loadedMap;

                            mapRenderer.setMap(
                                    mapData);

                            scene.getWorld()
                                    .setPrefSize(
                                            mapData.getWidth(),
                                            mapData.getHeight());

                            scene.setMapSpawn(
                                    mapData.getSpawn()
                                            .getX(),
                                    mapData.getSpawn()
                                            .getY());

                            scene.getWorld()
                                    .getChildren()
                                    .addAll(
                                            0,
                                            mapRenderer
                                                    .getMapNodes());

                            scene.getCamera()
                                    .update();

                            /*
                             * Chỉ bắt đầu simulation sau khi:
                             *
                             * 1. Map đã load.
                             * 2. Spawn đã được đặt.
                             * 3. Map đã được render.
                             *
                             * Như vậy GameSceneSimulation sẽ
                             * không chạy với MapData == null.
                             */
                            scene.startLoop();
                        });

                    } catch (IOException | IllegalStateException | InterruptedException e) {
                        Platform.runLater(() -> {

                            Alert alert =
                                    new Alert(
                                            Alert.AlertType.ERROR);

                            alert.setTitle(
                                    "Map loading failed");

                            alert.setHeaderText(
                                    "Không thể tải bản đồ "
                                            + mapId);

                            String message =
                                    e.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        e.getClass()
                                                .getSimpleName();
                            }

                            alert.setContentText(
                                    message);

                            alert.showAndWait();
                        });
                    }

                }, "game-map-loader");

        mapLoaderThread.setDaemon(true);

        mapLoaderThread.start();
    }
}