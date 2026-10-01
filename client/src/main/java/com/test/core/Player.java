package com.test.core;

import com.test.common.GameConfig;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player extends Rectangle {

    private boolean onGround = true;

    public Player(
            double x,
            double y) {

        super(
                GameConfig.PLAYER_WIDTH,
                GameConfig.PLAYER_HEIGHT);

        setFill(
                Color.BLUE);

        setX(x);
        setY(y);
    }

    public void setOnGround(
            boolean value) {

        onGround = value;
    }

    public boolean isOnGround() {

        return onGround;
    }
}