
package com.test.common.map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MapCellData {

    private final int gridX;
    private final int gridY;
    private final MapCellType type;

    @JsonCreator
    public MapCellData(
            @JsonProperty("gridX") int gridX,
            @JsonProperty("gridY") int gridY,
            @JsonProperty("type") MapCellType type) {

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