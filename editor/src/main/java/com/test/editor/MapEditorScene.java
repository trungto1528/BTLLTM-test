package com.test.editor;

import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MapEditorScene extends BorderPane {

    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 6000;
    private static final int CELL_SIZE = 40;

    private final MapData mapData;
    private final MapCanvas mapCanvas;

    private final ToggleGroup cellTypeGroup;

    private final ToggleButton emptyButton;
    private final ToggleButton squareButton;
    private final ToggleButton triangleLeftButton;
    private final ToggleButton triangleRightButton;

    private final Label statusLabel;

    public MapEditorScene() {

        mapData =
                new MapData(
                        MAP_WIDTH,
                        MAP_HEIGHT,
                        CELL_SIZE);

        cellTypeGroup =
                new ToggleGroup();

        emptyButton =
                createCellTypeButton(
                        "Empty",
                        MapCellType.EMPTY);

        squareButton =
                createCellTypeButton(
                        "Square",
                        MapCellType.SQUARE);

        triangleLeftButton =
                createCellTypeButton(
                        "Triangle Left",
                        MapCellType.TRIANGLE_LEFT);

        triangleRightButton =
                createCellTypeButton(
                        "Triangle Right",
                        MapCellType.TRIANGLE_RIGHT);

        squareButton.setSelected(true);

        mapCanvas =
                new MapCanvas(
                        mapData,
                        this::getSelectedCellType);

        statusLabel =
                new Label();

        updateStatus();

        createTopBar();
        createMapView();
        createBottomBar();
    }

    private ToggleButton createCellTypeButton(
            String text,
            MapCellType type) {

        ToggleButton button =
                new ToggleButton(text);

        button.setToggleGroup(
                cellTypeGroup);

        button.setOnAction(event ->
                updateStatus());

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
                        emptyButton);

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
                        + type.name());
    }

    public MapData getMapData() {
        return mapData;
    }

    public MapCanvas getMapCanvas() {
        return mapCanvas;
    }
}