package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class TimerACheck extends Check {
    private final AlertManager alertManager;
    private final int maxPackets;
    private final double decayPerSecond;

    public TimerACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("TimerA",
                plugin,
                true,
                4.0D,
                CheckCategory.MOVEMENT,
                CheckSeverity.HIGH,
                false,
                "Detects sustained excess flying packet rate.");
        this.alertManager = alertManager;
        this.maxPackets = plugin.getConfig().getInt("checks.TimerA.max-flying-packets-per-second", 55);
        this.decayPerSecond = plugin.getConfig().getDouble("checks.TimerA.decay-per-second", 0.35D);
    }

    @Override
    public void handle(PacketContext context) {
        PacketType type = context.getPacketType();
        if (type != PacketType.Play.Client.FLYING
                && type != PacketType.Play.Client.POSITION
                && type != PacketType.Play.Client.POSITION_LOOK
                && type != PacketType.Play.Client.LOOK) {
            return;
        }

        int packets = context.getData().recordFlyingPacket(context.getTimestamp());
        context.getData().decayViolation(getName(), decayPerSecond);

        if (packets > maxPackets) {
            double amount = Math.min(2.0D, (packets - maxPackets) / 8.0D);
            alertManager.flag(context.getPlayer(), context.getData(), this, amount,
                    "packets=" + packets + ",ping=" + context.getData().getTransactionPing() + "ms");
        }
    }
}
