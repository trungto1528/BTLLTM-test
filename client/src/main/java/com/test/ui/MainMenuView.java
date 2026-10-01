package com.test.ui;

import com.test.GameApp;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class MainMenuView extends VBox {

    public MainMenuView(GameApp app) {

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
                new Label("JUMP GAME");

        title.setFont(
                Font.font(48));

        title.setTextFill(
                Color.WHITE);

        // =================================================
        // SUBTITLE
        // =================================================

        Label subtitle =
                new Label("MULTIPLAYER");

        subtitle.setFont(
                Font.font(20));

        subtitle.setTextFill(
                Color.LIGHTGRAY);

        // =================================================
        // BUTTONS
        // =================================================

        Button createButton =
                createButton("CREATE ROOM");

        Button joinButton =
                createButton("JOIN ROOM");

        Button findButton =
                createButton("FIND ROOM");

        Button exitButton =
                createButton("EXIT");

        // =================================================
        // ACTIONS
        // =================================================

        createButton.setOnAction(
                e -> app.showCreateRoom());

        joinButton.setOnAction(
                e -> app.showJoinRoom());

        findButton.setOnAction(
                e -> app.showFindRoom());

        exitButton.setOnAction(
                e -> app.getStage().close());

        // =================================================
        // ADD
        // =================================================

        getChildren().addAll(
                title,
                subtitle,
                createButton,
                joinButton,
                findButton,
                exitButton
        );
    }

    private Button createButton(
            String text) {

        Button button =
                new Button(text);

        button.setPrefWidth(280);
        button.setPrefHeight(55);

        button.setFont(
                Font.font(18));

        return button;
    }
}