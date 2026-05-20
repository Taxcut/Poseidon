package dev.codex.poseidon.alert;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.behavior.BehaviorSignal;
import dev.codex.poseidon.behavior.BehaviorSignalBus;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.util.AsyncLineWriter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AlertManager {
    private final PoseidonPlugin plugin;
    private final BehaviorSignalBus signalBus;
    private final Set<UUID> disabledAlerts = Collections.synchronizedSet(new HashSet<UUID>());
    private final AsyncLineWriter flagLogWriter;

    public AlertManager(PoseidonPlugin plugin, BehaviorSignalBus signalBus) {
        this.plugin = plugin;
        this.signalBus = signalBus;
        this.flagLogWriter = new AsyncLineWriter(plugin,
                new File(plugin.getDataFolder(), plugin.getConfig().getString("settings.flag-log.file", "flags.jsonl")),
                plugin.getConfig().getLong("settings.flag-log.flush-interval-ticks", 20L),
                plugin.getConfig().getInt("settings.flag-log.max-lines-per-flush", 256));
        if (plugin.getConfig().getBoolean("settings.flag-log.enabled", true)) {
            this.flagLogWriter.start();
        }
    }

    public void flag(Player player, PlayerData data, Check check, double amount, String detail) {
        if (!check.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        double violation = data.addViolation(check.getName(), amount);
        if (violation < check.getAlertVl()) {
            return;
        }

        long cooldown = plugin.getConfig().getLong("settings.alerts.cooldown-ms", 250L);
        if (!data.canAlert(check.getName(), now, cooldown)) {
            return;
        }

        publishSignal(player, data, check, amount, detail, violation, now);

        final String message = color("&8[&3Poseidon&8] &f" + player.getName()
                + " &7failed &c" + check.getName()
                + " &7x" + format(violation)
                + " &8(" + detail + ")");

        if (Bukkit.isPrimaryThread()) {
            dispatch(message);
        } else {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    dispatch(message);
                }
            });
        }
    }

    public void signal(Player player, PlayerData data, Check check, double amount, String detail) {
        if (!check.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        double violation = data.addViolation(check.getName(), amount);
        publishSignal(player, data, check, amount, detail, violation, now);
    }

    private void publishSignal(Player player, PlayerData data, Check check, double amount, String detail, double violation, long now) {
        final String logRecord = buildLogRecord(player, data, check, violation, detail, now);
        writeLog(logRecord);
        signalBus.publish(BehaviorSignal.builder(player)
                .checkName(check.getName())
                .amount(amount)
                .violation(violation)
                .detail(detail)
                .timestamp(now)
                .transactionPing(data.getTransactionPing())
                .position(data.getX(), data.getY(), data.getZ())
                .rotation(data.getYaw(), data.getPitch())
                .instanceId(plugin.getConfig().getString("cloud.instance-id", "default"))
                .build());
    }

    private void dispatch(String message) {
        String permission = plugin.getConfig().getString("settings.alerts.permission", "poseidon.alerts");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission) && !disabledAlerts.contains(staff.getUniqueId())) {
                staff.sendMessage(message);
            }
        }

        if (plugin.getConfig().getBoolean("settings.alerts.console", true)) {
            plugin.getLogger().warning(ChatColor.stripColor(message));
        }
    }

    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledAlerts.contains(uuid)) {
            disabledAlerts.remove(uuid);
            return true;
        }
        disabledAlerts.add(uuid);
        return false;
    }

    public boolean hasAlertsEnabled(Player player) {
        return !disabledAlerts.contains(player.getUniqueId());
    }

    public void stop() {
        flagLogWriter.stop();
    }

    private void writeLog(final String logRecord) {
        if (!plugin.getConfig().getBoolean("settings.flag-log.enabled", true)) {
            return;
        }
        flagLogWriter.enqueue(logRecord);
    }

    private static String buildLogRecord(Player player, PlayerData data, Check check, double violation, String detail, long timestamp) {
        return "{"
                + "\"time\":" + timestamp + ","
                + "\"player\":\"" + escape(player.getName()) + "\","
                + "\"uuid\":\"" + player.getUniqueId().toString() + "\","
                + "\"check\":\"" + escape(check.getName()) + "\","
                + "\"vl\":" + jsonNumber(violation) + ","
                + "\"detail\":\"" + escape(detail) + "\","
                + "\"transactionPing\":" + data.getTransactionPing() + ","
                + "\"x\":" + jsonNumber(data.getX()) + ","
                + "\"y\":" + jsonNumber(data.getY()) + ","
                + "\"z\":" + jsonNumber(data.getZ()) + ","
                + "\"yaw\":" + jsonNumber(data.getYaw()) + ","
                + "\"pitch\":" + jsonNumber(data.getPitch()) + ""
                + "}";
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private static String jsonNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                builder.append('\\').append(c);
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else if (c == '\t') {
                builder.append("\\t");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
