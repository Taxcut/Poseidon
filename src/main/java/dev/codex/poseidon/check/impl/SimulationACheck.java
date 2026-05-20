package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;
import dev.codex.poseidon.simulation.MovementEnvelope;
import dev.codex.poseidon.simulation.MovementSimulator;

public final class SimulationACheck extends Check {
    private final AlertManager alertManager;
    private final MovementSimulator simulator;
    private final long teleportExemptionMs;

    public SimulationACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("SimulationA",
                plugin,
                true,
                5.0D,
                CheckCategory.EXPERIMENTAL,
                CheckSeverity.HIGH,
                true,
                "Experimental movement prediction envelope for 1.7/1.8 physics.");
        this.alertManager = alertManager;
        double tolerance = toleranceMultiplier(plugin.getConfig().getString("settings.movement-tolerance", "BALANCED"));
        this.simulator = new MovementSimulator(
                plugin.getConfig().getInt("checks.SimulationA.max-ticks", 5),
                plugin.getConfig().getDouble("checks.SimulationA.horizontal-buffer", 0.18D) * tolerance,
                plugin.getConfig().getDouble("checks.SimulationA.vertical-buffer", 0.18D) * tolerance,
                plugin.getConfig().getLong("checks.SimulationA.velocity-age-ms", 1000L));
        this.teleportExemptionMs = plugin.getConfig().getLong("checks.SimulationA.teleport-exemption-ms", 2000L);
    }

    @Override
    public void handle(PacketContext context) {
        if (!context.hasPosition()) {
            return;
        }

        PlayerData data = context.getData();
        if (!data.hasPosition() || data.isFlying() || data.isAllowFlight() || data.isInsideVehicle()) {
            return;
        }

        if (context.getTimestamp() - data.getLastTeleportTimestamp() < teleportExemptionMs) {
            return;
        }

        double dx = context.getX() - data.getX();
        double dy = context.getY() - data.getY();
        double dz = context.getZ() - data.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        MovementEnvelope envelope = simulator.predict(data, context.getTimestamp());
        boolean failedHorizontal = horizontal > envelope.getMaxHorizontal();
        boolean failedUpward = dy > envelope.getMaxUpward();
        boolean failedDownward = -dy > envelope.getMaxDownward();

        if (failedHorizontal || failedUpward || failedDownward) {
            double over = Math.max(horizontal - envelope.getMaxHorizontal(),
                    Math.max(dy - envelope.getMaxUpward(), -dy - envelope.getMaxDownward()));
            double amount = Math.min(5.0D, Math.max(1.0D, over * 2.0D));
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "h=" + format(horizontal) + "/" + format(envelope.getMaxHorizontal())
                            + ",dy=" + format(dy)
                            + ",up=" + format(envelope.getMaxUpward())
                            + ",down=" + format(envelope.getMaxDownward()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }

    private static double toleranceMultiplier(String value) {
        if ("SEVERE".equalsIgnoreCase(value)) {
            return 0.35D;
        }
        if ("MODERATE".equalsIgnoreCase(value)) {
            return 0.65D;
        }
        if ("PERMISSIVE".equalsIgnoreCase(value)) {
            return 1.55D;
        }
        if ("LENIENT".equalsIgnoreCase(value)) {
            return 2.25D;
        }
        return 1.0D;
    }
}
