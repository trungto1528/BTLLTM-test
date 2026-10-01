package com.test.common.map;

public final class MapFactory {

    private MapFactory() {
    }

    public static MapData createMap01() {
        MapData map =
                new MapData(
                        800,
                        6000,
                        40);

        addFloor(map, 149);
        addWalls(map, 0, 19, 0, 149);

        int[][] platforms = {
                {1, 145, 5},
                {8, 136, 4},
                {13, 127, 3},
                {7, 118, 4},
                {2, 108, 5},
                {10, 99, 4},
                {15, 91, 3},
                {9, 83, 4},
                {4, 74, 4},
                {11, 65, 3},
                {15, 57, 4},
                {8, 49, 3},
                {3, 41, 4},
                {10, 33, 4},
                {15, 25, 3},
                {9, 18, 4},
                {4, 11, 3}
        };

        for (int[] platform : platforms) {
            addPlatform(
                    map,
                    platform[0],
                    platform[1],
                    platform[2]);
        }

        int[][] leftSlopes = {
                {7, 139},
                {6, 121},
                {14, 94},
                {13, 68},
                {12, 36},
                {8, 14}
        };

        for (int[] slope : leftSlopes) {
            map.addCell(
                    new MapCellData(
                            slope[0],
                            slope[1],
                            MapCellType.TRIANGLE_LEFT));
        }

        int[][] rightSlopes = {
                {12, 130},
                {11, 102},
                {8, 77},
                {7, 52},
                {14, 21}
        };

        for (int[] slope : rightSlopes) {
            map.addCell(
                    new MapCellData(
                            slope[0],
                            slope[1],
                            MapCellType.TRIANGLE_RIGHT));
        }

        return map;
    }

    private static void addFloor(
            MapData map,
            int gridY) {

        for (int x = 0; x < map.getColumns(); x++) {
            addCell(
                    map,
                    x,
                    gridY,
                    MapCellType.SQUARE);
        }
    }

    private static void addWalls(
            MapData map,
            int left,
            int right,
            int fromY,
            int toY) {

        for (int y = fromY; y <= toY; y++) {
            addCell(
                    map,
                    left,
                    y,
                    MapCellType.SQUARE);
            addCell(
                    map,
                    right,
                    y,
                    MapCellType.SQUARE);
        }
    }

    private static void addPlatform(
            MapData map,
            int gridX,
            int gridY,
            int width) {

        for (int x = 0; x < width; x++) {
            addCell(
                    map,
                    gridX + x,
                    gridY,
                    MapCellType.SQUARE);
        }
    }

    private static void addCell(
            MapData map,
            int gridX,
            int gridY,
            MapCellType type) {

        if (!map.isInside(gridX, gridY)) {
            return;
        }

        if (map.getCell(gridX, gridY) != null) {
            return;
        }

        map.addCell(
                new MapCellData(
                        gridX,
                        gridY,
                        type));
    }
}
