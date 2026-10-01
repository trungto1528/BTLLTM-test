package com.test.editor;

import java.io.IOException;
import java.nio.file.Path;

import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    private static final int CELL_SIZE = 40;

    private final MapFileService mapFileService;

    private MapData mapData;
    private MapCanvas mapCanvas;

    private final ToggleGroup cellTypeGroup;

    private final ToggleButton emptyButton;
    private final ToggleButton squareButton;
    private final ToggleButton triangleLeftButton;
    private final ToggleButton triangleRightButton;

    private final Label statusLabel;

    private final Button saveButton;
    private final Button loadButton;

    public MapEditorScene() {

        mapFileService =
                new MapFileService();

        mapData =
                new MapData(
                        MAP_WIDTH,
                        MAP_HEIGHT,
                        CELL_SIZE);

        cellTypeGroup =
                new ToggleGroup();

        emptyButton =
                createCellTypeButton(
                        "Empty");

        squareButton =
                createCellTypeButton(
                        "Square");

        triangleLeftButton =
                createCellTypeButton(
                        "Triangle Left");

        triangleRightButton =
                createCellTypeButton(
                        "Triangle Right");

        squareButton.setSelected(true);

        mapCanvas =
                createMapCanvas();

        statusLabel =
                new Label();

        saveButton =
                new Button("Save");

        loadButton =
                new Button("Load");

        saveButton.setOnAction(
                event -> saveMap());

        loadButton.setOnAction(
                event -> loadMap());

        updateStatus();

        createTopBar();
        createMapView();
        createBottomBar();
    }

    private MapCanvas createMapCanvas() {

        return new MapCanvas(
                mapData,
                this::getSelectedCellType);
    }

    private ToggleButton createCellTypeButton(
            String text) {

        ToggleButton button =
                new ToggleButton(text);

        button.setToggleGroup(
                cellTypeGroup);

        button.setOnAction(
                event -> updateStatus());

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

    private void createTopBar() {

        Label title =
                new Label(
                        "LTM Map Editor");

        title.setStyle(
                "-fx-font-size: 18px; "
                        + "-fx-font-weight: bold;");

        Label mapInfo =
                new Label(
                        "Map: "
                                + mapData.getWidth()
                                + " x "
                                + mapData.getHeight()
                                + " | Cell: "
                                + mapData.getCellSize()
                                + " | Grid: "
                                + mapData.getColumns()
                                + " x "
                                + mapData.getRows());

        HBox toolbar =
                new HBox(
                        10,
                        title,
                        mapInfo,
                        squareButton,
                        triangleLeftButton,
                        triangleRightButton,
                        emptyButton,
                        saveButton,
                        loadButton);

        toolbar.setPadding(
                new Insets(10));

        toolbar.setSpacing(10);

        setTop(toolbar);
    }

    private void createMapView() {

        ScrollPane scrollPane =
                new ScrollPane();

        scrollPane.setContent(
                mapCanvas);

        scrollPane.setPannable(true);

        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS);

        setCenter(scrollPane);
    }

    private void createBottomBar() {

        Label instructionLabel =
                new Label(
                        "Click vào ô để đặt cell. "
                                + "Chọn Empty để xóa cell.");

        VBox bottomBar =
                new VBox(
                        4,
                        instructionLabel,
                        statusLabel);

        bottomBar.setPadding(
                new Insets(8));

        setBottom(bottomBar);
    }

    private void updateStatus() {

        MapCellType type =
                getSelectedCellType();

        statusLabel.setText(
                "Selected: "
                        + type.name()
                        + " | Cells: "
                        + mapData.getCells().size());
    }

    private void saveMap() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Save Map");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Map JSON",
                        "*.json"));

        fileChooser.setInitialFileName(
                "map.json");

        Path path =
                getSelectedFile(
                        fileChooser,
                        true);

        if (path == null) {
            return;
        }

        try {

            mapFileService.save(
                    mapData,
                    path);

            showInfo(
                    "Save thành công",
                    "Đã lưu map:\n"
                            + path);

        } catch (IOException exception) {

            showError(
                    "Không thể save map",
                    exception);
        }
    }

    private void loadMap() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Load Map");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Map JSON",
                        "*.json"));

        Path path =
                getSelectedFile(
                        fileChooser,
                        false);

        if (path == null) {
            return;
        }

        try {

            MapData loadedMap =
                    mapFileService.load(
                            path);

            mapData =
                    loadedMap;

            mapCanvas =
                    createMapCanvas();

            createMapView();

            updateStatus();

            showInfo(
                    "Load thành công",
                    "Đã load map:\n"
                            + path);

        } catch (IOException
                | IllegalArgumentException exception) {

            showError(
                    "Không thể load map",
                    exception);
        }
    }

    private Path getSelectedFile(
            FileChooser fileChooser,
            boolean save) {

        Window window =
                getScene() == null
                        ? null
                        : getScene().getWindow();

        if (save) {

            var file =
                    fileChooser.showSaveDialog(
                            window);

            if (file == null) {
                return null;
            }

            return file.toPath();
        }

        var file =
                fileChooser.showOpenDialog(
                        window);

        if (file == null) {
            return null;
        }

        return file.toPath();
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
                exception.getMessage());

        alert.showAndWait();
    }

    public MapData getMapData() {
        return mapData;
    }

    public MapCanvas getMapCanvas() {
        return mapCanvas;
    }
}