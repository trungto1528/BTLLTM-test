package com.test.map;

import org.springframework.stereotype.Component;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

@Component
public class GameMap {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 6000;
    public static final int CELL_SIZE = 40;

    private final MapData map;

    public GameMap() {
        map = new MapData(
                WIDTH,
                HEIGHT,
                CELL_SIZE);

        buildMap();
    }

    public MapData getMap() {
        return map;
    }

    private void buildMap() {
        addFloor();
        addWalls();

        addPlatform(1, 145, 5);
        addPlatform(8, 136, 4);
        addPlatform(13, 127, 3);
        addPlatform(7, 118, 4);
        addPlatform(2, 108, 5);

        addPlatform(10, 99, 4);
        addPlatform(15, 91, 3);
        addPlatform(9, 83, 4);
        addPlatform(4, 74, 4);

        addPlatform(11, 65, 3);
        addPlatform(15, 57, 4);
        addPlatform(8, 49, 3);
        addPlatform(3, 41, 4);

        addPlatform(10, 33, 4);
        addPlatform(15, 25, 3);
        addPlatform(9, 18, 4);
        addPlatform(4, 11, 3);

        addTriangleSlope(7, 139, true);
        addTriangleSlope(12, 130, false);
        addTriangleSlope(6, 121, true);
        addTriangleSlope(11, 102, false);
        addTriangleSlope(14, 94, true);
        addTriangleSlope(8, 77, false);
        addTriangleSlope(13, 68, true);
        addTriangleSlope(7, 52, false);
        addTriangleSlope(12, 36, true);
        addTriangleSlope(14, 21, false);
        addTriangleSlope(8, 14, true);
    }

    private void addFloor() {
        for (int x = 0; x < 20; x++) {
            addCell(
                    x,
                    149,
                    MapCellType.SQUARE);
        }
    }

    private void addWalls() {
        for (int y = 0; y < 150; y++) {
            addCell(
                    0,
                    y,
                    MapCellType.SQUARE);

            addCell(
                    19,
                    y,
                    MapCellType.SQUARE);
        }
    }

    private void addPlatform(
            int gridX,
            int gridY,
            int width) {

        for (int x = 0; x < width; x++) {
            if (gridX + x >= 1
                    && gridX + x < 19) {

                addCell(
                        gridX + x,
                        gridY,
                        MapCellType.SQUARE);
            }
        }
    }

    private void addTriangleSlope(
            int gridX,
            int gridY,
            boolean left) {

        if (gridX < 1 || gridX >= 19) {
            return;
        }

        addCell(
                gridX,
                gridY,
                left
                        ? MapCellType.TRIANGLE_LEFT
                        : MapCellType.TRIANGLE_RIGHT);
    }

    private void addCell(
            int gridX,
            int gridY,
            MapCellType type) {

        if (!map.isInside(gridX, gridY)) {
            return;
        }

        map.addCell(
                new MapCellData(
                        gridX,
                        gridY,
                        type));
    }
}