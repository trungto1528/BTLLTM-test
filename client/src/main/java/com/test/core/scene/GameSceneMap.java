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

                        Platform.runLater(() -> {

                            mapData =
                                    loadedMap;

                            mapRenderer.setMap(
                                    mapData);

                            scene.getWorld()
                                    .setPrefSize(
                                            mapData.getWidth(),
                                            mapData.getHeight());

                            if (mapData.getSpawn()
                                    == null) {

                                throw new IllegalStateException(
                                        "Map spawn is missing: "
                                                + mapId);
                            }

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
                                    "Không thể tải bản đồ "
                                            + mapId);

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