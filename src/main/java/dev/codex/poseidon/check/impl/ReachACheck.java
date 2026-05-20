package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class ReachACheck extends Check {
    private final PoseidonPlugin plugin;
    private final AlertManager alertManager;
    private final double maxDistance;
    private final double maxLatencyExtra;

    public ReachACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("ReachA",
                plugin.getConfig().getBoolean("checks.ReachA.enabled", true),
                plugin.getConfig().getDouble("checks.ReachA.alert-vl", 6.0D));
        this.plugin = plugin;
        this.alertManager = alertManager;
        this.maxDistance = plugin.getConfig().getDouble("checks.ReachA.max-distance", 4.40D);
        this.maxLatencyExtra = plugin.getConfig().getDouble("checks.ReachA.max-latency-extra", 0.60D);
    }

    @Override
    public void handle(final PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY
                || !context.isAttack()
                || context.getEntityId() < 0) {
            return;
        }

        PlayerData targetData = plugin.getDataManager().getByEntityId(context.getEntityId());
        if (targetData == null || targetData.getUuid().equals(context.getData().getUuid())) {
            return;
        }

        PlayerData data = context.getData();
        if (!data.getLastWorldName().equals(targetData.getLastWorldName())) {
            return;
        }

        long historyTimestamp = context.getTimestamp() - Math.min(500L, Math.max(0L, data.getTransactionPing() / 2L));
        PlayerData.PositionSnapshot targetSnapshot = targetData.getPositionAtOrBefore(historyTimestamp);
        if (targetSnapshot == null || !data.hasPosition()) {
            return;
        }

        double eyeX = data.getX();
        double eyeY = data.getY() + 1.62D;
        double eyeZ = data.getZ();
        double targetX = targetSnapshot.getX();
        double targetY = targetSnapshot.getY();
        double targetZ = targetSnapshot.getZ();

        double distance = distanceToPlayerBox(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
        double latencyExtra = latencyExtra(data.getTransactionPing());
        double allowed = maxDistance + latencyExtra;

        if (distance > allowed) {
            double amount = Math.min(6.0D, Math.max(1.0D, (distance - allowed) * 2.0D));
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "dist=" + format(distance) + ",allowed=" + format(allowed) + ",ping=" + data.getTransactionPing() + "ms");
        }
    }

    private double latencyExtra(long transactionPing) {
        if (transactionPing <= 150L) {
            return 0.0D;
        }
        return Math.min(maxLatencyExtra, (transactionPing - 150L) * 0.002D);
    }

    private static double distanceToPlayerBox(double eyeX, double eyeY, double eyeZ, double targetX, double targetY, double targetZ) {
        double halfWidth = 0.30D;
        double minX = targetX - halfWidth;
        double maxX = targetX + halfWidth;
        double minY = targetY;
        double maxY = targetY + 1.80D;
        double minZ = targetZ - halfWidth;
        double maxZ = targetZ + halfWidth;

        double dx = axisDistance(eyeX, minX, maxX);
        double dy = axisDistance(eyeY, minY, maxY);
        double dz = axisDistance(eyeZ, minZ, maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axisDistance(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
