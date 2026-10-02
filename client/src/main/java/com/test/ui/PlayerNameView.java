package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class PlayerNameView extends VBox {

    private static final int MAX_NAME_LENGTH = 16;

    private final GameApp app;

    private final Label titleLabel;
    private final Label subtitleLabel;
    private final TextField nameField;
    private final Label errorLabel;
    private final Button continueButton;

    public PlayerNameView(GameApp app) {

        this.app = app;

        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(40));

        setPrefWidth(420);
        setPrefHeight(300);

        // =================================================
        // TITLE
        // =================================================

        titleLabel =
                new Label("JUMP GAME");

        titleLabel.setStyle(
                "-fx-font-size: 32px;"
                        + "-fx-font-weight: bold;");

        // =================================================
        // SUBTITLE
        // =================================================

        subtitleLabel =
                new Label("ENTER YOUR NAME");

        subtitleLabel.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: bold;");

        // =================================================
        // NAME FIELD
        // =================================================

        nameField =
                new TextField();

        nameField.setPromptText(
                "Your name");

        nameField.setMaxWidth(280);

        nameField.setPrefHeight(42);

        nameField.setStyle(
                "-fx-font-size: 16px;");

        // =================================================
        // ERROR
        // =================================================

        errorLabel =
                new Label();

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        errorLabel.setStyle(
                "-fx-text-fill: #d32f2f;"
                        + "-fx-font-size: 13px;");

        // =================================================
        // BUTTON
        // =================================================

        continueButton =
                new Button("CONTINUE");

        continueButton.setPrefWidth(180);
        continueButton.setPrefHeight(40);

        continueButton.setOnAction(
                event -> submitName());

        nameField.setOnAction(
                event -> submitName());

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

                            hideError();
                        });

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                titleLabel,
                subtitleLabel,
                nameField,
                errorLabel,
                continueButton);
    }

    // =====================================================
    // FIRST NAME SETUP
    // =====================================================

    public void setupForFirstName() {

        subtitleLabel.setText(
                "ENTER YOUR NAME");

        continueButton.setText(
                "CONTINUE");

        nameField.clear();

        hideError();

        continueButton.setDisable(
                false);
    }

    // =====================================================
    // CHANGE NAME SETUP
    // =====================================================

    public void setupForChangeName(
            String currentName) {

        subtitleLabel.setText(
                "CHANGE YOUR NAME");

        continueButton.setText(
                "SAVE");

        if (currentName == null
                || currentName.isBlank()) {

            nameField.clear();

        } else {

            nameField.setText(
                    currentName);
        }

        hideError();

        continueButton.setDisable(
                false);
    }

    // =====================================================
    // SUBMIT
    // =====================================================

    private void submitName() {

        String name =
                nameField.getText();

        if (name == null) {

            showError(
                    "Please enter your name.");

            return;
        }

        name =
                name.trim();

        if (name.isBlank()) {

            showError(
                    "Please enter your name.");

            return;
        }

        if (name.length()
                > MAX_NAME_LENGTH) {

            showError(
                    "Name must be 16 characters or less.");

            return;
        }

        if (name.contains("|")) {

            showError(
                    "The character '|' is not allowed.");

            return;
        }

        if (name.contains("\n")
                || name.contains("\r")) {

            showError(
                    "Invalid characters in name.");

            return;
        }

        continueButton.setDisable(
                true);

        app.submitPlayerName(
                name);
    }

    // =====================================================
    // SHOW ERROR
    // =====================================================

    public void showError(
            String message) {

        errorLabel.setText(
                message);

        errorLabel.setVisible(
                true);

        errorLabel.setManaged(
                true);

        continueButton.setDisable(
                false);
    }

    // =====================================================
    // HIDE ERROR
    // =====================================================

    public void hideError() {

        errorLabel.setText("");

        errorLabel.setVisible(
                false);

        errorLabel.setManaged(
                false);
    }

    // =====================================================
    // RESET
    // =====================================================

    public void reset() {

        setupForFirstName();

        nameField.requestFocus();
    }

    // =====================================================
    // GET PLAYER NAME
    // =====================================================

    public String getPlayerName() {

        String name =
                nameField.getText();

        if (name == null) {
            return "";
        }

        return name.trim();
    }
}