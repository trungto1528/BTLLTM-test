package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class MainMenuView extends VBox {

    private static final int MAX_NAME_LENGTH = 16;

    private final GameApp app;

    private final TextField nameField;

    private final Button saveNameButton;

    private final Label nameErrorLabel;

    public MainMenuView(GameApp app) {

        this.app =
                app;

        setSpacing(16);

        setAlignment(
                Pos.CENTER);

        setPadding(
                new Insets(30));

        setPrefSize(
                1000,
                700);

        setStyle(
                "-fx-background-color: #20242b;"
        );

        // =================================================
        // TITLE
        // =================================================

        Label title =
                new Label(
                        "JUMP GAME");

        title.setFont(
                Font.font(48));

        title.setTextFill(
                Color.WHITE);

        // =================================================
        // SUBTITLE
        // =================================================

        Label subtitle =
                new Label(
                        "MULTIPLAYER");

        subtitle.setFont(
                Font.font(20));

        subtitle.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // NAME TITLE
        // =================================================

        Label nameTitle =
                new Label(
                        "NAME");

        nameTitle.setFont(
                Font.font(16));

        nameTitle.setTextFill(
                Color.WHITE);

        // =================================================
        // NAME FIELD
        // =================================================

        nameField =
                new TextField();

        nameField.setPromptText(
                "Enter your name");

        nameField.setPrefWidth(
                220);

        nameField.setPrefHeight(
                45);

        nameField.setStyle(
                "-fx-font-size: 16px;"
        );

        // =================================================
        // SAVE NAME BUTTON
        // =================================================

        saveNameButton =
                createSmallButton(
                        "SAVE");

        saveNameButton.setOnAction(
                event -> submitPlayerName());

        nameField.setOnAction(
                event -> submitPlayerName());

        // =================================================
        // NAME VALIDATION
        // =================================================

        nameField.textProperty()
                .addListener(
                        (observable,
                                oldValue,
                                newValue) -> {

                            if (newValue == null) {
                                return;
                            }

                            if (newValue.length()
                                    > MAX_NAME_LENGTH) {

                                nameField.setText(
                                        newValue.substring(
                                                0,
                                                MAX_NAME_LENGTH));
                            }

                            hideNameError();
                        });

        // =================================================
        // NAME ERROR
        // =================================================

        nameErrorLabel =
                new Label();

        nameErrorLabel.setVisible(
                false);

        nameErrorLabel.setManaged(
                false);

        nameErrorLabel.setWrapText(
                true);

        nameErrorLabel.setMaxWidth(
                300);

        nameErrorLabel.setTextFill(
                Color.web("#ff6b6b"));

        nameErrorLabel.setFont(
                Font.font(13));

        // =================================================
        // NAME CONTAINER
        // =================================================

        HBox nameBox =
                new HBox(8);

        nameBox.setAlignment(
                Pos.CENTER);

        nameBox.getChildren().addAll(
                nameField,
                saveNameButton);

        // =================================================
        // BUTTONS
        // =================================================

        Button createButton =
                createButton(
                        "CREATE ROOM");

        Button joinButton =
                createButton(
                        "JOIN ROOM");

        Button findButton =
                createButton(
                        "FIND ROOM");

        Button exitButton =
                createButton(
                        "EXIT");

        // =================================================
        // ACTIONS
        // =================================================

        createButton.setOnAction(
                event -> app.showCreateRoom());

        joinButton.setOnAction(
                event -> app.showJoinRoom());

        findButton.setOnAction(
                event -> app.showFindRoom());

        exitButton.setOnAction(
                event -> app.getStage().close());

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                subtitle,
                nameTitle,
                nameBox,
                nameErrorLabel,
                createButton,
                joinButton,
                findButton,
                exitButton
        );
    }

    // =====================================================
    // PLAYER NAME
    // =====================================================

    public void setPlayerName(
            String name) {

        if (name == null
                || name.isBlank()) {

            nameField.clear();

            return;
        }

        nameField.setText(
                name);

        hideNameError();
    }

    // =====================================================
    // SUBMIT NAME
    // =====================================================

    private void submitPlayerName() {

        String name =
                nameField.getText();

        if (name == null) {

            showNameError(
                    "Please enter your name.");

            return;
        }

        name =
                name.trim();

        if (name.isBlank()) {

            showNameError(
                    "Please enter your name.");

            return;
        }

        if (name.length()
                > MAX_NAME_LENGTH) {

            showNameError(
                    "Name must be 16 characters or less.");

            return;
        }

        if (name.contains("|")) {

            showNameError(
                    "The character '|' is not allowed.");

            return;
        }

        if (name.contains("\n")
                || name.contains("\r")) {

            showNameError(
                    "Invalid characters in name.");

            return;
        }

        saveNameButton.setDisable(
                true);

        app.submitPlayerName(
                name);
    }

    // =====================================================
    // NAME ERROR
    // =====================================================

    public void showNameError(
            String message) {

        if (message == null
                || message.isBlank()) {

            message =
                    "Unable to set name.";
        }

        nameErrorLabel.setText(
                message);

        nameErrorLabel.setVisible(
                true);

        nameErrorLabel.setManaged(
                true);

        saveNameButton.setDisable(
                false);
    }

    // =====================================================
    // HIDE NAME ERROR
    // =====================================================

    public void hideNameError() {

        nameErrorLabel.setText(
                "");

        nameErrorLabel.setVisible(
                false);

        nameErrorLabel.setManaged(
                false);

        saveNameButton.setDisable(
                false);
    }

    // =====================================================
    // CHECK NAME
    // =====================================================

    public boolean hasPlayerName() {

        String name =
                nameField.getText();

        return name != null
                && !name.trim().isBlank();
    }

    // =====================================================
    // FOCUS NAME
    // =====================================================

    public void focusNameField() {

        nameField.requestFocus();

        nameField.positionCaret(
                nameField.getText().length());
    }

    // =====================================================
    // CREATE LARGE BUTTON
    // =====================================================

    private Button createButton(
            String text) {

        Button button =
                new Button(text);

        button.setPrefWidth(
                280);

        button.setPrefHeight(
                55);

        button.setFont(
                Font.font(18));

        return button;
    }

    // =====================================================
    // CREATE SMALL BUTTON
    // =====================================================

    private Button createSmallButton(
            String text) {

        Button button =
                new Button(text);

        button.setPrefWidth(
                75);

        button.setPrefHeight(
                45);

        button.setFont(
                Font.font(14));

        return button;
    }
}