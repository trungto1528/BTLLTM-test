package com.test.common;

import java.util.List;

public class PhysicsEngine {

        public void update(
                        PlayerState player,
                        double deltaTime,
                        List<PlatformData> platforms,
                        boolean movingLeft,
                        boolean movingRight) {

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
                 * - Player đứng yên
                 * - Không bị gravity kéo xuống
                 *
                 * Khi release jump:
                 *
                 * - chargingJump = false
                 * - velocityY đã được set âm
                 * - gravity bắt đầu hoạt động
                 */

                if (!player.isChargingJump()) {

                        player.setVelocityY(
                                        player.getVelocityY()
                                                        + GameConfig.GRAVITY * deltaTime);
                }

                /*
                 * =========================
                 * MOVE X
                 * =========================
                 */

                double newX = player.getX()
                                + player.getVelocityX()
                                                * deltaTime;

                player.setX(newX);

                /*
                 * =========================
                 * SIDE COLLISION
                 * =========================
                 */

                for (PlatformData platform : platforms) {

                        boolean verticalOverlap = player.getY()
                                        + GameConfig.PLAYER_HEIGHT > platform.getY()
                                        &&
                                        player.getY() < platform.getY()
                                                        + platform.getHeight();

                        if (!verticalOverlap) {
                                continue;
                        }

                        /*
                         * Moving right
                         */

                        if (player.getVelocityX() > 0) {

                                boolean crossed = oldX
                                                + GameConfig.PLAYER_WIDTH <= platform.getX()
                                                &&
                                                player.getX()
                                                                + GameConfig.PLAYER_WIDTH >= platform.getX();

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

                                boolean crossed = oldX >= platform.getX()
                                                + platform.getWidth()
                                                &&
                                                player.getX() <= platform.getX()
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

                double newY = player.getY()
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

                        boolean horizontalOverlap = player.getX()
                                        + GameConfig.PLAYER_WIDTH > platform.getX()
                                        &&
                                        player.getX() < platform.getX()
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

                                boolean crossedTop = oldY
                                                + GameConfig.PLAYER_HEIGHT <= platform.getY()
                                                &&
                                                player.getY()
                                                                + GameConfig.PLAYER_HEIGHT >= platform.getY();

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

                                boolean crossedBottom = oldY >= platform.getY()
                                                + platform.getHeight()
                                                &&
                                                player.getY() <= platform.getY()
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
                                + GameConfig.PLAYER_WIDTH > GameConfig.MAP_WIDTH
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

                double floorY = GameConfig.MAP_HEIGHT
                                - GameConfig.WALL_WIDTH;

                if (player.getY()
                                + GameConfig.PLAYER_HEIGHT > floorY) {

                        player.setY(
                                        floorY
                                                        - GameConfig.PLAYER_HEIGHT);

                        player.setVelocityY(0);

                        player.setOnGround(true);
                }
        }

        public static void updateRemotePlayer(
                        PlayerState player,
                        List<PlatformData> platforms,
                        double deltaTime) {

                double velocityX = player.getVelocityX();
                double velocityY = player.getVelocityY();

                /*
                 * Gravity
                 */
                if (!player.isOnGround()) {
                        velocityY += GameConfig.GRAVITY * deltaTime;
                }

                double oldX = player.getX();
                double oldY = player.getY();

                double newX = oldX + velocityX * deltaTime;
                double newY = oldY + velocityY * deltaTime;

                /*
                 * =========================================
                 * WALL
                 * =========================================
                 */

                if (newX < GameConfig.WALL_WIDTH) {

                        newX = GameConfig.WALL_WIDTH;

                        velocityX = Math.abs(velocityX);
                }

                if (newX + GameConfig.PLAYER_WIDTH > GameConfig.MAP_WIDTH - GameConfig.WALL_WIDTH) {

                        newX = GameConfig.MAP_WIDTH
                                        - GameConfig.WALL_WIDTH
                                        - GameConfig.PLAYER_WIDTH;

                        velocityX = -Math.abs(velocityX);
                }

                /*
                 * =========================================
                 * PLATFORM COLLISION
                 * =========================================
                 */

                boolean onGround = false;

                for (PlatformData platform : platforms) {

                        double playerLeft = newX;
                        double playerRight = newX + GameConfig.PLAYER_WIDTH;

                        double playerTop = newY;
                        double playerBottom = newY + GameConfig.PLAYER_HEIGHT;

                        double platformLeft = platform.getX();

                        double platformRight = platform.getX()
                                        + platform.getWidth();

                        double platformTop = platform.getY();

                        double platformBottom = platform.getY()
                                        + platform.getHeight();

                        boolean horizontalOverlap = playerRight > platformLeft
                                        && playerLeft < platformRight;

                        /*
                         * FALLING ONTO PLATFORM
                         */

                        if (velocityY >= 0
                                        && oldY + GameConfig.PLAYER_HEIGHT <= platformTop
                                        && playerBottom >= platformTop
                                        && horizontalOverlap) {

                                newY = platformTop
                                                - GameConfig.PLAYER_HEIGHT;

                                velocityY = 0;

                                onGround = true;
                        }

                        /*
                         * HITTING PLATFORM FROM BELOW
                         */

                        else if (velocityY < 0
                                        && oldY >= platformBottom
                                        && playerTop <= platformBottom
                                        && horizontalOverlap) {

                                newY = platformBottom;

                                velocityY = 0;
                        }
                }

                /*
                 * =========================================
                 * BOTTOM
                 * =========================================
                 */

                if (newY + GameConfig.PLAYER_HEIGHT >= GameConfig.MAP_HEIGHT) {

                        newY = GameConfig.MAP_HEIGHT
                                        - GameConfig.PLAYER_HEIGHT;

                        velocityY = 0;

                        onGround = true;
                }

                /*
                 * =========================================
                 * APPLY
                 * =========================================
                 */

                player.setX(newX);
                player.setY(newY);

                player.setVelocityX(velocityX);
                player.setVelocityY(velocityY);

                player.setOnGround(onGround);
        }
}