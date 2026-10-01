package com.test.common.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MapData {

    private final int width;
    private final int height;
    private final int cellSize;
    private final MapSpawnData spawn;
    private final List<MapCellData> cells;

    public MapData(
            int width,
            int height,
            int cellSize) {

        this(
                width,
                height,
                cellSize,
                null,
                null);
    }

    public MapData(
            int width,
            int height,
            int cellSize,
            MapSpawnData spawn) {

        this(
                width,
                height,
                cellSize,
                spawn,
                null);
    }

    @JsonCreator
    public MapData(
            @JsonProperty("width") int width,
            @JsonProperty("height") int height,
            @JsonProperty("cellSize") int cellSize,
            @JsonProperty("spawn") MapSpawnData spawn,
            @JsonProperty("cells") List<MapCellData> cells) {

        if (width <= 0) {
            throw new IllegalArgumentException(
                    "width must be positive");
        }

        if (height <= 0) {
            throw new IllegalArgumentException(
                    "height must be positive");
        }

        if (cellSize <= 0) {
            throw new IllegalArgumentException(
                    "cellSize must be positive");
        }

        if (width % cellSize != 0) {
            throw new IllegalArgumentException(
                    "width must be divisible by cellSize");
        }

        if (height % cellSize != 0) {
            throw new IllegalArgumentException(
                    "height must be divisible by cellSize");
        }

        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.spawn = spawn;
        this.cells = new ArrayList<>();

        if (cells != null) {
            for (MapCellData cell : cells) {

                if (cell == null) {
                    throw new IllegalArgumentException(
                            "cells must not contain null");
                }

                if (!isInside(
                        cell.getGridX(),
                        cell.getGridY())) {

                    throw new IllegalArgumentException(
                            "Cell is outside map bounds: "
                                    + cell.getGridX()
                                    + ", "
                                    + cell.getGridY());
                }

                if (getCell(
                        cell.getGridX(),
                        cell.getGridY()) != null) {

                    throw new IllegalArgumentException(
                            "Duplicate map cell: "
                                    + cell.getGridX()
                                    + ", "
                                    + cell.getGridY());
                }

                this.cells.add(cell);
            }
        }
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

    public MapSpawnData getSpawn() {
        return spawn;
    }

    @JsonIgnore
    public int getColumns() {
        return width / cellSize;
    }

    @JsonIgnore
    public int getRows() {
        return height / cellSize;
    }

    public void addCell(
            MapCellData cell) {

        if (cell == null) {
            throw new IllegalArgumentException(
                    "cell must not be null");
        }

        if (!isInside(
                cell.getGridX(),
                cell.getGridY())) {

            throw new IllegalArgumentException(
                    "Cell is outside map bounds");
        }

        if (getCell(
                cell.getGridX(),
                cell.getGridY()) != null) {

            throw new IllegalArgumentException(
                    "Duplicate map cell: "
                            + cell.getGridX()
                            + ", "
                            + cell.getGridY());
        }

        cells.add(cell);
    }

    public List<MapCellData> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public MapCellData getCell(
            int gridX,
            int gridY) {

        for (MapCellData cell : cells) {

            if (cell.getGridX() == gridX
                    && cell.getGridY() == gridY) {

                return cell;
            }
        }

        return null;
    }

    public boolean isInside(
            int gridX,
            int gridY) {

        return gridX >= 0
                && gridX < getColumns()
                && gridY >= 0
                && gridY < getRows();
    }
}