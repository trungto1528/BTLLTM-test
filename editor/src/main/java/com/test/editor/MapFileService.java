
package com.test.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;
import com.test.common.map.MapSpawnData;

public class MapFileService {

    private final ObjectMapper objectMapper;

    public MapFileService() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT);
    }

    public void save(
            MapDocument document,
            Path path) throws IOException {

        if (document == null) {
            throw new IllegalArgumentException(
                    "document must not be null");
        }

        if (path == null) {
            throw new IllegalArgumentException(
                    "path must not be null");
        }

        MapData map = document.mapData();

        MapFile file = new MapFile(
                document.id(),
                document.name(),
                map.getWidth(),
                map.getHeight(),
                map.getCellSize(),
                map.getSpawn(),
                collectCells(map, MapCellType.SQUARE),
                collectCells(map, MapCellType.TRIANGLE_LEFT),
                collectCells(map, MapCellType.TRIANGLE_RIGHT));

        validateMapFile(file);

        objectMapper.writeValue(path.toFile(), file);
    }

    public MapDocument load(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException(
                    "path must not be null");
        }

        MapFile file = objectMapper.readValue(
                path.toFile(),
                MapFile.class);

        validateMapFile(file);

        MapData map = new MapData(
                file.width(),
                file.height(),
                file.cellSize(),
                file.spawn());

        addCells(map, file.square(), MapCellType.SQUARE);
        addCells(
                map,
                file.triangleLeft(),
                MapCellType.TRIANGLE_LEFT);
        addCells(
                map,
                file.triangleRight(),
                MapCellType.TRIANGLE_RIGHT);

        return new MapDocument(
                file.id(),
                file.name(),
                map);
    }

    private List<List<Integer>> collectCells(
            MapData map,
            MapCellType type) {

        List<List<Integer>> coordinates =
                new ArrayList<>();

        for (MapCellData cell : map.getCells()) {
            if (cell.getType() == type) {
                coordinates.add(List.of(
                        cell.getGridX(),
                        cell.getGridY()));
            }
        }

        return coordinates;
    }

    private void addCells(
            MapData map,
            List<List<Integer>> coordinates,
            MapCellType type) {

        if (coordinates == null) {
            return;
        }

        for (List<Integer> coordinate : coordinates) {
            if (coordinate == null
                    || coordinate.size() != 2
                    || coordinate.get(0) == null
                    || coordinate.get(1) == null) {
                throw new IllegalArgumentException(
                        "Invalid map coordinate: "
                                + coordinate);
            }

            int x = coordinate.get(0);
            int y = coordinate.get(1);

            if (!map.isInside(x, y)) {
                throw new IllegalArgumentException(
                        "Cell is outside map bounds: "
                                + x + ", " + y);
            }

            if (map.hasCell(x, y)) {
                throw new IllegalArgumentException(
                        "Duplicate map cell: "
                                + x + ", " + y);
            }

            map.addCell(new MapCellData(x, y, type));
        }
    }

    private void validateMapFile(MapFile file) {
        if (file == null) {
            throw new IllegalArgumentException(
                    "Map file must not be null");
        }

        if (file.id() == null || file.id().isBlank()) {
            throw new IllegalArgumentException(
                    "Map id must not be blank");
        }

        if (file.name() == null || file.name().isBlank()) {
            throw new IllegalArgumentException(
                    "Map name must not be blank");
        }

        if (file.width() <= 0
                || file.height() <= 0
                || file.cellSize() <= 0
                || file.width() % file.cellSize() != 0
                || file.height() % file.cellSize() != 0) {
            throw new IllegalArgumentException(
                    "Invalid map dimensions or cell size");
        }

        MapSpawnData spawn = file.spawn();

        if (spawn == null
                || spawn.getX() < 0
                || spawn.getX() >= file.width()
                || spawn.getY() < 0
                || spawn.getY() >= file.height()) {
            throw new IllegalArgumentException(
                    "Map spawn is outside map bounds");
        }
    }

    private record MapFile(
            String id,
            String name,
            int width,
            int height,
            int cellSize,
            MapSpawnData spawn,
            List<List<Integer>> square,
            List<List<Integer>> triangleLeft,
            List<List<Integer>> triangleRight) {
    }
}