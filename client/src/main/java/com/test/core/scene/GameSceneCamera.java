package com.test.core.scene;

import com.test.common.map.MapData;

public class GameSceneCamera {

    private static final double VIEW_WIDTH = 800;
    private static final double VIEW_HEIGHT = 600;

    private double cameraY = 0;

    private final GameScene scene;

    public GameSceneCamera(
            GameScene scene) {

        this.scene = scene;
    }

    public double getCameraY() {
        return cameraY;
    }

    public void update() {

        MapData mapData =
                scene.getMapData();

        if (mapData == null) {
            return;
        }

        double desiredCameraY =
                scene.getPlayer().getY()
                        - VIEW_HEIGHT / 2.0;

        double maxCameraY =
                Math.max(
                        0,
                        mapData.getHeight()
                                - VIEW_HEIGHT);

        cameraY =
                Math.max(
                        0,
                        Math.min(
                                desiredCameraY,
                                maxCameraY));

        scene.getWorld()
                .setTranslateX(0);

        scene.getWorld()
                .setTranslateY(
                        -cameraY);
    }
}