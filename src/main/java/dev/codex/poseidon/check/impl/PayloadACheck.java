package dev.codex.poseidon.check.impl;

import com.comphenix.protocol.PacketType;
import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckCategory;
import dev.codex.poseidon.check.CheckSeverity;
import dev.codex.poseidon.packet.PacketContext;

public final class PayloadACheck extends Check {
    private final AlertManager alertManager;
    private final int maxBytes;

    public PayloadACheck(PoseidonPlugin plugin, AlertManager alertManager) {
        super("PayloadA", plugin, true, 1.0D, CheckCategory.PACKET, CheckSeverity.CRITICAL, false,
                "Protects against oversized custom payload packets.");
        this.alertManager = alertManager;
        this.maxBytes = plugin.getConfig().getInt("checks.PayloadA.max-bytes", 32767);
    }

    @Override
    public void handle(PacketContext context) {
        if (context.getPacketType() != PacketType.Play.Client.CUSTOM_PAYLOAD) {
            return;
        }
        String channel = safeString(context, 0);
        int size = payloadSize(context);
        if (size > maxBytes || channel.length() > 32) {
            alertManager.flag(context.getPlayer(), context.getData(), this, 1.0D,
                    "channel=" + channel + ",bytes=" + size);
        }
    }

    private static String safeString(PacketContext context, int index) {
        try {
            if (context.getPacket().getStrings().size() <= index) {
                return "";
            }
            String value = context.getPacket().getStrings().read(index);
            return value == null ? "" : value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int payloadSize(PacketContext context) {
        try {
            if (context.getPacket().getByteArrays().size() > 0) {
                byte[] bytes = context.getPacket().getByteArrays().read(0);
                return bytes == null ? 0 : bytes.length;
            }
        } catch (RuntimeException ignored) {
        }
        return 0;
    }
}
