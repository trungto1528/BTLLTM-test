package com.test.editor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MapEditorApp extends Application {

    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;

    @Override
    public void start(Stage stage) {

        MapEditorScene editorScene =
                new MapEditorScene();

        Scene scene =
                new Scene(
                        editorScene,
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