package com.test.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.test.common.map.MapCellType;
import com.test.common.map.MapData;
import com.test.common.map.MapSpawnData;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class MapEditorScene extends BorderPane {

    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 6000;
    private static final int CELL_SIZE = 16;

    private final MapFileService mapFileService =
            new MapFileService();

    private Path lastMapDirectory =
            Path.of(
                    "server",
                    "src",
                    "main",
                    "resources",
                    "map")
                    .toAbsolutePath()
                    .normalize();

    private MapData mapData;
    private MapCanvas mapCanvas;
    private ScrollPane mapScrollPane;

    private String currentMapId;
    private String currentMapName;
    private Path currentMapPath;

    private final ToggleGroup cellTypeGroup =
            new ToggleGroup();

    private final ToggleButton emptyButton =
            createCellTypeButton("Empty");

    private final ToggleButton squareButton =
            createCellTypeButton("Square");

    private final ToggleButton triangleLeftButton =
            createCellTypeButton(
                    "Triangle Left");

    private final ToggleButton triangleRightButton =
            createCellTypeButton(
                    "Triangle Right");

    private final ToggleButton spawnButton =
            new ToggleButton("Set Spawn");

    private final TextField mapNameField =
            new TextField();

    private final Label mapIdLabel =
            new Label();

    private final Label statusLabel =
            new Label();

    private final Label mapInfoLabel =
            new Label();

    private final Button newButton =
            new Button("New");

    private final Button saveButton =
            new Button("Save");

    private final Button saveAsButton =
            new Button("Save As");

    private final Button loadButton =
            new Button("Load");

    public MapEditorScene() {

        createNewMap();

        squareButton.setSelected(true);

        mapNameField.setPromptText(
                "Map Name");

        mapNameField.setPrefColumnCount(10);

        spawnButton.setOnAction(
                event -> {

                    if (spawnButton.isSelected()) {

                        statusLabel.setText(
                                "Chế độ Spawn: "
                                        + "nhấp vào vị trí mới.");

                    } else {

                        updateStatus();
                    }
                });

        newButton.setOnAction(
                event -> createNewMap());

        saveButton.setOnAction(
                event -> saveMap());

        saveAsButton.setOnAction(
                event -> saveMapAs());

        loadButton.setOnAction(
                event -> loadMap());

        createTopBar();
        createMapView();
        createBottomBar();

        updateStatus();
    }

    private void createNewMap() {

        currentMapId =
                MapDocument.generateId();

        currentMapName =
                "New Map";

        currentMapPath =
                null;

        mapData =
                new MapData(
                        MAP_WIDTH,
                        MAP_HEIGHT,
                        CELL_SIZE,
                        new MapSpawnData(
                                180,
                                5930));

        mapNameField.setText(
                currentMapName);

        mapIdLabel.setText(
                currentMapId);

        spawnButton.setSelected(false);

        if (mapCanvas != null) {

            mapCanvas =
                    createMapCanvas();

            createMapView();
        }
    }

    private ToggleButton createCellTypeButton(
            String text) {

        ToggleButton button =
                new ToggleButton(text);

        button.setToggleGroup(
                cellTypeGroup);

        button.setOnAction(
                event -> {

                    spawnButton.setSelected(false);

                    updateStatus();
                });

        return button;
    }

    private MapCellType getSelectedCellType() {

        if (emptyButton.isSelected()) {
            return MapCellType.EMPTY;
        }

        if (triangleLeftButton.isSelected()) {
            return MapCellType.TRIANGLE_LEFT;
        }

        if (triangleRightButton.isSelected()) {
            return MapCellType.TRIANGLE_RIGHT;
        }

        return MapCellType.SQUARE;
    }

    private void cycleSelectedCellType() {

        if (emptyButton.isSelected()) {

            squareButton.setSelected(true);

        } else if (squareButton.isSelected()) {

            triangleLeftButton.setSelected(true);

        } else if (triangleLeftButton.isSelected()) {

            triangleRightButton.setSelected(true);

        } else {

            squareButton.setSelected(true);
        }

        spawnButton.setSelected(false);

        updateStatus();
    }

    private MapCanvas createMapCanvas() {

        return new MapCanvas(
                mapData,
                this::getSelectedCellType,
                this::cycleSelectedCellType,
                spawnButton::isSelected,
                this::updateSpawn,
                this::updateStatus);
    }

    private void createTopBar() {

        Label title =
                new Label(
                        "LTM Map Editor");

        title.setStyle(
                "-fx-font-size: 18px; "
                        + "-fx-font-weight: bold;");

        HBox metadata =
                new HBox(
                        6,
                        new Label("ID:"),
                        mapIdLabel,
                        new Label("Name:"),
                        mapNameField);

        HBox toolbar =
                new HBox(
                        8,
                        title,
                        metadata,
                        squareButton,
                        triangleLeftButton,
                        triangleRightButton,
                        emptyButton,
                        spawnButton,
                        newButton,
                        saveButton,
                        saveAsButton,
                        loadButton);

        toolbar.setPadding(
                new Insets(10));

        toolbar.setSpacing(8);

        setTop(toolbar);
    }

    private void createMapView() {

        if (mapScrollPane == null) {

            mapScrollPane =
                    new ScrollPane();

            mapScrollPane.setPannable(true);

            mapScrollPane.setFitToWidth(false);
            mapScrollPane.setFitToHeight(false);

            VBox.setVgrow(
                    mapScrollPane,
                    Priority.ALWAYS);

            setCenter(
                    mapScrollPane);
        }

        if (mapCanvas == null) {

            mapCanvas =
                    createMapCanvas();
        }

        mapScrollPane.setContent(
                mapCanvas);
    }

    private void createBottomBar() {

        Label instructions =
                new Label(
                        "Chọn loại ô rồi nhấp để vẽ. "
                                + "Chọn Empty để xóa ô. "
                                + "Chuột phải để đổi loại block. "
                                + "Chọn Set Spawn để đặt "
                                + "điểm xuất phát.");

        VBox bottomBar =
                new VBox(
                        4,
                        mapInfoLabel,
                        instructions,
                        statusLabel);

        bottomBar.setPadding(
                new Insets(8));

        setBottom(bottomBar);
    }

    private void updateStatus() {

        if (mapData == null) {
            return;
        }

        MapCellType selectedType =
                getSelectedCellType();

        MapSpawnData spawn =
                mapData.getSpawn();

        String spawnText =
                spawn == null
                        ? "not set"
                        : String.format(
                                "(%.1f, %.1f)",
                                spawn.getX(),
                                spawn.getY());

        mapInfoLabel.setText(
                "Map: "
                        + mapData.getWidth()
                        + " x "
                        + mapData.getHeight()
                        + " | Cell: "
                        + mapData.getCellSize()
                        + " | Grid: "
                        + mapData.getColumns()
                        + " x "
                        + mapData.getRows()
                        + " | Spawn: "
                        + spawnText);

        if (!spawnButton.isSelected()) {

            statusLabel.setText(
                    "Selected: "
                            + selectedType
                            + " | Cells: "
                            + mapData.getCells().size());
        }
    }

    private void updateSpawn(
            MapSpawnData spawn) {

        if (spawn == null) {
            return;
        }

        if (spawn.getX() < 0
                || spawn.getX()
                        >= mapData.getWidth()
                || spawn.getY() < 0
                || spawn.getY()
                        >= mapData.getHeight()) {

            return;
        }

        mapData.setSpawn(
                spawn);

        if (mapCanvas != null) {
            mapCanvas.refresh();
        }

        spawnButton.setSelected(false);

        updateStatus();
    }

    private void saveMap() {

        if (currentMapPath == null) {
            saveMapAs();
            return;
        }

        saveToPath(
                currentMapPath,
                currentMapId);
    }

    private void saveMapAs() {

        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Save Map As");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Map JSON",
                        "*.json"));

        chooser.setInitialFileName(
                currentMapId + ".json");

        Path path =
                getSelectedFile(
                        chooser,
                        true);

        if (path == null) {
            return;
        }

        String newId =
                MapDocument.generateId();

        saveToPath(
                path,
                newId);
    }

    private void saveToPath(
            Path path,
            String id) {

        String name =
                mapNameField.getText()
                        .trim();

        if (name.isBlank()) {

            showError(
                    "Không thể save map",
                    new IllegalArgumentException(
                            "Map Name không được để trống."));

            return;
        }

        try {

            MapDocument document =
                    new MapDocument(
                            id,
                            name,
                            mapData);

            mapFileService.save(
                    document,
                    path);

            currentMapId =
                    id;

            currentMapName =
                    name;

            currentMapPath =
                    path.toAbsolutePath()
                            .normalize();

            mapIdLabel.setText(
                    currentMapId);

            mapNameField.setText(
                    currentMapName);

            showInfo(
                    "Save thành công",
                    "Đã lưu bản đồ:\n"
                            + currentMapPath
                            + "\nID: "
                            + currentMapId);

        } catch (IOException
                | IllegalArgumentException exception) {

            showError(
                    "Không thể save map",
                    exception);
        }
    }

    private void loadMap() {

        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Load Map");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Map JSON",
                        "*.json"));

        Path path =
                getSelectedFile(
                        chooser,
                        false);

        if (path == null) {
            return;
        }

        try {

            MapDocument document =
                    mapFileService.load(
                            path);

            mapData =
                    document.mapData();

            currentMapId =
                    document.id();

            currentMapName =
                    document.name();

            currentMapPath =
                    path.toAbsolutePath()
                            .normalize();

            mapIdLabel.setText(
                    currentMapId);

            mapNameField.setText(
                    currentMapName);

            spawnButton.setSelected(false);

            squareButton.setSelected(true);

            mapCanvas =
                    createMapCanvas();

            createMapView();

            updateStatus();

            showInfo(
                    "Load thành công",
                    "Đã tải bản đồ:\n"
                            + currentMapPath
                            + "\nID: "
                            + currentMapId);

        } catch (IOException
                | IllegalArgumentException exception) {

            showError(
                    "Không thể load map",
                    exception);
        }
    }

    private Path getSelectedFile(
            FileChooser chooser,
            boolean save) {

        Window window =
                getScene() == null
                        ? null
                        : getScene().getWindow();

        Path initialDirectory =
                lastMapDirectory;

        if (!Files.isDirectory(
                initialDirectory)) {

            initialDirectory =
                    Path.of("")
                            .toAbsolutePath()
                            .normalize();
        }

        if (Files.isDirectory(
                initialDirectory)) {

            chooser.setInitialDirectory(
                    initialDirectory.toFile());
        }

        var file =
                save
                        ? chooser.showSaveDialog(
                                window)
                        : chooser.showOpenDialog(
                                window);

        if (file == null) {
            return null;
        }

        Path selectedPath =
                file.toPath()
                        .toAbsolutePath()
                        .normalize();

        Path parent =
                selectedPath.getParent();

        if (parent != null
                && Files.isDirectory(parent)) {

            lastMapDirectory =
                    parent;
        }

        return selectedPath;
    }

    private void showInfo(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showError(
            String title,
            Exception exception) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                exception.getMessage() == null
                        ? exception.toString()
                        : exception.getMessage());

        alert.showAndWait();
    }

    public MapData getMapData() {
        return mapData;
    }

    public MapCanvas getMapCanvas() {
        return mapCanvas;
    }
}