package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class MoveACheck extends Check {
    private final PoseidonPlugin plugin;
    private final AlertManager alertManager;
    private final double maxHorizontalDelta;
    private final double maxVerticalDelta;
    private final long teleportExemptionMs;

    public MoveACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("MoveA",
                plugin,
                true,
                2.0D,
                CheckCategory.MOVEMENT,
                CheckSeverity.MEDIUM,
                false,
                "Conservative impossible movement delta guard.");
        this.plugin = plugin;
        this.alertManager = alertManager;
        this.maxHorizontalDelta = plugin.getConfig().getDouble("checks.MoveA.max-horizontal-delta", 12.0D);
        this.maxVerticalDelta = plugin.getConfig().getDouble("checks.MoveA.max-vertical-delta", 12.0D);
        this.teleportExemptionMs = plugin.getConfig().getLong("checks.MoveA.teleport-exemption-ms", 2000L);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasPosition()) {
            return;
        }

        PlayerData data = context.getData();
        if (!data.hasPosition()) {
            return;
        }

        if (plugin.getExemptionManager().isMovementExempt(context.getPlayer(), data, context.getTimestamp())) {
            return;
        }

        if (context.getTimestamp() - data.getLastTeleportTimestamp() < teleportExemptionMs) {
            return;
        }

        double dx = context.getX() - data.getX();
        double dy = context.getY() - data.getY();
        double dz = context.getZ() - data.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal > maxHorizontalDelta || Math.abs(dy) > maxVerticalDelta) {
            double amount = Math.min(4.0D, Math.max(1.0D, horizontal / maxHorizontalDelta));
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "h=" + format(horizontal) + ",dy=" + format(dy));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
