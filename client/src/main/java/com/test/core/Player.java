package com.test.core;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player extends Rectangle {

    private boolean onGround = true;

    public Player(
            double x,
            double y) {

        super(
                30,
                30);

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