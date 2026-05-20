package dev.codex.poseidon.simulation;

public final class MovementEnvelope {
    private final double maxHorizontal;
    private final double maxUpward;
    private final double maxDownward;

    public MovementEnvelope(double maxHorizontal, double maxUpward, double maxDownward) {
        this.maxHorizontal = maxHorizontal;
        this.maxUpward = maxUpward;
        this.maxDownward = maxDownward;
    }

    public double getMaxHorizontal() {
        return maxHorizontal;
    }

    public double getMaxUpward() {
        return maxUpward;
    }

    public double getMaxDownward() {
        return maxDownward;
    }
}
