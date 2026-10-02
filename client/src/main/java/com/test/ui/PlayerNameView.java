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

        Label titleLabel =
                new Label("JUMP GAME");

        titleLabel.setStyle(
                "-fx-font-size: 32px;"
                        + "-fx-font-weight: bold;");

        Label subtitleLabel =
                new Label("ENTER YOUR NAME");

        subtitleLabel.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: bold;");

        nameField =
                new TextField();

        nameField.setPromptText(
                "Your name");

        nameField.setMaxWidth(280);

        nameField.setPrefHeight(42);

        nameField.setStyle(
                "-fx-font-size: 16px;");

        errorLabel =
                new Label();

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        errorLabel.setStyle(
                "-fx-text-fill: #d32f2f;"
                        + "-fx-font-size: 13px;");

        continueButton =
                new Button("CONTINUE");

        continueButton.setPrefWidth(180);
        continueButton.setPrefHeight(40);

        continueButton.setOnAction(
                event -> submitName());

        nameField.setOnAction(
                event -> submitName());

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

        getChildren().addAll(
                titleLabel,
                subtitleLabel,
                nameField,
                errorLabel,
                continueButton);
    }

    private void submitName() {

        String name =
                nameField.getText();

        if (name == null) {

            showError(
                    "Please enter your name.");

            return;
        }

        name = name.trim();

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

        continueButton.setDisable(true);

        app.submitPlayerName(name);
    }

    public void showError(
            String message) {

        errorLabel.setText(
                message);

        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        continueButton.setDisable(false);
    }

    public void hideError() {

        errorLabel.setText("");

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void reset() {

        nameField.clear();

        hideError();

        continueButton.setDisable(false);

        nameField.requestFocus();
    }

    public String getPlayerName() {

        return nameField.getText()
                .trim();
    }
}