package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class BadPacketsACheck extends Check {
    private final AlertManager alertManager;
    private final double maxPitch;

    public BadPacketsACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("BadPacketsA",
                plugin,
                true,
                1.0D,
                CheckCategory.PACKET,
                CheckSeverity.CRITICAL,
                false,
                "Rejects impossible rotation values such as invalid pitch.");
        this.alertManager = alertManager;
        this.maxPitch = plugin.getConfig().getDouble("checks.BadPacketsA.max-pitch", 90.0D);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasRotation()) {
            return;
        }

        float yaw = context.getYaw();
        float pitch = context.getPitch();
        if (Float.isNaN(yaw) || Float.isInfinite(yaw)
                || Float.isNaN(pitch) || Float.isInfinite(pitch)
                || Math.abs(pitch) > maxPitch) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "yaw=" + yaw + ",pitch=" + pitch);
        }
    }
}
