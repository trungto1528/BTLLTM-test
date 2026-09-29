package com.test;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Platform extends Rectangle {

    public Platform(
            double x,
            double y,
            double width,
            double height) {

        super(
                width,
                height
        );

        setX(x);
        setY(y);

        setFill(
                Color.DARKGREEN
        );
    }
}