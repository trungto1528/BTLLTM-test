package com.test.editor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MapEditorApp extends Application {

    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;

    @Override
    public void start(Stage stage) {

        Label label =
                new Label("LTM Map Editor");

        StackPane root =
                new StackPane(label);

        Scene scene =
                new Scene(
                        root,
                        WINDOW_WIDTH,
                        WINDOW_HEIGHT);

        stage.setTitle("LTM Map Editor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}