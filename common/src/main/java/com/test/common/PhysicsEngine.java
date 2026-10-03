package com.test.common;

import com.test.common.map.MapCellData;
import com.test.common.map.MapCellType;
import com.test.common.map.MapData;

public class PhysicsEngine {

    private static final double SLOPE_NORMAL = 0.70710678118;
    private static final double FOOT_PADDING = 2.0;
    private static final double MAX_STEP_DOWN = 12.0; // Khoảng cách tự động dính chân xuống dốc

    public void tick(
            PlayerState player,
            MapData map,
            boolean movingLeft,
            boolean movingRight) {

        final double deltaTime = GameConfig.TICK_DT;

        /*
         * =========================
         * INPUT HORIZONTAL
         * =========================
         */
        if (player.isOnGround() && !player.isChargingJump()) {
            if (movingLeft && !movingRight) {
                player.setVelocityX(-GameConfig.MOVE_SPEED);
            } else if (movingRight && !movingLeft) {
                player.setVelocityX(GameConfig.MOVE_SPEED);
            } else {
                player.setVelocityX(0);
            }
        }

        double oldX = player.getX();
        double oldY = player.getY();
        double oldBottom = oldY + GameConfig.PLAYER_HEIGHT;

        /*
         * =========================
         * GRAVITY
         * =========================
         */
        if (!player.isChargingJump()) {
            player.setVelocityY(player.getVelocityY() + GameConfig.GRAVITY * deltaTime);
        }

        /*
         * =========================
         * POSITION X
         * =========================
         */
        double newX = player.getX() + player.getVelocityX() * deltaTime;
        player.setX(newX);

        resolveHorizontalCollision(player, map, oldX);

        /*
         * =========================
         * POSITION Y & SLOPE RESOLUTION
         * =========================
         */
        double newY = player.getY() + player.getVelocityY() * deltaTime;
        player.setY(newY);

        player.setOnGround(false);

        resolveVerticalCollision(player, map, oldY, oldBottom);

        /*
         * =========================
         * MAP BOUNDS
         * =========================
         */
        resolveMapBounds(player, map);
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

            MapCellType type = cell.getType();
            double x = cell.getGridX() * map.getCellSize();
            double y = cell.getGridY() * map.getCellSize();
            double size = map.getCellSize();

            switch (type) {
                case SQUARE -> resolveSquareHorizontalCollision(player, oldX, x, y, size);
                case TRIANGLE_LEFT -> resolveTriangleLeftHorizontalCollision(player, oldX, x, y, size);
                case TRIANGLE_RIGHT -> resolveTriangleRightHorizontalCollision(player, oldX, x, y, size);
                default -> {}
            }
        }
    }

    private void resolveSquareHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        // Chỉ chặn va chạm ngang nếu thân nhân vật lấn vào khối vuông (tránh va chạm nhầm chân dốc)
        boolean verticalOverlap = player.getY() + GameConfig.PLAYER_HEIGHT - 2.0 > y
                && player.getY() + 2.0 < y + size;

        if (!verticalOverlap) {
            return;
        }

        if (player.getVelocityX() > 0) {
            boolean crossed = oldX + GameConfig.PLAYER_WIDTH <= x
                    && player.getX() + GameConfig.PLAYER_WIDTH >= x;

            if (crossed) {
                player.setX(x - GameConfig.PLAYER_WIDTH);
                // Bật nảy rebound
                player.setVelocityX(-player.getVelocityX() * GameConfig.BOUND_RATIO);
            }
        } else if (player.getVelocityX() < 0) {
            boolean crossed = oldX >= x + size
                    && player.getX() <= x + size;

            if (crossed) {
                player.setX(x + size);
                // Bật nảy rebound
                player.setVelocityX(-player.getVelocityX() * GameConfig.BOUND_RATIO);
            }
        }
    }

    private void resolveTriangleLeftHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        double playerTop = player.getY();
        double playerBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        if (playerBottom <= y + 2.0 || playerTop >= y + size) {
            return;
        }

        double triangleRight = x + size;

        // Va chạm vách đứng bên phải của tam giác /| (khi đâm từ phải sang trái)
        if (player.getVelocityX() < 0) {
            boolean crossedRightWall = oldX >= triangleRight && player.getX() <= triangleRight;
            if (crossedRightWall) {
                player.setX(triangleRight);
                player.setVelocityX(-player.getVelocityX() * GameConfig.BOUND_RATIO);
            }
        }
    }

    private void resolveTriangleRightHorizontalCollision(
            PlayerState player,
            double oldX,
            double x,
            double y,
            double size) {

        double playerTop = player.getY();
        double playerBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        if (playerBottom <= y + 2.0 || playerTop >= y + size) {
            return;
        }

        double triangleLeft = x;

        // Va chạm vách đứng bên trái của tam giác |\ (khi đâm từ trái sang phải)
        if (player.getVelocityX() > 0) {
            boolean crossedLeftWall = oldX + GameConfig.PLAYER_WIDTH <= triangleLeft
                    && player.getX() + GameConfig.PLAYER_WIDTH >= triangleLeft;
            if (crossedLeftWall) {
                player.setX(triangleLeft - GameConfig.PLAYER_WIDTH);
                player.setVelocityX(-player.getVelocityX() * GameConfig.BOUND_RATIO);
            }
        }
    }

    // =====================================================
    // VERTICAL COLLISION & SLOPE HANDLING
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

            double x = cell.getGridX() * map.getCellSize();
            double y = cell.getGridY() * map.getCellSize();
            double size = map.getCellSize();
            MapCellType type = cell.getType();

            switch (type) {
                case SQUARE -> resolveSquareVerticalCollision(player, oldY, x, y, size);
                case TRIANGLE_LEFT -> resolveTriangleLeftCollision(player, x, y, size);
                case TRIANGLE_RIGHT -> resolveTriangleRightCollision(player, x, y, size);
                default -> {}
            }
        }
    }

    private void resolveSquareVerticalCollision(
            PlayerState player,
            double oldY,
            double x,
            double y,
            double size) {

        boolean horizontalOverlap = player.getX() + GameConfig.PLAYER_WIDTH > x
                && player.getX() < x + size;

        if (!horizontalOverlap) {
            return;
        }

        if (player.getVelocityY() >= 0) {
            boolean crossedTop = oldY + GameConfig.PLAYER_HEIGHT <= y + 4.0
                    && player.getY() + GameConfig.PLAYER_HEIGHT >= y;

            if (crossedTop) {
                player.setY(y - GameConfig.PLAYER_HEIGHT);
                player.setVelocityY(0);
                player.setOnGround(true);
            }
        } else if (player.getVelocityY() < 0) {
            boolean crossedBottom = oldY >= y + size
                    && player.getY() <= y + size;

            if (crossedBottom) {
                player.setY(y + size);
                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // TRIANGLE LEFT (Dốc hướng lên bên phải: /)
    // =====================================================

    private void resolveTriangleLeftCollision(
            PlayerState player,
            double x,
            double y,
            double size) {

        double playerLeft = player.getX();
        double playerRight = player.getX() + GameConfig.PLAYER_WIDTH;

        if (playerRight <= x || playerLeft >= x + size) {
            return;
        }

        double contactX = Math.max(x, Math.min(x + size, playerRight - FOOT_PADDING));
        double relativeX = contactX - x;
        double surfaceY = y + size - relativeX;

        double currentBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        if (currentBottom >= surfaceY - 4.0 && currentBottom <= surfaceY + MAX_STEP_DOWN) {
            player.setY(surfaceY - GameConfig.PLAYER_HEIGHT);
            applyLeftSlopeVelocity(player);
        } else if (player.getVelocityY() < 0) {
            double triangleBottom = y + size;
            if (player.getY() <= triangleBottom && player.getY() >= y) {
                player.setY(triangleBottom);
                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // TRIANGLE RIGHT (Dốc hướng xuống bên phải: \)
    // =====================================================

    private void resolveTriangleRightCollision(
            PlayerState player,
            double x,
            double y,
            double size) {

        double playerLeft = player.getX();
        double playerRight = player.getX() + GameConfig.PLAYER_WIDTH;

        if (playerRight <= x || playerLeft >= x + size) {
            return;
        }

        double contactX = Math.max(x, Math.min(x + size, playerLeft + FOOT_PADDING));
        double relativeX = contactX - x;
        double surfaceY = y + relativeX;

        double currentBottom = player.getY() + GameConfig.PLAYER_HEIGHT;

        if (currentBottom >= surfaceY - 4.0 && currentBottom <= surfaceY + MAX_STEP_DOWN) {
            player.setY(surfaceY - GameConfig.PLAYER_HEIGHT);
            applyRightSlopeVelocity(player);
        } else if (player.getVelocityY() < 0) {
            double triangleBottom = y + size;
            if (player.getY() <= triangleBottom && player.getY() >= y) {
                player.setY(triangleBottom);
                player.setVelocityY(0);
            }
        }
    }

    // =====================================================
    // SLOPE SLIDE HELPERS
    // =====================================================

    private void applyLeftSlopeVelocity(PlayerState player) {
        double currentSpeed = Math.hypot(player.getVelocityX(), player.getVelocityY());
        if (currentSpeed < 100.0) {
            currentSpeed = 100.0;
        }
        double slideSpeed = currentSpeed * SLOPE_NORMAL;

        player.setVelocityX(-slideSpeed);
        player.setVelocityY(slideSpeed);
        player.setOnGround(false);
    }

    private void applyRightSlopeVelocity(PlayerState player) {
        double currentSpeed = Math.hypot(player.getVelocityX(), player.getVelocityY());
        if (currentSpeed < 100.0) {
            currentSpeed = 100.0;
        }
        double slideSpeed = currentSpeed * SLOPE_NORMAL;

        player.setVelocityX(slideSpeed);
        player.setVelocityY(slideSpeed);
        player.setOnGround(false);
    }

    // =====================================================
    // MAP BOUNDS
    // =====================================================

    private void resolveMapBounds(PlayerState player, MapData map) {
        double leftWall = map.getCellSize();
        double rightWall = map.getWidth() - map.getCellSize();

        if (player.getX() < leftWall) {
            player.setX(leftWall);
            player.setVelocityX(0);
        }

        if (player.getX() + GameConfig.PLAYER_WIDTH > rightWall) {
            player.setX(rightWall - GameConfig.PLAYER_WIDTH);
            player.setVelocityX(0);
        }

        double floorY = map.getHeight() - map.getCellSize();

        if (player.getY() + GameConfig.PLAYER_HEIGHT > floorY) {
            player.setY(floorY - GameConfig.PLAYER_HEIGHT);
            player.setVelocityY(0);
            player.setOnGround(true);
        }
    }
}