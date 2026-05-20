package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class AimSnapCheck extends Check {
    private final AlertManager alertManager;
    private final double minYawDelta;
    private final double maxPitchDelta;

    public AimSnapCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AimSnap", plugin, true, 6.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental combat snap rotation analysis.");
        this.alertManager = alertManager;
        this.minYawDelta = plugin.getConfig().getDouble("checks.AimSnap.min-yaw-delta", 70.0D);
        this.maxPitchDelta = plugin.getConfig().getDouble("checks.AimSnap.max-pitch-delta", 1.0D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasRotation() || context.getTimestamp() - context.getData().getLastAttackTimestamp() > 750L) {
            return;
        }
        double yawDelta = Math.abs(wrap(context.getYaw() - context.getData().getYaw()));
        double pitchDelta = Math.abs(context.getPitch() - context.getData().getPitch());
        if (yawDelta > minYawDelta && pitchDelta < maxPitchDelta) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "yaw=" + format(yawDelta) + ",pitch=" + format(pitchDelta));
        }
    }

    private static double wrap(double value) {
        value %= 360.0D;
        if (value >= 180.0D) {
            value -= 360.0D;
        }
        if (value < -180.0D) {
            value += 360.0D;
        }
        return value;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
