package com.test.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class GameMapRenderer {

    private final List<Node> mapNodes =
            new ArrayList<>();

    private MapData map;

    public GameMapRenderer() {
    }

    public void setMap(MapData map) {
        if (map == null) {
            throw new IllegalArgumentException(
                    "map must not be null");
        }

        this.map = map;
        mapNodes.clear();

        for (MapCellData cell : map.getCells()) {
            addCell(cell);
        }
    }

    public MapData getMap() {
        return map;
    }

    public List<Node> getMapNodes() {
        return Collections.unmodifiableList(
                mapNodes);
    }

    public double getWidth() {
        if (map == null) {
            return 0;
        }

        return map.getWidth();
    }

    public double getHeight() {
        if (map == null) {
            return 0;
        }

        return map.getHeight();
    }

    public double getCellSize() {
        if (map == null) {
            return 0;
        }

        return map.getCellSize();
    }

    private void addCell(MapCellData cell) {
        double x =
                cell.getGridX()
                        * map.getCellSize();

        double y =
                cell.getGridY()
                        * map.getCellSize();

        double size =
                map.getCellSize();

        switch (cell.getType()) {
            case SQUARE ->
                    addSquare(
                            x,
                            y,
                            size);

            case TRIANGLE_LEFT ->
                    addTriangleLeft(
                            x,
                            y,
                            size);

            case TRIANGLE_RIGHT ->
                    addTriangleRight(
                            x,
                            y,
                            size);

            case EMPTY -> {
            }
        }
    }

    private void addSquare(
            double x,
            double y,
            double size) {

        Rectangle rectangle =
                new Rectangle(
                        size,
                        size);

        rectangle.setX(x);
        rectangle.setY(y);

        rectangle.setFill(
                Color.DARKGREEN);

        mapNodes.add(
                rectangle);
    }

    private void addTriangleLeft(
            double x,
            double y,
            double size) {

        Polygon triangle =
                new Polygon(
                        x,
                        y + size,
                        x + size,
                        y,
                        x + size,
                        y + size);

        triangle.setFill(
                Color.DARKGREEN);

        mapNodes.add(
                triangle);
    }

    private void addTriangleRight(
            double x,
            double y,
            double size) {

        Polygon triangle =
                new Polygon(
                        x,
                        y,
                        x,
                        y + size,
                        x + size,
                        y + size);

        triangle.setFill(
                Color.DARKGREEN);

        mapNodes.add(
                triangle);
    }
}