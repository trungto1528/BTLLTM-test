package com.test.common;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

public class PhysicsEngine {

    private static final double SLOPE_NORMAL =
            0.70710678118;

    public void tick(
            PlayerState player,
            MapData map,
            boolean movingLeft,
            boolean movingRight) {

        final double deltaTime =
                GameConfig.TICK_DT;

        /*
         * =========================
         * INPUT HORIZONTAL
         * =========================
         *
         * Chỉ cho A/D điều khiển X khi đang
         * đứng trên mặt phẳng.
         *
         * Trên dốc, X/Y do slope physics
         * quyết định.
         */
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

        double oldX =
                player.getX();

        double oldY =
                player.getY();

        double oldBottom =
                oldY
                        + GameConfig.PLAYER_HEIGHT;

        /*
         * =========================
         * GRAVITY
         * =========================
         */
        if (!player.isChargingJump()) {

            player.setVelocityY(
                    player.getVelocityY()
                            + GameConfig.GRAVITY
                            * deltaTime);
        }

        /*
         * =========================
         * POSITION X
         * =========================
         */
        double newX =
                player.getX()
                        + player.getVelocityX()
                        * deltaTime;

        player.setX(newX);

        resolveHorizontalCollision(
                player,
                map,
                oldX);

        /*
         * =========================
         * POSITION Y
         * =========================
         */
        double newY =
                player.getY()
                        + player.getVelocityY()
                        * deltaTime;

        player.setY(newY);

        player.setOnGround(false);

        /*
         * =========================
         * VERTICAL COLLISION
         * =========================
         */
        resolveVerticalCollision(
                player,
                map,
                oldY,
                oldBottom);

        /*
         * =========================
         * MAP BOUNDS
         * =========================
         */
        resolveMapBounds(
                player,
                map);
    }

    // =====================================================
    // HORIZONTAL COLLISION
    // =====================================================

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

            switch (type) {

                case SQUARE -> {

                    resolveSquareHorizontalCollision(
                            player,
                            oldX,
                            x,
                            y,
                            size);
                }

                case TRIANGLE_LEFT -> {

                    resolveTriangleLeftHorizontalCollision(
                            player,
                            oldX,
                            x,
                            y,
                            size);
                }

                case TRIANGLE_RIGHT -> {

                    resolveTriangleRightHorizontalCollision(
                            player,
                            oldX,
                            x,
                            y,
                            size);
                }

                default -> {
                }
            }
        }
    }

    // =====================================================
    // SQUARE HORIZONTAL
    // =====================================================

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

    // =====================================================
    // TRIANGLE LEFT HORIZONTAL
    // =====================================================

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

        double playerTop =
                player.getY();

        double playerBottom =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT;

        if (playerBottom <= y
                || playerTop >= y + size) {

            return;
        }

        /*
         * Cạnh đứng bên phải của triangle.
         */
        if (player.getX()
                        + GameConfig.PLAYER_WIDTH
                        > triangleRight) {

            boolean crossed =
                    oldX
                            + GameConfig.PLAYER_WIDTH
                            <= triangleRight
                    &&
                    player.getX()
                            + GameConfig.PLAYER_WIDTH
                            >= triangleRight;

            if (crossed) {

                player.setX(
                        triangleRight
                                - GameConfig.PLAYER_WIDTH);

                player.setVelocityX(
                        -Math.abs(
                                player.getVelocityX())
                                * GameConfig.BOUND_RATIO);
            }

            return;
        }
    }

    // =====================================================
    // TRIANGLE RIGHT HORIZONTAL
    // =====================================================

    private void resolveTriangleRightHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        if (player.getVelocityX() >= 0) {
            return;
        }

        double triangleLeft =
                x;

        double playerTop =
                player.getY();

        double playerBottom =
                player.getY()
                        + GameConfig.PLAYER_HEIGHT;

        if (playerBottom <= y
                || playerTop >= y + size) {

            return;
        }

        /*
         * Cạnh đứng bên trái của triangle.
         */
        if (player.getX()
                        < triangleLeft) {

            boolean crossed =
                    oldX
                            >= triangleLeft
                    &&
                    player.getX()
                            <= triangleLeft;

            if (crossed) {

                player.setX(
                        triangleLeft);

                player.setVelocityX(
                        Math.abs(
                                player.getVelocityX())
                                * GameConfig.BOUND_RATIO);
            }

            return;
        }
    }

    // =====================================================
    // VERTICAL COLLISION
    // =====================================================

    private void resolveVerticalCollision(
            PlayerState player,
            MapData map,
            double oldY,
            double oldBottom) {

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

            switch (type) {

                case SQUARE -> {

                    resolveSquareVerticalCollision(
                            player,
                            oldY,
                            x,
                            y,
                            size);
                }

                case TRIANGLE_LEFT -> {

                    resolveTriangleLeftCollision(
                            player,
                            oldY,
                            oldBottom,
                            x,
                            y,
                            size);
                }

                case TRIANGLE_RIGHT -> {

                    resolveTriangleRightCollision(
                            player,
                            oldY,
                            oldBottom,
                            x,
                            y,
                            size);
                }

                default -> {
                }
            }
        }
    }

    // =====================================================
    // SQUARE VERTICAL
    // =====================================================

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

        /*
         * Rơi xuống.
         */
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

        /*
         * Bay lên.
         */
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

    // =====================================================
    // TRIANGLE LEFT
    // =====================================================

    private void resolveTriangleLeftCollision(
            PlayerState player,
            double oldY,
            double oldBottom,
            double x,
            double y,
            double size) {

        /*
         * =================================================
         * RƠI XUỐNG DỐC
         * =================================================
         */

        if (player.getVelocityY() > 0) {

            double centerX =
                    player.getX()
                            + GameConfig.PLAYER_WIDTH / 2.0;

            if (centerX < x
                    || centerX > x + size) {

                return;
            }

            double relativeX =
                    centerX - x;

            /*
             * TRIANGLE_LEFT:
             *
             * surface:
             *
             * y + size - relativeX
             *
             * /
             */
            double surfaceY =
                    y + size - relativeX;

            double newBottom =
                    player.getY()
                            + GameConfig.PLAYER_HEIGHT;

            /*
             * Chỉ snap vào dốc nếu thực sự đi
             * từ phía trên xuống.
             *
             * Đây là phần quan trọng chống
             * xuyên dốc khi tốc độ rơi lớn.
             */
            if (oldBottom <= surfaceY
                    && newBottom >= surfaceY) {

                player.setY(
                        surfaceY
                                - GameConfig.PLAYER_HEIGHT);

                applyLeftSlopeVelocity(
                        player);

                return;
            }

            /*
             * Nếu player đã nằm dưới mặt dốc
             * ở tick trước nhưng vẫn còn trong
             * vùng triangle, không được kéo ngược
             * player lên dốc.
             *
             * Player tiếp tục đi xuống và rời dốc.
             */
            return;
        }

        /*
         * =================================================
         * BAY TỪ DƯỚI LÊN
         * =================================================
         */

        if (player.getVelocityY() < 0) {

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

            double triangleBottom =
                    y + size;

            boolean crossedBottom =
                    oldY
                            >= triangleBottom
                    &&
                    player.getY()
                            <= triangleBottom;

            if (crossedBottom) {

                player.setY(
                        triangleBottom);

                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // TRIANGLE RIGHT
    // =====================================================

    private void resolveTriangleRightCollision(
            PlayerState player,
            double oldY,
            double oldBottom,
            double x,
            double y,
            double size) {

        /*
         * =================================================
         * RƠI XUỐNG DỐC
         * =================================================
         */

        if (player.getVelocityY() > 0) {

            double centerX =
                    player.getX()
                            + GameConfig.PLAYER_WIDTH / 2.0;

            if (centerX < x
                    || centerX > x + size) {

                return;
            }

            double relativeX =
                    centerX - x;

            /*
             * TRIANGLE_RIGHT:
             *
             * surface:
             *
             * y + relativeX
             *
             * \
             */
            double surfaceY =
                    y + relativeX;

            double newBottom =
                    player.getY()
                            + GameConfig.PLAYER_HEIGHT;

            /*
             * Chống xuyên dốc khi rơi nhanh.
             */
            if (oldBottom <= surfaceY
                    && newBottom >= surfaceY) {

                player.setY(
                        surfaceY
                                - GameConfig.PLAYER_HEIGHT);

                applyRightSlopeVelocity(
                        player);

                return;
            }

            /*
             * Không snap ngược player lên dốc
             * nếu nó đã đi xuyên qua mặt dốc
             * từ tick trước.
             */
            return;
        }

        /*
         * =================================================
         * BAY TỪ DƯỚI LÊN
         * =================================================
         */

        if (player.getVelocityY() < 0) {

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

            double triangleBottom =
                    y + size;

            boolean crossedBottom =
                    oldY
                            >= triangleBottom
                    &&
                    player.getY()
                            <= triangleBottom;

            if (crossedBottom) {

                player.setY(
                        triangleBottom);

                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // LEFT SLOPE MOVEMENT
    // =====================================================

    private void applyLeftSlopeVelocity(
            PlayerState player) {

        double speed =
                Math.abs(
                        player.getVelocityY());

        if (speed < 1.0) {

            speed =
                    GameConfig.GRAVITY
                            * GameConfig.TICK_DT;
        }

        double slopeSpeed =
                speed
                        * SLOPE_NORMAL;

        /*
         * TRIANGLE_LEFT:
         *
         * /
         *
         * Xuống dốc = trái + xuống.
         */
        player.setVelocityX(
                -slopeSpeed);

        player.setVelocityY(
                slopeSpeed);

        /*
         * Không coi dốc là ground.
         */
        player.setOnGround(false);
    }

    // =====================================================
    // RIGHT SLOPE MOVEMENT
    // =====================================================

    private void applyRightSlopeVelocity(
            PlayerState player) {

        double speed =
                Math.abs(
                        player.getVelocityY());

        if (speed < 1.0) {

            speed =
                    GameConfig.GRAVITY
                            * GameConfig.TICK_DT;
        }

        double slopeSpeed =
                speed
                        * SLOPE_NORMAL;

        /*
         * TRIANGLE_RIGHT:
         *
         * \
         *
         * Xuống dốc = phải + xuống.
         */
        player.setVelocityX(
                slopeSpeed);

        player.setVelocityY(
                slopeSpeed);

        /*
         * Không coi dốc là ground.
         */
        player.setOnGround(false);
    }

    // =====================================================
    // MAP BOUNDS
    // =====================================================

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