package com.test.editor;

import java.util.function.Supplier;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

public class MapCanvas extends Canvas {

    private static final double GRID_LINE_WIDTH = 1.0;

    private final MapData mapData;
    private final Supplier<MapCellType> selectedCellTypeSupplier;

    public MapCanvas(
            MapData mapData,
            Supplier<MapCellType> selectedCellTypeSupplier) {

        if (mapData == null) {
            throw new IllegalArgumentException(
                    "mapData must not be null");
        }

        if (selectedCellTypeSupplier == null) {
            throw new IllegalArgumentException(
                    "selectedCellTypeSupplier must not be null");
        }

        this.mapData = mapData;
        this.selectedCellTypeSupplier =
                selectedCellTypeSupplier;

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

        setOnMouseClicked(event -> {

            if (event.getButton()
                    != MouseButton.PRIMARY) {

                return;
            }

            int cellSize =
                    mapData.getCellSize();

            int gridX =
                    (int) (event.getX()
                            / cellSize);

            int gridY =
                    (int) (event.getY()
                            / cellSize);

            if (!mapData.isInside(
                    gridX,
                    gridY)) {

                return;
            }

            MapCellType selectedType =
                    selectedCellTypeSupplier.get();

            mapData.setCell(
                    gridX,
                    gridY,
                    selectedType);

            draw();
        });
    }

    private void draw() {

        GraphicsContext graphics =
                getGraphicsContext2D();

        graphics.clearRect(
                0,
                0,
                getWidth(),
                getHeight());

        drawBackground(graphics);
        drawCells(graphics);
        drawGrid(graphics);
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

        for (MapCellData cell :
                mapData.getCells()) {

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

        switch (type) {

            case SQUARE:

                graphics.setFill(
                        Color.DARKGRAY);

                graphics.fillRect(
                        x,
                        y,
                        size,
                        size);

                break;

            case TRIANGLE_LEFT:

                graphics.setFill(
                        Color.DARKGRAY);

                graphics.fillPolygon(
                        new double[]{
                                x,
                                x + size,
                                x
                        },
                        new double[]{
                                y,
                                y + size,
                                y + size
                        },
                        3);

                break;

            case TRIANGLE_RIGHT:

                graphics.setFill(
                        Color.DARKGRAY);

                graphics.fillPolygon(
                        new double[]{
                                x,
                                x + size,
                                x + size
                        },
                        new double[]{
                                y,
                                y,
                                y + size
                        },
                        3);

                break;

            case EMPTY:

                break;
        }
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