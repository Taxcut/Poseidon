package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class AutoClickerACheck extends Check {
    private final AlertManager alertManager;
    private final int maxCps;

    public AutoClickerACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("AutoClickerA",
                plugin,
                true,
                5.0D,
                CheckCategory.COMBAT,
                CheckSeverity.MEDIUM,
                false,
                "Combat click rate and burst detection.");
        this.alertManager = alertManager;
        this.maxCps = plugin.getConfig().getInt("checks.AutoClickerA.max-cps", 24);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.USE_ENTITY
                || !context.isAttack()
                || context.getEntityId() < 0) {
            return;
        }

        int cps = context.getData().recordAttackPacket(context.getTimestamp());
        if (cps > maxCps) {
            double amount = Math.min(3.0D, Math.max(1.0D, (cps - maxCps) / 3.0D));
            alertManager.flag(context.getPlayer(), context.getData(), this, amount,
                    "cps=" + cps);
        }
    }
}
