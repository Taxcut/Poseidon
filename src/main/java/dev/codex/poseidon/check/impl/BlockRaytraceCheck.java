package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class BlockRaytraceCheck extends Check {
    private final AlertManager alertManager;
    private final double maxAngle;

    public BlockRaytraceCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("BlockRaytrace", plugin, true, 5.0D, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true,
                "Experimental block placement ray alignment check.");
        this.alertManager = alertManager;
        this.maxAngle = plugin.getConfig().getDouble("checks.BlockRaytrace.max-angle", 75.0D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.BLOCK_PLACE) {
            return;
        }
        BlockPlaceData place = BlockPlaceData.read(context.getPacket());
        if (!place.isValid()) {
            return;
        }
        double eyeX = context.getData().getX();
        double eyeY = context.getData().getY() + 1.62D;
        double eyeZ = context.getData().getZ();
        double vx = place.getX() + 0.5D - eyeX;
        double vy = place.getY() + 0.5D - eyeY;
        double vz = place.getZ() + 0.5D - eyeZ;
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length <= 0.0D) {
            return;
        }
        double[] direction = direction(context.getData().getYaw(), context.getData().getPitch());
        double dot = (vx / length) * direction[0] + (vy / length) * direction[1] + (vz / length) * direction[2];
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, dot))));
        if (angle > maxAngle) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "angle=" + format(angle));
        }
    }

    private static double[] direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        return new double[] {
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        };
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
