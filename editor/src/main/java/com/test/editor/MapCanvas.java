package com.test.editor;

import java.util.HashSet;
import java.util.Set;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class MapCanvas extends Canvas {

    private static final double GRID_LINE_WIDTH = 1.0;

    private final MapData mapData;

    private final Supplier<MapCellType>
            selectedCellTypeSupplier;

    private final Runnable cycleCellType;

    private final BooleanSupplier
            spawnModeSupplier;

    private final Consumer<MapSpawnData>
            spawnConsumer;

    private final Runnable changeListener;

    private final Set<Long> paintedCells =
            new HashSet<>();

    private int lastGridX = -1;
    private int lastGridY = -1;

    public MapCanvas(
            MapData mapData,
            Supplier<MapCellType> selectedCellTypeSupplier,
            Runnable cycleCellType,
            BooleanSupplier spawnModeSupplier,
            Consumer<MapSpawnData> spawnConsumer,
            Runnable changeListener) {

        if (mapData == null
                || selectedCellTypeSupplier == null
                || cycleCellType == null
                || spawnModeSupplier == null
                || spawnConsumer == null
                || changeListener == null) {

            throw new IllegalArgumentException(
                    "MapCanvas arguments must not be null");
        }

        this.mapData =
                mapData;

        this.selectedCellTypeSupplier =
                selectedCellTypeSupplier;

        this.cycleCellType =
                cycleCellType;

        this.spawnModeSupplier =
                spawnModeSupplier;

        this.spawnConsumer =
                spawnConsumer;

        this.changeListener =
                changeListener;

        setWidth(
                mapData.getWidth());

        setHeight(
                mapData.getHeight());

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

        setOnMousePressed(
                this::handleMousePressed);

        setOnMouseDragged(
                this::handleMouseDragged);

        setOnMouseReleased(
                this::handleMouseReleased);
    }

    private void handleMousePressed(
            MouseEvent event) {

        if (event.getButton()
                == MouseButton.SECONDARY) {

            cycleCellType();

            event.consume();

            return;
        }

        if (event.getButton()
                != MouseButton.PRIMARY) {

            return;
        }

        paintedCells.clear();

        lastGridX = -1;
        lastGridY = -1;

        if (spawnModeSupplier
                .getAsBoolean()) {

            handleSpawn(
                    event.getX(),
                    event.getY());

            event.consume();

            return;
        }

        paintAt(
                event.getX(),
                event.getY());

        event.consume();
    }

    private void handleMouseDragged(
            MouseEvent event) {

        if (!event.isPrimaryButtonDown()) {
            return;
        }

        if (spawnModeSupplier
                .getAsBoolean()) {

            return;
        }

        paintAt(
                event.getX(),
                event.getY());

        event.consume();
    }

    private void handleMouseReleased(
            MouseEvent event) {

        if (event.getButton()
                != MouseButton.PRIMARY) {

            return;
        }

        paintedCells.clear();

        lastGridX = -1;
        lastGridY = -1;

        event.consume();
    }

    private void handleSpawn(
            double x,
            double y) {

        if (x < 0
                || x >= mapData.getWidth()
                || y < 0
                || y >= mapData.getHeight()) {

            return;
        }

        spawnConsumer.accept(
                new MapSpawnData(
                        x,
                        y));
    }

    private void paintAt(
            double x,
            double y) {

        int cellSize =
                mapData.getCellSize();

        if (x < 0
                || x >= mapData.getWidth()
                || y < 0
                || y >= mapData.getHeight()) {

            return;
        }

        int gridX =
                (int) (x / cellSize);

        int gridY =
                (int) (y / cellSize);

        if (!mapData.isInside(
                gridX,
                gridY)) {

            return;
        }

        if (lastGridX < 0
                || lastGridY < 0) {

            boolean changed =
                    paintLine(
                            gridX,
                            gridY,
                            gridX,
                            gridY);

            lastGridX =
                    gridX;

            lastGridY =
                    gridY;

            if (changed) {

                draw();

                changeListener.run();
            }

            return;
        }

        if (gridX == lastGridX
                && gridY == lastGridY) {

            return;
        }

        boolean changed =
                paintLine(
                        lastGridX,
                        lastGridY,
                        gridX,
                        gridY);

        lastGridX =
                gridX;

        lastGridY =
                gridY;

        if (changed) {

            draw();

            changeListener.run();
        }
    }

    private boolean paintLine(
            int startX,
            int startY,
            int endX,
            int endY) {

        boolean changed = false;

        int x = startX;
        int y = startY;

        int dx =
                Math.abs(
                        endX - startX);

        int dy =
                Math.abs(
                        endY - startY);

        int sx =
                startX < endX
                        ? 1
                        : -1;

        int sy =
                startY < endY
                        ? 1
                        : -1;

        int error =
                dx - dy;

        while (true) {

            if (paintCell(
                    x,
                    y)) {

                changed = true;
            }

            if (x == endX
                    && y == endY) {

                break;
            }

            int doubleError =
                    error * 2;

            if (doubleError > -dy) {

                error -= dy;

                x += sx;
            }

            if (doubleError < dx) {

                error += dx;

                y += sy;
            }
        }

        return changed;
    }

    private boolean paintCell(
            int gridX,
            int gridY) {

        if (!mapData.isInside(
                gridX,
                gridY)) {

            return false;
        }

        long key =
                createCellKey(
                        gridX,
                        gridY);

        if (!paintedCells.add(key)) {
            return false;
        }

        MapCellType selectedType =
                selectedCellTypeSupplier.get();

        if (selectedType == null) {
            return false;
        }

        MapCellData currentCell =
                mapData.getCell(
                        gridX,
                        gridY);

        MapCellType currentType =
                currentCell == null
                        ? MapCellType.EMPTY
                        : currentCell.getType();

        MapCellType newType;

        if (currentType == selectedType) {

            newType =
                    MapCellType.EMPTY;

        } else {

            newType =
                    selectedType;
        }

        if (currentType == newType) {
            return false;
        }

        mapData.setCell(
                gridX,
                gridY,
                newType);

        return true;
    }

    private long createCellKey(
            int gridX,
            int gridY) {

        return ((long) gridX << 32)
                | (gridY & 0xffffffffL);
    }

    private void cycleCellType() {

        cycleCellType.run();
    }

    private void draw() {

        GraphicsContext graphics =
                getGraphicsContext2D();

        graphics.clearRect(
                0,
                0,
                getWidth(),
                getHeight());

        drawBackground(
                graphics);

        drawCells(
                graphics);

        drawSpawn(
                graphics);

        drawGrid(
                graphics);
    }

    private void drawBackground(
            GraphicsContext graphics) {

        graphics.setFill(
                Color.WHITE);

        graphics.fillRect(
                0,
                0,
                getWidth(),
                getHeight());
    }

    private void drawCells(
            GraphicsContext graphics) {

        int cellSize =
                mapData.getCellSize();

        for (MapCellData cell
                : mapData.getCells()) {

            double x =
                    cell.getGridX()
                            * cellSize;

            double y =
                    cell.getGridY()
                            * cellSize;

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

        graphics.setFill(
                Color.DARKGRAY);

        switch (type) {

            case SQUARE:

                graphics.fillRect(
                        x,
                        y,
                        size,
                        size);

                break;

            case TRIANGLE_LEFT:

                graphics.beginPath();

                graphics.moveTo(
                        x + size,
                        y + size);

                graphics.lineTo(
                        x + size,
                        y);

                graphics.lineTo(
                        x,
                        y + size);

                graphics.closePath();

                graphics.fill();

                break;

            case TRIANGLE_RIGHT:

                graphics.beginPath();

                graphics.moveTo(
                        x,
                        y);

                graphics.lineTo(
                        x + size,
                        y + size);

                graphics.lineTo(
                        x,
                        y + size);

                graphics.closePath();

                graphics.fill();

                break;

            case EMPTY:
                break;
        }
    }

    private void drawSpawn(
            GraphicsContext graphics) {

        MapSpawnData spawn =
                mapData.getSpawn();

        if (spawn == null) {
            return;
        }

        double x =
                spawn.getX();

        double y =
                spawn.getY();

        graphics.setStroke(
                Color.DODGERBLUE);

        graphics.setLineWidth(3);

        graphics.strokeOval(
                x - 8,
                y - 8,
                16,
                16);

        graphics.strokeLine(
                x - 12,
                y,
                x + 12,
                y);

        graphics.strokeLine(
                x,
                y - 12,
                x,
                y + 12);

        graphics.setFill(
                Color.DODGERBLUE);

        graphics.fillText(
                "SPAWN",
                x + 12,
                y - 10);
    }

    private void drawGrid(
            GraphicsContext graphics) {

        int cellSize =
                mapData.getCellSize();

        int columns =
                mapData.getColumns();

        int rows =
                mapData.getRows();

        graphics.setStroke(
                Color.LIGHTGRAY);

        graphics.setLineWidth(
                GRID_LINE_WIDTH);

        for (int x = 0;
                x <= columns;
                x++) {

            double pixelX =
                    x * cellSize;

            graphics.strokeLine(
                    pixelX,
                    0,
                    pixelX,
                    mapData.getHeight());
        }

        for (int y = 0;
                y <= rows;
                y++) {

            double pixelY =
                    y * cellSize;

            graphics.strokeLine(
                    0,
                    pixelY,
                    mapData.getWidth(),
                    pixelY);
        }
    }
}