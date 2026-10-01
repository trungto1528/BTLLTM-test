package com.test.common;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

public class PhysicsEngine {

    public void tick(
            PlayerState player,
            MapData map,
            boolean movingLeft,
            boolean movingRight) {

        final double deltaTime =
                GameConfig.TICK_DT;

        if (player.isOnGround()
                && !player.isChargingJump()) {

            if (movingLeft && !movingRight) {

                player.setVelocityX(
                        -GameConfig.MOVE_SPEED);

            } else if (movingRight && !movingLeft) {

                player.setVelocityX(
                        GameConfig.MOVE_SPEED);

            } else {

                player.setVelocityX(0);
            }
        }

        double oldX = player.getX();
        double oldY = player.getY();

        if (!player.isChargingJump()) {

            player.setVelocityY(
                    player.getVelocityY()
                            + GameConfig.GRAVITY
                            * deltaTime);
        }

        double newX =
                player.getX()
                        + player.getVelocityX()
                        * deltaTime;

        player.setX(newX);

        resolveHorizontalCollision(
                player,
                map,
                oldX);

        double newY =
                player.getY()
                        + player.getVelocityY()
                        * deltaTime;

        player.setY(newY);

        player.setOnGround(false);

        resolveVerticalCollision(
                player,
                map,
                oldY);

        resolveMapBounds(
                player,
                map);
    }

    private void resolveHorizontalCollision(
            PlayerState player,
            MapData map,
            double oldX) {

        for (MapCellData cell : map.getCells()) {

            if (cell.isEmpty()) {
                continue;
            }

            MapCellType type =
                    cell.getType();

            double x =
                    cell.getGridX()
                            * map.getCellSize();

            double y =
                    cell.getGridY()
                            * map.getCellSize();

            double size =
                    map.getCellSize();

            if (type == MapCellType.SQUARE) {

                resolveSquareHorizontalCollision(
                        player,
                        oldX,
                        x,
                        y,
                        size);

            } else if (type
                    == MapCellType.TRIANGLE_LEFT) {

                resolveTriangleLeftHorizontalCollision(
                        player,
                        oldX,
                        x,
                        y,
                        size);

            } else if (type
                    == MapCellType.TRIANGLE_RIGHT) {

                resolveTriangleRightHorizontalCollision(
                        player,
                        oldX,
                        x,
                        y,
                        size);
            }
        }
    }

    private void resolveSquareHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        boolean verticalOverlap =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT
                        > y
                &&
                player.getY()
                        < y + size;

        if (!verticalOverlap) {
            return;
        }

        if (player.getVelocityX() > 0) {

            boolean crossed =
                    oldX
                            + GameConfig.PLAYER_WIDTH
                            <= x
                    &&
                    player.getX()
                            + GameConfig.PLAYER_WIDTH
                            >= x;

            if (crossed) {

                player.setX(
                        x
                                - GameConfig.PLAYER_WIDTH);

                player.setVelocityX(
                        -player.getVelocityX()
                                * GameConfig.BOUND_RATIO);
            }

        } else if (player.getVelocityX() < 0) {

            boolean crossed =
                    oldX
                            >= x + size
                    &&
                    player.getX()
                            <= x + size;

            if (crossed) {

                player.setX(
                        x + size);

                player.setVelocityX(
                        -player.getVelocityX()
                                * GameConfig.BOUND_RATIO);
            }
        }
    }

    private void resolveTriangleLeftHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        if (player.getVelocityX() <= 0) {
            return;
        }

        double triangleRight =
                x + size;

        boolean verticalOverlap =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT
                        > y
                &&
                player.getY()
                        < y + size;

        if (!verticalOverlap) {
            return;
        }

        boolean crossed =
                oldX
                        + GameConfig.PLAYER_WIDTH
                        <= triangleRight
                &&
                player.getX()
                        + GameConfig.PLAYER_WIDTH
                        >= triangleRight;

        if (!crossed) {
            return;
        }

        player.setX(
                triangleRight
                        - GameConfig.PLAYER_WIDTH);

        player.setVelocityX(
                -player.getVelocityX()
                        * GameConfig.BOUND_RATIO);
    }

    private void resolveTriangleRightHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        if (player.getVelocityX() >= 0) {
            return;
        }

        boolean verticalOverlap =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT
                        > y
                &&
                player.getY()
                        < y + size;

        if (!verticalOverlap) {
            return;
        }

        boolean crossed =
                oldX
                        >= x
                &&
                player.getX()
                        <= x;

        if (!crossed) {
            return;
        }

        player.setX(
                x);

        player.setVelocityX(
                -player.getVelocityX()
                        * GameConfig.BOUND_RATIO);
    }

    private void resolveVerticalCollision(
            PlayerState player,
            MapData map,
            double oldY) {

        for (MapCellData cell : map.getCells()) {

            if (cell.isEmpty()) {
                continue;
            }

            double x =
                    cell.getGridX()
                            * map.getCellSize();

            double y =
                    cell.getGridY()
                            * map.getCellSize();

            double size =
                    map.getCellSize();

            MapCellType type =
                    cell.getType();

            if (type == MapCellType.SQUARE) {

                resolveSquareVerticalCollision(
                        player,
                        oldY,
                        x,
                        y,
                        size);

            } else if (type
                    == MapCellType.TRIANGLE_LEFT) {

                resolveTriangleLeftCollision(
                        player,
                        oldY,
                        x,
                        y,
                        size);

            } else if (type
                    == MapCellType.TRIANGLE_RIGHT) {

                resolveTriangleRightCollision(
                        player,
                        oldY,
                        x,
                        y,
                        size);
            }
        }
    }

    private void resolveSquareVerticalCollision(
            PlayerState player,
            double oldY,
            double x,
            double y,
            double size) {

        boolean horizontalOverlap =
                player.getX()
                        + GameConfig.PLAYER_WIDTH
                        > x
                &&
                player.getX()
                        < x + size;

        if (!horizontalOverlap) {
            return;
        }

        if (player.getVelocityY() > 0) {

            boolean crossedTop =
                    oldY
                            + GameConfig.PLAYER_HEIGHT
                            <= y
                    &&
                    player.getY()
                            + GameConfig.PLAYER_HEIGHT
                            >= y;

            if (crossedTop) {

                player.setY(
                        y
                                - GameConfig.PLAYER_HEIGHT);

                player.setVelocityY(0);

                player.setOnGround(true);
            }

        } else if (player.getVelocityY() < 0) {

            boolean crossedBottom =
                    oldY
                            >= y + size
                    &&
                    player.getY()
                            <= y + size;

            if (crossedBottom) {

                player.setY(
                        y + size);

                player.setVelocityY(0);
            }
        }
    }

    private void resolveTriangleLeftCollision(
            PlayerState player,
            double oldY,
            double x,
            double y,
            double size) {

        if (player.getVelocityY() <= 0) {
            return;
        }

        double playerCenterX =
                player.getX()
                        + GameConfig.PLAYER_WIDTH / 2.0;

        if (playerCenterX < x
                || playerCenterX > x + size) {
            return;
        }

        double relativeX =
                playerCenterX - x;

        double surfaceY =
                y + size - relativeX;

        double oldBottom =
                oldY
                        + GameConfig.PLAYER_HEIGHT;

        double newBottom =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT;

        if (oldBottom <= surfaceY
                && newBottom >= surfaceY) {

            player.setY(
                    surfaceY
                            - GameConfig.PLAYER_HEIGHT);

            applyLeftSlopeVelocity(
                    player);
        }
    }

    private void resolveTriangleRightCollision(
            PlayerState player,
            double oldY,
            double x,
            double y,
            double size) {

        if (player.getVelocityY() <= 0) {
            return;
        }

        double playerCenterX =
                player.getX()
                        + GameConfig.PLAYER_WIDTH / 2.0;

        if (playerCenterX < x
                || playerCenterX > x + size) {
            return;
        }

        double relativeX =
                playerCenterX - x;

        double surfaceY =
                y + relativeX;

        double oldBottom =
                oldY
                        + GameConfig.PLAYER_HEIGHT;

        double newBottom =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT;

        if (oldBottom <= surfaceY
                && newBottom >= surfaceY) {

            player.setY(
                    surfaceY
                            - GameConfig.PLAYER_HEIGHT);

            applyRightSlopeVelocity(
                    player);
        }
    }

    private void applyLeftSlopeVelocity(
            PlayerState player) {

        double speed =
                Math.abs(player.getVelocityY());

        if (speed < 1.0) {
            speed =
                    GameConfig.GRAVITY
                            * GameConfig.TICK_DT;
        }

        double slopeSpeed =
                speed
                        * 0.70710678118;

        player.setVelocityX(
                -slopeSpeed);

        player.setVelocityY(
                slopeSpeed);

        player.setOnGround(false);
    }

    private void applyRightSlopeVelocity(
            PlayerState player) {

        double speed =
                Math.abs(player.getVelocityY());

        if (speed < 1.0) {
            speed =
                    GameConfig.GRAVITY
                            * GameConfig.TICK_DT;
        }

        double slopeSpeed =
                speed
                        * 0.70710678118;

        player.setVelocityX(
                slopeSpeed);

        player.setVelocityY(
                slopeSpeed);

        player.setOnGround(false);
    }

    private void resolveMapBounds(
            PlayerState player,
            MapData map) {

        double leftWall =
                map.getCellSize();

        double rightWall =
                map.getWidth()
                        - map.getCellSize();

        if (player.getX() < leftWall) {

            player.setX(leftWall);

            player.setVelocityX(0);
        }

        if (player.getX()
                + GameConfig.PLAYER_WIDTH
                > rightWall) {

            player.setX(
                    rightWall
                            - GameConfig.PLAYER_WIDTH);

            player.setVelocityX(0);
        }

        double floorY =
                map.getHeight()
                        - map.getCellSize();

        if (player.getY()
                + GameConfig.PLAYER_HEIGHT
                > floorY) {

            player.setY(
                    floorY
                            - GameConfig.PLAYER_HEIGHT);

            player.setVelocityY(0);

            player.setOnGround(true);
        }
    }
}