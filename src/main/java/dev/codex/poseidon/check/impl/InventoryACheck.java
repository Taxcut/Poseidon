package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class InventoryACheck extends Check {
    private final AlertManager alertManager;
    private final int maxClicksPerSecond;
    private final double maxMovingHorizontal;
    private final long recentAttackMs;

    public InventoryACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("InventoryA",
                plugin,
                false,
                4.0D,
                CheckCategory.INVENTORY,
                CheckSeverity.MEDIUM,
                false,
                "Legacy aggregate inventory behavior check.");
        this.alertManager = alertManager;
        this.maxClicksPerSecond = plugin.getConfig().getInt("checks.InventoryA.max-clicks-per-second", 18);
        this.maxMovingHorizontal = plugin.getConfig().getDouble("checks.InventoryA.max-moving-horizontal", 0.35D);
        this.recentAttackMs = plugin.getConfig().getLong("checks.InventoryA.recent-attack-ms", 250L);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.WINDOW_CLICK) {
            return;
        }

        PlayerData data = context.getData();
        int clicks = data.recordInventoryClickPacket(context.getTimestamp());
        boolean fastClicking = clicks > maxClicksPerSecond;
        boolean moving = data.getLastHorizontalDelta() > maxMovingHorizontal;
        boolean combatClick = context.getTimestamp() - data.getLastAttackTimestamp() < recentAttackMs;

        if (fastClicking || moving || combatClick) {
            double amount = 1.0D;
            if (fastClicking) {
                amount += Math.min(3.0D, (clicks - maxClicksPerSecond) / 4.0D);
            }
            if (moving) {
                amount += 0.75D;
            }
            if (combatClick) {
                amount += 0.75D;
            }
            alertManager.flag(context.getPlayer(), data, this, amount,
                    "cps=" + clicks + ",move=" + format(data.getLastHorizontalDelta()) + ",combat=" + combatClick);
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
