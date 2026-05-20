package dev.codex.poseidon.check;

import dev.codex.poseidon.packet.PacketContext;

public abstract class Check {
    private final String name;
    private final boolean enabled;
    private final double alertVl;

    protected Check(String name, boolean enabled, double alertVl) {
        this.name = name;
        this.enabled = enabled;
        this.alertVl = alertVl;
    }

    public abstract void handle(PacketContext context);

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getAlertVl() {
        return alertVl;
    }
}
