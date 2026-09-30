package com.test.common.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapData {

    private final int width;
    private final int height;
    private final int cellSize;
    private final List<MapCellData> cells;

    public MapData(
            int width,
            int height,
            int cellSize) {

        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }

        if (cellSize <= 0) {
            throw new IllegalArgumentException("cellSize must be positive");
        }

        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.cells = new ArrayList<>();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCellSize() {
        return cellSize;
    }

    public int getColumns() {
        return width / cellSize;
    }

    public int getRows() {
        return height / cellSize;
    }

    public void addCell(MapCellData cell) {
        if (cell == null) {
            throw new IllegalArgumentException("cell must not be null");
        }

        cells.add(cell);
    }

    public List<MapCellData> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public MapCellData getCell(int gridX, int gridY) {
        for (MapCellData cell : cells) {
            if (cell.getGridX() == gridX
                    && cell.getGridY() == gridY) {
                return cell;
            }
        }

        return null;
    }

    public boolean isInside(int gridX, int gridY) {
        return gridX >= 0
                && gridX < getColumns()
                && gridY >= 0
                && gridY < getRows();
    }
}