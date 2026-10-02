package com.test.game;

import com.test.core.scene.GameScene;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class GameAppGame {

    private final GameApp app;

    private GameScene gameScene;

    private StackPane gameContainer;

    private Label departureToast;

    private PauseTransition departureToastTimer;

    public GameAppGame(
            GameApp app) {

        this.app = app;
    }

    // =====================================================
    // GET GAME SCENE
    // =====================================================

    public GameScene getGameScene() {

        return gameScene;
    }

    // =====================================================
    // OPEN GAME
    // =====================================================

    public void openGameScene() {

        if (gameScene != null) {
            return;
        }

        String mapId =
                app.getCurrentMapId();

        if (mapId == null
                || mapId.isBlank()) {

            System.err.println(
                    "Cannot open game: mapId is missing");

            return;
        }

        gameScene = new GameScene(
                app.getNetwork(),
                mapId,
                app.getPlayerDirectory());

        String localPlayerId =
                app.getLocalPlayerId();

        if (localPlayerId != null) {

            gameScene.setLocalPlayerId(
                    localPlayerId);
        }

        app.getNetwork().setGameScene(
                gameScene);

        gameContainer =
                new StackPane();

        gameContainer.setPrefSize(
                800,
                600);

        gameContainer.getChildren().add(
                gameScene);

        // =================================================
        // DEPARTURE TOAST
        // =================================================

        departureToast =
                new Label();

        departureToast.setVisible(
                false);

        departureToast.setManaged(
                false);

        departureToast.setMouseTransparent(
                true);

        departureToast.setWrapText(
                true);

        departureToast.setMaxWidth(
                420);

        departureToast.setAlignment(
                Pos.CENTER_LEFT);

        departureToast.setStyle(
                "-fx-background-color: rgba(25, 29, 36, 0.94);"
                        + "-fx-text-fill: white;"
                        + "-fx-padding: 10 14;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-color: rgba(255,255,255,0.18);"
                        + "-fx-border-radius: 8;"
                        + "-fx-font-size: 14px;");

        StackPane.setAlignment(
                departureToast,
                Pos.BOTTOM_LEFT);

        StackPane.setMargin(
                departureToast,
                new Insets(
                        0,
                        0,
                        20,
                        20));

        gameContainer.getChildren().add(
                departureToast);

        departureToastTimer =
                new PauseTransition(
                        Duration.seconds(4));

        departureToastTimer.setOnFinished(
                event ->
                        hideDepartureToast());

        app.getScene().setRoot(
                gameContainer);

        gameScene.requestFocus();
    }

    // =====================================================
    // SHOW DEPARTURE TOAST
    // =====================================================

    public void showDepartureToast(
            String message) {

        if (departureToast == null) {
            return;
        }

        Platform.runLater(
                () -> {

                    departureToast.setText(
                            message);

                    departureToast.setVisible(
                            true);

                    departureToast.toFront();

                    if (departureToastTimer != null) {

                        departureToastTimer
                                .playFromStart();
                    }
                });
    }

    // =====================================================
    // HIDE DEPARTURE TOAST
    // =====================================================

    public void hideDepartureToast() {

        if (departureToastTimer != null) {

            departureToastTimer.stop();
        }

        if (departureToast != null) {

            departureToast.setVisible(
                    false);
        }
    }

    // =====================================================
    // CHECK LOBBY
    // =====================================================

    public boolean isLobbyShowing() {

        if (app.getScene() == null) {
            return false;
        }

        return app.getScene().getRoot()
                == app.getLobbyView();
    }

    // =====================================================
    // DISPOSE
    // =====================================================

    public void dispose() {

        hideDepartureToast();

        if (app.getNetwork() != null) {

            app.getNetwork().setGameScene(
                    null);
        }

        gameScene = null;
        gameContainer = null;
        departureToast = null;
        departureToastTimer = null;
    }
}