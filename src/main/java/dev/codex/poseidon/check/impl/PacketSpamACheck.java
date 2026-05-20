package dev.codex.poseidon.check.impl;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class PacketSpamACheck extends Check {
    private final AlertManager alertManager;
    private final int maxPacketsPerSecond;

    public PacketSpamACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("PacketSpamA", plugin, true, 6.0D, CheckCategory.PACKET, CheckSeverity.HIGH, false,
                "Detects excessive inbound packet bursts.");
        this.alertManager = alertManager;
        this.maxPacketsPerSecond = plugin.getConfig().getInt("checks.PacketSpamA.max-packets-per-second", 180);
    }

    @Override
    public void handle(PacketContext context) {
        int packets = context.getData().recordAnyPacket(context.getTimestamp());
        if (packets > maxPacketsPerSecond) {
            alertManager.flag(context.getPlayer(), context.getData(), this,
                    Math.min(3.0D, (packets - maxPacketsPerSecond) / 20.0D),
                    "pps=" + packets);
        }
    }
}
