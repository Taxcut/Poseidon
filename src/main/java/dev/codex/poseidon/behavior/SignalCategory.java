package dev.codex.poseidon.behavior;

import java.util.Locale;

public enum SignalCategory {
    MOVEMENT,
    COMBAT,
    INVENTORY,
    WORLD,
    PACKET,
    TIMING,
    LATENCY,
    BEHAVIOR,
    UNKNOWN;

    public static SignalCategory fromCheckName(String checkName) {
        if (checkName == null) {
            return UNKNOWN;
        }

        String lower = checkName.toLowerCase(Locale.US);
        if (lower.startsWith("move") || lower.startsWith("simulation") || lower.startsWith("fly") || lower.startsWith("speed")) {
            return MOVEMENT;
        }
        if (lower.startsWith("reach") || lower.startsWith("auto") || lower.startsWith("aim") || lower.startsWith("velocity")) {
            return COMBAT;
        }
        if (lower.startsWith("inventory")) {
            return INVENTORY;
        }
        if (lower.startsWith("world") || lower.startsWith("scaffold")) {
            return WORLD;
        }
        if (lower.startsWith("timer")) {
            return TIMING;
        }
        if (lower.startsWith("latency")) {
            return LATENCY;
        }
        if (lower.startsWith("badpacket")) {
            return PACKET;
        }
        if (lower.startsWith("model") || lower.startsWith("behavior")) {
            return BEHAVIOR;
        }
        return UNKNOWN;
    }
}
