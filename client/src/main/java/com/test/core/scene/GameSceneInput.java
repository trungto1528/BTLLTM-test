package com.test.core.scene;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;

public class GameSceneInput {

    private final GameScene scene;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean spacePressed;

    public GameSceneInput(
            GameScene scene) {

        this.scene = scene;
    }

    public void setup() {

        scene.setFocusTraversable(true);

        scene.setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.A
                    || event.getCode()
                    == KeyCode.LEFT) {

                if (!leftPressed) {

                    leftPressed = true;

                    scene.getSimulation()
                            .sendInput(
                                    "LEFT_PRESS");
                }
            }

            if (event.getCode() == KeyCode.D
                    || event.getCode()
                    == KeyCode.RIGHT) {

                if (!rightPressed) {

                    rightPressed = true;

                    scene.getSimulation()
                            .sendInput(
                                    "RIGHT_PRESS");
                }
            }

            if (event.getCode()
                    == KeyCode.SPACE) {

                if (!spacePressed) {

                    spacePressed = true;

                    scene.getSimulation()
                            .sendInput(
                                    "JUMP_START");
                }
            }
        });

        scene.setOnKeyReleased(event -> {

            if (event.getCode() == KeyCode.A
                    || event.getCode()
                    == KeyCode.LEFT) {

                if (leftPressed) {

                    leftPressed = false;

                    scene.getSimulation()
                            .sendInput(
                                    "LEFT_RELEASE");
                }
            }

            if (event.getCode() == KeyCode.D
                    || event.getCode()
                    == KeyCode.RIGHT) {

                if (rightPressed) {

                    rightPressed = false;

                    scene.getSimulation()
                            .sendInput(
                                    "RIGHT_RELEASE");
                }
            }

            if (event.getCode()
                    == KeyCode.SPACE) {

                if (spacePressed) {

                    spacePressed = false;

                    scene.getSimulation()
                            .sendJumpRelease();
                }
            }
        });

        Platform.runLater(
                scene::requestFocus);
    }
}