package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class BadPacketsBCheck extends Check {
    private final AlertManager alertManager;
    private final double maxCoordinate;

    public BadPacketsBCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("BadPacketsB",
                plugin,
                true,
                1.0D,
                CheckCategory.PACKET,
                CheckSeverity.CRITICAL,
                false,
                "Rejects invalid or out-of-world movement coordinates.");
        this.alertManager = alertManager;
        this.maxCoordinate = plugin.getConfig().getDouble("checks.BadPacketsB.max-coordinate", 32000000.0D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasPosition()) {
            return;
        }

        double x = context.getX();
        double y = context.getY();
        double z = context.getZ();

        if (!isValid(x) || !isValid(y) || !isValid(z)
                || Math.abs(x) > maxCoordinate
                || Math.abs(z) > maxCoordinate
                || Math.abs(y) > maxCoordinate) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "pos=" + trim(x) + "," + trim(y) + "," + trim(z));
        }
    }

    private static boolean isValid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static String trim(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
