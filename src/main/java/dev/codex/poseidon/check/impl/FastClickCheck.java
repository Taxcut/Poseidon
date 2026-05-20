package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class FastClickCheck extends Check {
    private final AlertManager alertManager;
    private final int maxClicksPerSecond;

    public FastClickCheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("FastClick", plugin, true, 4.0D, CheckCategory.INVENTORY, CheckSeverity.MEDIUM, false,
                "Detects excessive inventory click rate.");
        this.alertManager = alertManager;
        this.maxClicksPerSecond = plugin.getConfig().getInt("checks.FastClick.max-clicks-per-second", 20);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.WINDOW_CLICK) {
            return;
        }
        int clicks = context.getData().recordInventoryClickPacket(context.getTimestamp());
        if (clicks > maxClicksPerSecond) {
            alertManager.flag(context.getPlayer(), context.getData(), this,
                    Math.min(3.0D, (clicks - maxClicksPerSecond) / 3.0D),
                    "cps=" + clicks);
        }
    }
}
