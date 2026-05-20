package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;

public final class LatencyAbuseACheck extends Check {
    private final AlertManager alertManager;
    private final boolean alert;
    private final long signalIntervalMs;
    private final int maxPendingTransactions;
    private final long staleTransactionMs;

    public LatencyAbuseACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("LatencyAbuseA",
                plugin.getConfig().getBoolean("checks.LatencyAbuseA.enabled", true),
                plugin.getConfig().getDouble("checks.LatencyAbuseA.alert-vl", 999.0D));
        this.alertManager = alertManager;
        this.alert = plugin.getConfig().getBoolean("checks.LatencyAbuseA.alert", false);
        this.signalIntervalMs = plugin.getConfig().getLong("checks.LatencyAbuseA.signal-interval-ms", 1000L);
        this.maxPendingTransactions = plugin.getConfig().getInt("checks.LatencyAbuseA.max-pending-transactions", 6);
        this.staleTransactionMs = plugin.getConfig().getLong("checks.LatencyAbuseA.stale-transaction-ms", 2500L);
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

        PlayerData data = context.getData();
        boolean pending = data.getPendingTransactionCount() > maxPendingTransactions;
        boolean stale = data.getLastTransactionSent() > 0L
                && context.getTimestamp() - data.getLastTransactionReceived() > staleTransactionMs
                && context.getTimestamp() - data.getLastTransactionSent() > staleTransactionMs;

        if (pending || stale) {
            data.setLatencyCompensated(true, context.getTimestamp());
            String detail = "pending=" + data.getPendingTransactionCount()
                    + ",ping=" + data.getTransactionPing() + "ms,stale=" + stale;
            if (alert) {
                alertManager.flag(context.getPlayer(), data, this, 1.0D, detail);
            } else if (data.canLatencySignal(context.getTimestamp(), signalIntervalMs)) {
                alertManager.signal(context.getPlayer(), data, this, 0.5D, detail);
            }
        } else if (data.isLatencyCompensated()) {
            data.setLatencyCompensated(false, context.getTimestamp());
        }
    }
}
