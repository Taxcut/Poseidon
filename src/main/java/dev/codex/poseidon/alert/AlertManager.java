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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertManager {
    private final PoseidonPlugin plugin;
    private final BehaviorSignalBus signalBus;
    private final Set<UUID> disabledAlerts = Collections.synchronizedSet(new HashSet<UUID>());
    private final Set<UUID> verboseStaff = Collections.synchronizedSet(new HashSet<UUID>());
    private final Map<UUID, UUID> verboseTargets = new ConcurrentHashMap<UUID, UUID>();
    private AsyncLineWriter flagLogWriter;

    public AlertManager(PoseidonPlugin plugin, BehaviorSignalBus signalBus) {
        this.plugin = plugin;
        this.signalBus = signalBus;
        reload();
    }

    public void flag(Player player, PlayerData data, Check check, double amount, String detail) {
        if (!check.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        double violation = data.addViolation(check.getName(), amount, check.getMaxVl());
        if (violation < check.getAlertVl()) {
            return;
        }

        long cooldown = plugin.getConfig().getLong("settings.alerts.cooldown-ms", 250L);
        if (!data.canAlert(check.getName(), now, cooldown)) {
            return;
        }

        publishSignal(player, data, check, amount, detail, violation, now);
        if (!check.shouldAlert()) {
            return;
        }

        final String message = color("&8[&3Poseidon&8] &f" + player.getName()
                + " &7failed &c" + check.getName()
                + (check.isExperimental() ? " &8[EXP]" : "")
                + " &7type=&f" + check.getCategory().name()
                + " &7x" + format(violation)
                + " &7ping=&f" + data.getTransactionPing() + "ms"
                + " &7tps=&f" + format(plugin.getTpsTracker().getTps())
                + " &7confidence=&f" + confidenceLabel(data, violation)
                + " &8(" + detail + ")");

        final String verbose = color("&8[&3Poseidon Verbose&8] &7world=&f" + data.getLastWorldName()
                + " &7pos=&f" + format(data.getX()) + "," + format(data.getY()) + "," + format(data.getZ())
                + " &7rot=&f" + format(data.getYaw()) + "," + format(data.getPitch())
                + " &7velocity=&f" + data.getLastVelocitySummary());
        final UUID flaggedUuid = player.getUniqueId();

        if (Bukkit.isPrimaryThread()) {
            dispatch(message, verbose, flaggedUuid);
        } else {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    dispatch(message, verbose, flaggedUuid);
                }
            });
        }
    }

    public void signal(Player player, PlayerData data, Check check, double amount, String detail) {
        if (!check.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        double violation = data.addViolation(check.getName(), amount, check.getMaxVl());
        publishSignal(player, data, check, amount, detail, violation, now);
    }

    private void publishSignal(Player player, PlayerData data, Check check, double amount, String detail, double violation, long now) {
        writeLog(buildLogRecord(player, data, check, violation, detail, now));
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

    private void dispatch(String message, String verbose, UUID flaggedUuid) {
        String permission = plugin.getConfig().getString("settings.alerts.permission", "poseidon.alerts");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission) && !disabledAlerts.contains(staff.getUniqueId())) {
                staff.sendMessage(message);
                UUID target = verboseTargets.get(staff.getUniqueId());
                if (verboseStaff.contains(staff.getUniqueId())
                        && (target == null || target.equals(flaggedUuid))) {
                    staff.sendMessage(verbose);
                }
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

    public boolean setAlerts(Player player, boolean enabled) {
        if (enabled) {
            disabledAlerts.remove(player.getUniqueId());
        } else {
            disabledAlerts.add(player.getUniqueId());
        }
        return enabled;
    }

    public boolean hasAlertsEnabled(Player player) {
        return !disabledAlerts.contains(player.getUniqueId());
    }

    public boolean toggleVerbose(Player player) {
        UUID uuid = player.getUniqueId();
        if (verboseStaff.contains(uuid)) {
            verboseStaff.remove(uuid);
            verboseTargets.remove(uuid);
            return false;
        }
        verboseStaff.add(uuid);
        return true;
    }

    public void setVerboseTarget(Player staff, Player target) {
        verboseStaff.add(staff.getUniqueId());
        verboseTargets.put(staff.getUniqueId(), target.getUniqueId());
    }

    public boolean isVerbose(Player player) {
        return verboseStaff.contains(player.getUniqueId());
    }

    public void clear(UUID uuid) {
        disabledAlerts.remove(uuid);
        verboseStaff.remove(uuid);
        verboseTargets.remove(uuid);
        for (Map.Entry<UUID, UUID> entry : verboseTargets.entrySet()) {
            if (uuid.equals(entry.getValue())) {
                verboseTargets.remove(entry.getKey());
            }
        }
    }

    public int getPendingFlagLogLines() {
        return flagLogWriter == null ? 0 : flagLogWriter.pending();
    }

    public void reload() {
        if (flagLogWriter != null) {
            flagLogWriter.stop();
        }
        flagLogWriter = new AsyncLineWriter(plugin,
                new File(plugin.getDataFolder(), plugin.getConfig().getString("settings.flag-log.file", "flags.jsonl")),
                plugin.getConfig().getLong("settings.flag-log.flush-interval-ticks", 20L),
                plugin.getConfig().getInt("settings.flag-log.max-lines-per-flush", 256),
                plugin.getConfig().getLong("settings.flag-log.max-bytes", 10485760L),
                plugin.getConfig().getInt("settings.flag-log.max-backups", 3));
        if (plugin.getConfig().getBoolean("settings.flag-log.enabled", true)) {
            flagLogWriter.start();
        }
    }

    public void stop() {
        if (flagLogWriter != null) {
            flagLogWriter.stop();
        }
    }

    private void writeLog(final String logRecord) {
        if (!plugin.getConfig().getBoolean("settings.flag-log.enabled", true)) {
            return;
        }
        flagLogWriter.enqueue(logRecord);
    }

    private String buildLogRecord(Player player, PlayerData data, Check check, double violation, String detail, long timestamp) {
        return "{"
                + "\"time\":" + timestamp + ","
                + "\"player\":\"" + escape(player.getName()) + "\","
                + "\"uuid\":\"" + player.getUniqueId().toString() + "\","
                + "\"check\":\"" + escape(check.getName()) + "\","
                + "\"category\":\"" + check.getCategory().name() + "\","
                + "\"severity\":\"" + check.getSeverity().name() + "\","
                + "\"experimental\":" + check.isExperimental() + ","
                + "\"vl\":" + jsonNumber(violation) + ","
                + "\"detail\":\"" + escape(detail) + "\","
                + "\"confidence\":\"" + confidenceLabel(data, violation) + "\","
                + "\"transactionPing\":" + data.getTransactionPing() + ","
                + "\"averageTransactionPing\":" + jsonNumber(data.getAverageTransactionPing()) + ","
                + "\"transactionJitter\":" + jsonNumber(data.getTransactionPingJitter()) + ","
                + "\"tps\":" + jsonNumber(plugin.getTpsTracker().getTps()) + ","
                + "\"world\":\"" + escape(data.getLastWorldName()) + "\","
                + "\"velocity\":\"" + escape(data.getLastVelocitySummary()) + "\","
                + "\"x\":" + jsonNumber(data.getX()) + ","
                + "\"y\":" + jsonNumber(data.getY()) + ","
                + "\"z\":" + jsonNumber(data.getZ()) + ","
                + "\"yaw\":" + jsonNumber(data.getYaw()) + ","
                + "\"pitch\":" + jsonNumber(data.getPitch())
                + "}";
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private static String confidenceLabel(PlayerData data, double violation) {
        double score = Math.max(violation * 0.08D, data.getModelScore());
        if (data.getTransactionPing() > 250L || data.isLatencyCompensated()) {
            score *= 0.75D;
        }
        if (score >= 0.90D) {
            return "CRITICAL";
        }
        if (score >= 0.70D) {
            return "HIGH";
        }
        if (score >= 0.40D) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String jsonNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
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
