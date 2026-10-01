package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CreateRoomView extends VBox {

    private static final String DEFAULT_MAP_ID = "map01";

    private final Label statusLabel;
    private final Label mapLabel;
    private final Button createButton;
    private final Button backButton;

    public CreateRoomView(GameApp app) {

        setSpacing(20);
        setAlignment(Pos.CENTER);
        setPrefSize(1000, 700);

        setStyle(
                "-fx-background-color: #20242b;"
        );

        // =================================================
        // TITLE
        // =================================================

        Label title =
                new Label("CREATE ROOM");

        title.setFont(
                Font.font(36));

        title.setTextFill(
                Color.WHITE);

        // =================================================
        // DESCRIPTION
        // =================================================

        Label description =
                new Label(
                        "Create a new multiplayer room"
                );

        description.setFont(
                Font.font(18));

        description.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // MAP
        // =================================================

        mapLabel =
                new Label(
                        "Map: "
                                + DEFAULT_MAP_ID
                );

        mapLabel.setFont(
                Font.font(18));

        mapLabel.setTextFill(
                Color.WHITE);

        // =================================================
        // STATUS
        // =================================================

        statusLabel =
                new Label(
                        "Ready to create room."
                );

        statusLabel.setFont(
                Font.font(16));

        statusLabel.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // CREATE
        // =================================================

        createButton =
                new Button("CREATE");

        createButton.setPrefWidth(250);
        createButton.setPrefHeight(55);

        createButton.setFont(
                Font.font(18));

        createButton.setOnAction(
                e -> {

                    setCreating();

                    app.createRoom(
                            DEFAULT_MAP_ID);
                });

        // =================================================
        // BACK
        // =================================================

        backButton =
                new Button("BACK");

        backButton.setPrefWidth(250);
        backButton.setPrefHeight(45);

        backButton.setFont(
                Font.font(16));

        backButton.setOnAction(
                e -> app.showMainMenu());

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                description,
                mapLabel,
                statusLabel,
                createButton,
                backButton
        );
    }

    // =====================================================
    // CREATING
    // =====================================================

    private void setCreating() {

        statusLabel.setText(
                "Creating room..."
        );

        createButton.setDisable(
                true);

        backButton.setDisable(
                true);
    }

    // =====================================================
    // RESET
    // =====================================================

    public void reset() {

        statusLabel.setText(
                "Ready to create room."
        );

        mapLabel.setText(
                "Map: "
                        + DEFAULT_MAP_ID
        );

        createButton.setDisable(
                false);

        backButton.setDisable(
                false);
    }

    // =====================================================
    // ERROR
    // =====================================================

    public void setError(
            String message) {

        statusLabel.setText(
                message);

        createButton.setDisable(
                false);

        backButton.setDisable(
                false);
    }
}