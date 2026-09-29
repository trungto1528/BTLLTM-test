package com.test.common;

import java.util.List;

public class PhysicsEngine {

    /**
     * Update player đúng 1 game tick.
     *
     * Toàn bộ game simulation sử dụng:
     *
     *     GameConfig.TICK_RATE = 60
     *
     * nên mỗi lần gọi method này tương ứng đúng 1/60 giây.
     *
     * Không nhận deltaTime từ bên ngoài để tránh việc
     * physics phụ thuộc vào FPS hoặc thời gian frame.
     */
    public void tick(
            PlayerState player,
            List<PlatformData> platforms,
            boolean movingLeft,
            boolean movingRight) {

        final double deltaTime =
                GameConfig.TICK_DT;

        /*
         * =========================
         * HORIZONTAL MOVEMENT
         * =========================
         *
         * Chỉ cho điều khiển A/D trực tiếp
         * khi player đang đứng trên mặt đất
         * và không charge jump.
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

        double oldX = player.getX();
        double oldY = player.getY();

        /*
         * =========================
         * GRAVITY
         * =========================
         *
         * Khi đang charge jump:
         *
         * - Player đứng yên theo trục Y
         * - Không bị gravity kéo xuống
         *
         * Khi release:
         *
         * - chargingJump = false
         * - velocityY đã được set âm
         * - gravity bắt đầu hoạt động
         */

        if (!player.isChargingJump()) {

            player.setVelocityY(
                    player.getVelocityY()
                            + GameConfig.GRAVITY
                            * deltaTime);
        }

        /*
         * =========================
         * MOVE X
         * =========================
         */

        double newX =
                player.getX()
                        + player.getVelocityX()
                        * deltaTime;

        player.setX(newX);

        /*
         * =========================
         * SIDE COLLISION
         * =========================
         */

        for (PlatformData platform : platforms) {

            boolean verticalOverlap =
                    player.getY()
                            + GameConfig.PLAYER_HEIGHT
                            > platform.getY()
                    &&
                    player.getY()
                            < platform.getY()
                                    + platform.getHeight();

            if (!verticalOverlap) {
                continue;
            }

            /*
             * Moving right
             */

            if (player.getVelocityX() > 0) {

                boolean crossed =
                        oldX
                                + GameConfig.PLAYER_WIDTH
                                <= platform.getX()
                        &&
                        player.getX()
                                + GameConfig.PLAYER_WIDTH
                                >= platform.getX();

                if (crossed) {

                    player.setX(
                            platform.getX()
                                    - GameConfig.PLAYER_WIDTH);

                    player.setVelocityX(
                            -player.getVelocityX()
                                    * GameConfig.BOUND_RATIO);
                }
            }

            /*
             * Moving left
             */

            else if (player.getVelocityX() < 0) {

                boolean crossed =
                        oldX
                                >= platform.getX()
                                        + platform.getWidth()
                        &&
                        player.getX()
                                <= platform.getX()
                                        + platform.getWidth();

                if (crossed) {

                    player.setX(
                            platform.getX()
                                    + platform.getWidth());

                    player.setVelocityX(
                            -player.getVelocityX()
                                    * GameConfig.BOUND_RATIO);
                }
            }
        }

        /*
         * =========================
         * MOVE Y
         * =========================
         */

        double newY =
                player.getY()
                        + player.getVelocityY()
                        * deltaTime;

        player.setY(newY);

        /*
         * Mặc định sau khi update Y,
         * player không còn đứng trên ground.
         *
         * Collision bên dưới sẽ set lại true
         * nếu player thực sự đáp xuống.
         */

        player.setOnGround(false);

        /*
         * =========================
         * TOP / BOTTOM COLLISION
         * =========================
         */

        for (PlatformData platform : platforms) {

            boolean horizontalOverlap =
                    player.getX()
                            + GameConfig.PLAYER_WIDTH
                            > platform.getX()
                    &&
                    player.getX()
                            < platform.getX()
                                    + platform.getWidth();

            if (!horizontalOverlap) {
                continue;
            }

            /*
             * =========================
             * FALLING
             * =========================
             */

            if (player.getVelocityY() > 0) {

                boolean crossedTop =
                        oldY
                                + GameConfig.PLAYER_HEIGHT
                                <= platform.getY()
                        &&
                        player.getY()
                                + GameConfig.PLAYER_HEIGHT
                                >= platform.getY();

                if (crossedTop) {

                    player.setY(
                            platform.getY()
                                    - GameConfig.PLAYER_HEIGHT);

                    player.setVelocityY(0);

                    player.setOnGround(true);
                }
            }

            /*
             * =========================
             * GOING UP
             * =========================
             */

            else if (player.getVelocityY() < 0) {

                boolean crossedBottom =
                        oldY
                                >= platform.getY()
                                        + platform.getHeight()
                        &&
                        player.getY()
                                <= platform.getY()
                                        + platform.getHeight();

                if (crossedBottom) {

                    player.setY(
                            platform.getY()
                                    + platform.getHeight());

                    player.setVelocityY(0);
                }
            }
        }

        /*
         * =========================
         * MAP LEFT WALL
         * =========================
         */

        if (player.getX() < GameConfig.WALL_WIDTH) {

            player.setX(
                    GameConfig.WALL_WIDTH);

            player.setVelocityX(0);
        }

        /*
         * =========================
         * MAP RIGHT WALL
         * =========================
         */

        if (player.getX()
                + GameConfig.PLAYER_WIDTH
                > GameConfig.MAP_WIDTH
                        - GameConfig.WALL_WIDTH) {

            player.setX(
                    GameConfig.MAP_WIDTH
                            - GameConfig.WALL_WIDTH
                            - GameConfig.PLAYER_WIDTH);

            player.setVelocityX(0);
        }

        /*
         * =========================
         * FLOOR
         * =========================
         */

        double floorY =
                GameConfig.MAP_HEIGHT
                        - GameConfig.WALL_WIDTH;

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