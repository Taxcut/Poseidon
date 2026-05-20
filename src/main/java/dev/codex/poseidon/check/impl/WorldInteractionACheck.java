package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class WorldInteractionACheck extends Check {
    private final AlertManager alertManager;
    private final int maxPlacesPerSecond;
    private final double maxBridgeHorizontal;
    private final double minBridgePitch;

    public WorldInteractionACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("WorldInteractionA",
                plugin.getConfig().getBoolean("checks.WorldInteractionA.enabled", true),
                plugin.getConfig().getDouble("checks.WorldInteractionA.alert-vl", 4.0D));
        this.alertManager = alertManager;
        this.maxPlacesPerSecond = plugin.getConfig().getInt("checks.WorldInteractionA.max-places-per-second", 14);
        this.maxBridgeHorizontal = plugin.getConfig().getDouble("checks.WorldInteractionA.max-bridge-horizontal", 0.34D);
        this.minBridgePitch = plugin.getConfig().getDouble("checks.WorldInteractionA.min-bridge-pitch", 65.0D);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.BLOCK_PLACE) {
            return;
        }

        PlayerData data = context.getData();
        int places = data.recordBlockPlacePacket(context.getTimestamp());
        boolean fastPlacing = places > maxPlacesPerSecond;
        boolean bridgeMotion = data.getLastHorizontalDelta() > maxBridgeHorizontal && data.getPitch() < minBridgePitch;

        if (fastPlacing || bridgeMotion) {
            double amount = 1.0D;
            if (fastPlacing) {
                amount += Math.min(3.0D, (places - maxPlacesPerSecond) / 3.0D);
            }
            if (bridgeMotion) {
                amount += 1.25D;
            }
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "pps=" + places + ",move=" + format(data.getLastHorizontalDelta()) + ",pitch=" + format(data.getPitch()));
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
