package com.test;

public class InputCommand {

    private final int sequence;
    private final String action;
    private final double jumpPower;

    public InputCommand(
            int sequence,
            String action) {

        this(
                sequence,
                action,
                0);
    }

    public InputCommand(
            int sequence,
            String action,
            double jumpPower) {

        this.sequence = sequence;
        this.action = action;
        this.jumpPower = jumpPower;
    }

    public int getSequence() {
        return sequence;
    }

    public String getAction() {
        return action;
    }

    public double getJumpPower() {
        return jumpPower;
    }
}