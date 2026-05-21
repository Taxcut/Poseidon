package dev.codex.poseidon.replay;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.packet.PacketContext;
import dev.codex.poseidon.util.AsyncLineWriter;
import org.bukkit.entity.Player;

import java.io.File;

public final class ReplayRecorder {
    private final PoseidonPlugin plugin;
    private AsyncLineWriter writer;

    public ReplayRecorder(PoseidonPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (writer != null) {
            writer.stop();
        }
        writer = new AsyncLineWriter(plugin,
                new File(plugin.getDataFolder(), plugin.getConfig().getString("replay.file", "replay-sessions.jsonl")),
                plugin.getConfig().getLong("replay.flush-interval-ticks", 40L),
                plugin.getConfig().getInt("replay.max-lines-per-flush", 512),
                plugin.getConfig().getLong("replay.max-bytes", 20971520L),
                plugin.getConfig().getInt("replay.max-backups", 2));
        if (isEnabled()) {
            writer.start();
        }
    }

    public void stop() {
        if (writer != null) {
            writer.stop();
        }
    }

    public void record(PacketContext context) {
        if (!isEnabled() || !shouldRecord(context)) {
            return;
        }
        PlayerData data = context.getData();
        writer.enqueue("{"
                + "\"time\":" + context.getTimestamp() + ","
                + "\"type\":\"" + context.getPacketType().name() + "\","
                + "\"player\":\"" + escape(context.getPlayer().getName()) + "\","
                + "\"uuid\":\"" + context.getPlayer().getUniqueId().toString() + "\","
                + "\"world\":\"" + escape(data.getLastWorldName()) + "\","
                + "\"x\":" + number(context.getX()) + ","
                + "\"y\":" + number(context.getY()) + ","
                + "\"z\":" + number(context.getZ()) + ","
                + "\"yaw\":" + number(context.getYaw()) + ","
                + "\"pitch\":" + number(context.getPitch()) + ","
                + "\"ground\":" + context.isOnGround() + ","
                + "\"entityId\":" + context.getEntityId()
                + "}");
    }

    public void recordVelocity(Player player, PlayerData data, double x, double y, double z, long timestamp) {
        if (!isEnabled()) {
            return;
        }
        writer.enqueue("{"
                + "\"time\":" + timestamp + ","
                + "\"type\":\"SERVER_VELOCITY\","
                + "\"player\":\"" + escape(player.getName()) + "\","
                + "\"uuid\":\"" + player.getUniqueId().toString() + "\","
                + "\"world\":\"" + escape(data.getLastWorldName()) + "\","
                + "\"vx\":" + number(x) + ","
                + "\"vy\":" + number(y) + ","
                + "\"vz\":" + number(z)
                + "}");
    }

    public int pending() {
        return writer == null ? 0 : writer.pending();
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("replay.enabled", false);
    }

    private boolean shouldRecord(PacketContext context) {
        return context.hasPosition()
                || context.hasRotation()
                || context.getEntityId() >= 0
                || context.getPacketType().name().contains("BLOCK_PLACE")
                || context.getPacketType().name().contains("WINDOW_CLICK");
    }

    private static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
