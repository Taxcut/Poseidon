package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class ReachACheck extends Check {
    private final PoseidonPlugin plugin;
    private final AlertManager alertManager;
    private final double maxDistance;
    private final double maxLatencyExtra;
    private final boolean obstructionCheck;

    public ReachACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("ReachA",
                plugin,
                true,
                6.0D,
                CheckCategory.COMBAT,
                CheckSeverity.HIGH,
                false,
                "Latency-compensated combat reach validation.");
        this.plugin = plugin;
        this.alertManager = alertManager;
        this.maxDistance = plugin.getConfig().getDouble("checks.ReachA.max-distance", 4.40D);
        this.maxLatencyExtra = plugin.getConfig().getDouble("checks.ReachA.max-latency-extra", 0.60D);
        this.obstructionCheck = plugin.getConfig().getBoolean("checks.ReachA.obstruction-check", true);
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

        double latencyExtra = latencyExtra(data.getTransactionPing());
        double allowed = maxDistance + latencyExtra;
        double eyeX = data.getX();
        double eyeY = data.getY() + (data.isSneaking() ? 1.54D : 1.62D);
        double eyeZ = data.getZ();
        double[] direction = direction(data.getYaw(), data.getPitch());
        BoundingBox box = playerBox(targetSnapshot.getX(), targetSnapshot.getY(), targetSnapshot.getZ(), targetData.isSneaking(), latencyExtra);
        Double hitDistance = intersectRay(eyeX, eyeY, eyeZ, direction[0], direction[1], direction[2], box, allowed + 0.75D);

        if (hitDistance == null) {
            alertManager.flag(context.getPlayer(), data, this, 1.25D,
                    "ray-miss,allowed=" + format(allowed) + ",ping=" + data.getTransactionPing() + "ms");
            return;
        }

        if (hitDistance.doubleValue() > allowed) {
            double amount = Math.min(6.0D, Math.max(1.0D, (hitDistance.doubleValue() - allowed) * 2.0D));
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "ray=" + format(hitDistance.doubleValue()) + ",allowed=" + format(allowed) + ",ping=" + data.getTransactionPing() + "ms");
            return;
        }

        if (obstructionCheck && isObstructed(context, eyeX, eyeY, eyeZ, direction, hitDistance.doubleValue())) {
            alertManager.flag(context.getPlayer(), data, this, 0.75D,
                    "obstructed,ray=" + format(hitDistance.doubleValue()));
        }
    }

    private double latencyExtra(long transactionPing) {
        if (transactionPing <= 150L) {
            return 0.0D;
        }
        return Math.min(maxLatencyExtra, (transactionPing - 150L) * 0.002D);
    }

    private static BoundingBox playerBox(double x, double y, double z, boolean sneaking, double expand) {
        double halfWidth = 0.30D + Math.min(0.20D, expand * 0.20D);
        double height = sneaking ? 1.65D : 1.80D;
        return new BoundingBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
    }

    private static double[] direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new double[] {x, y, z};
    }

    private static Double intersectRay(double originX,
                                       double originY,
                                       double originZ,
                                       double dirX,
                                       double dirY,
                                       double dirZ,
                                       BoundingBox box,
                                       double maxDistance) {
        double tMin = 0.0D;
        double tMax = maxDistance;

        double[] origin = {originX, originY, originZ};
        double[] direction = {dirX, dirY, dirZ};
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < 1.0E-8D) {
                if (origin[axis] < min[axis] || origin[axis] > max[axis]) {
                    return null;
                }
            } else {
                double inverse = 1.0D / direction[axis];
                double t1 = (min[axis] - origin[axis]) * inverse;
                double t2 = (max[axis] - origin[axis]) * inverse;
                if (t1 > t2) {
                    double swap = t1;
                    t1 = t2;
                    t2 = swap;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return null;
                }
            }
        }
        return Double.valueOf(tMin);
    }

    private static boolean isObstructed(PacketContext context,
                                        double originX,
                                        double originY,
                                        double originZ,
                                        double[] direction,
                                        double hitDistance) {
        double step = 0.25D;
        for (double distance = step; distance < hitDistance - 0.20D; distance += step) {
            int x = floor(originX + direction[0] * distance);
            int y = floor(originY + direction[1] * distance);
            int z = floor(originZ + direction[2] * distance);
            if (context.getPlayer().getWorld().getBlockAt(x, y, z).getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static final class BoundingBox {
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;

        private BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }
}
