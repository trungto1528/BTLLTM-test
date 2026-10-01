
package com.test.editor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;
import com.test.common.map.MapSpawnData;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

public class MapCanvas extends Canvas {

    private static final double GRID_LINE_WIDTH = 1.0;

    private final MapData mapData;
    private final Supplier<MapCellType> selectedCellTypeSupplier;
    private final BooleanSupplier spawnModeSupplier;
    private final Consumer<MapSpawnData> spawnConsumer;
    private final Runnable changeListener;

    public MapCanvas(
            MapData mapData,
            Supplier<MapCellType> selectedCellTypeSupplier,
            BooleanSupplier spawnModeSupplier,
            Consumer<MapSpawnData> spawnConsumer,
            Runnable changeListener) {

        if (mapData == null
                || selectedCellTypeSupplier == null
                || spawnModeSupplier == null
                || spawnConsumer == null
                || changeListener == null) {
            throw new IllegalArgumentException(
                    "MapCanvas arguments must not be null");
        }

        this.mapData = mapData;
        this.selectedCellTypeSupplier =
                selectedCellTypeSupplier;
        this.spawnModeSupplier = spawnModeSupplier;
        this.spawnConsumer = spawnConsumer;
        this.changeListener = changeListener;

        setWidth(mapData.getWidth());
        setHeight(mapData.getHeight());

        setupMouseHandling();
        draw();
    }

    public MapData getMapData() {
        return mapData;
    }

    public void refresh() {
        draw();
    }

    private void setupMouseHandling() {
        setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            double x = event.getX();
            double y = event.getY();

            if (x < 0 || x >= mapData.getWidth()
                    || y < 0 || y >= mapData.getHeight()) {
                return;
            }

            if (spawnModeSupplier.getAsBoolean()) {
                spawnConsumer.accept(
                        new MapSpawnData(x, y));
                return;
            }

            int cellSize = mapData.getCellSize();
            int gridX = (int) (x / cellSize);
            int gridY = (int) (y / cellSize);

            mapData.setCell(
                    gridX,
                    gridY,
                    selectedCellTypeSupplier.get());

            draw();
            changeListener.run();
        });
    }

    private void draw() {
        GraphicsContext graphics = getGraphicsContext2D();

        graphics.clearRect(
                0, 0, getWidth(), getHeight());

        drawBackground(graphics);
        drawCells(graphics);
        drawSpawn(graphics);
        drawGrid(graphics);
    }

    private void drawBackground(GraphicsContext graphics) {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(
                0, 0, getWidth(), getHeight());
    }

    private void drawCells(GraphicsContext graphics) {
        int cellSize = mapData.getCellSize();

        for (MapCellData cell : mapData.getCells()) {
            double x = cell.getGridX() * cellSize;
            double y = cell.getGridY() * cellSize;

            drawCell(
                    graphics,
                    cell.getType(),
                    x,
                    y,
                    cellSize);
        }
    }

    private void drawCell(
            GraphicsContext graphics,
            MapCellType type,
            double x,
            double y,
            double size) {

        graphics.setFill(Color.DARKGRAY);

        switch (type) {
            case SQUARE -> graphics.fillRect(x, y, size, size);

            case TRIANGLE_LEFT -> {
                graphics.beginPath();
                graphics.moveTo(x + size, y + size);
                graphics.lineTo(x + size, y);
                graphics.lineTo(x, y + size);
                graphics.closePath();
                graphics.fill();
            }

            case TRIANGLE_RIGHT -> {
                graphics.beginPath();
                graphics.moveTo(x, y);
                graphics.lineTo(x + size, y + size);
                graphics.lineTo(x, y + size);
                graphics.closePath();
                graphics.fill();
            }

            case EMPTY -> {
            }
        }
    }

    private void drawSpawn(GraphicsContext graphics) {
        MapSpawnData spawn = mapData.getSpawn();

        if (spawn == null) {
            return;
        }

        double x = spawn.getX();
        double y = spawn.getY();

        graphics.setStroke(Color.DODGERBLUE);
        graphics.setLineWidth(3);

        graphics.strokeOval(
                x - 8, y - 8, 16, 16);

        graphics.strokeLine(
                x - 12, y, x + 12, y);

        graphics.strokeLine(
                x, y - 12, x, y + 12);

        graphics.setFill(Color.DODGERBLUE);
        graphics.fillText(
                "SPAWN",
                x + 12,
                y - 10);
    }

    private void drawGrid(GraphicsContext graphics) {
        int cellSize = mapData.getCellSize();
        int columns = mapData.getColumns();
        int rows = mapData.getRows();

        graphics.setStroke(Color.LIGHTGRAY);
        graphics.setLineWidth(GRID_LINE_WIDTH);

        for (int x = 0; x <= columns; x++) {
            double pixelX = x * cellSize;

            graphics.strokeLine(
                    pixelX, 0,
                    pixelX, mapData.getHeight());
        }

        for (int y = 0; y <= rows; y++) {
            double pixelY = y * cellSize;

            graphics.strokeLine(
                    0, pixelY,
                    mapData.getWidth(), pixelY);
        }
    }
}