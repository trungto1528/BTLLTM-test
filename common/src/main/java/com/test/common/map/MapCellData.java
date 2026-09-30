package com.test.common.map;

public class MapCellData {

    private final int gridX;
    private final int gridY;
    private final MapCellType type;

    public MapCellData(
            int gridX,
            int gridY,
            MapCellType type) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "type must not be null");
        }

        this.gridX = gridX;
        this.gridY = gridY;
        this.type = type;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public MapCellType getType() {
        return type;
    }

    public boolean isEmpty() {
        return type == MapCellType.EMPTY;
    }

    public boolean hasCollision() {
        return type != MapCellType.EMPTY;
    }
}