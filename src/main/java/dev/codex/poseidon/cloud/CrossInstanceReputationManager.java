package dev.codex.poseidon.cloud;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.behavior.BehaviorSignal;
import dev.codex.poseidon.behavior.BehaviorSignalListener;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import dev.codex.poseidon.util.AsyncLineWriter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CrossInstanceReputationManager implements BehaviorSignalListener {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Object fileLock = new Object();
    private final AsyncLineWriter reputationWriter;
    private BukkitTask importTask;

    public CrossInstanceReputationManager(PoseidonPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.reputationWriter = new AsyncLineWriter(plugin,
                reputationFile(),
                plugin.getConfig().getLong("cloud.cross-instance.flush-interval-ticks", 40L),
                plugin.getConfig().getInt("cloud.cross-instance.max-lines-per-flush", 512),
                plugin.getConfig().getLong("cloud.cross-instance.max-file-bytes", 10485760L),
                plugin.getConfig().getInt("cloud.cross-instance.max-backups", 2));
    }

    public void start() {
        stop();
        if (!isEnabled()) {
            return;
        }

        reputationWriter.start();
        long interval = Math.max(20L, plugin.getConfig().getLong("cloud.cross-instance.import-interval-ticks", 100L));
        importTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                importReputation();
            }
        }, interval, interval);
    }

    public void stop() {
        if (importTask != null) {
            importTask.cancel();
            importTask = null;
        }
        reputationWriter.stop();
    }

    @Override
    public void onSignal(BehaviorSignal signal) {
        if (!isEnabled()) {
            return;
        }

        reputationWriter.enqueue(toRecord(signal));
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("cloud.enabled", false)
                && plugin.getConfig().getBoolean("cloud.cross-instance.enabled", false);
    }

    private void importReputation() {
        File file = reputationFile();
        if (!file.exists()) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxAge = plugin.getConfig().getLong("cloud.cross-instance.max-record-age-ms", 86400000L);
        final Map<UUID, Aggregate> aggregates = new HashMap<UUID, Aggregate>();

        synchronized (fileLock) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    ReputationRecord record = ReputationRecord.parse(line);
                    if (record == null || now - record.timestamp > maxAge) {
                        continue;
                    }

                    Aggregate aggregate = aggregates.get(record.uuid);
                    if (aggregate == null) {
                        aggregate = new Aggregate();
                        aggregates.put(record.uuid, aggregate);
                    }
                    double weight = currentInstance().equals(record.instanceId)
                            ? plugin.getConfig().getDouble("cloud.cross-instance.local-weight", 1.0D)
                            : plugin.getConfig().getDouble("cloud.cross-instance.remote-weight", 0.75D);
                    aggregate.risk += record.risk * weight;
                    aggregate.records++;
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to import cross-instance reputation: " + exception.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                apply(aggregates);
            }
        });
    }

    private void apply(Map<UUID, Aggregate> aggregates) {
        double minimumRisk = plugin.getConfig().getDouble("cloud.cross-instance.minimum-risk", 10.0D);
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Aggregate aggregate = aggregates.get(player.getUniqueId());
            PlayerData data = dataManager.getOrCreate(player);
            if (aggregate == null || aggregate.risk < minimumRisk) {
                data.setCrossInstanceRisk(0.0D, 0, now);
            } else {
                data.setCrossInstanceRisk(aggregate.risk, aggregate.records, now);
            }
        }
    }

    private File reputationFile() {
        String configured = plugin.getConfig().getString("cloud.cross-instance.reputation-file", "cross-instance-reputation.jsonl");
        File file = new File(configured);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(plugin.getDataFolder(), configured);
    }

    private String toRecord(BehaviorSignal signal) {
        double risk = Math.max(0.25D, signal.getAmount()) + Math.min(10.0D, signal.getViolation() * 0.15D);
        return signal.getTimestamp() + "|"
                + currentInstance() + "|"
                + signal.getPlayerUuid() + "|"
                + signal.getCategory().name() + "|"
                + format(risk) + "|"
                + sanitize(signal.getCheckName());
    }

    private String currentInstance() {
        return plugin.getConfig().getString("cloud.instance-id", "default");
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', '_').replace('\n', '_').replace('\r', '_');
    }

    private static final class Aggregate {
        private double risk;
        private int records;
    }

    private static final class ReputationRecord {
        private final long timestamp;
        private final String instanceId;
        private final UUID uuid;
        private final double risk;

        private ReputationRecord(long timestamp, String instanceId, UUID uuid, double risk) {
            this.timestamp = timestamp;
            this.instanceId = instanceId;
            this.uuid = uuid;
            this.risk = risk;
        }

        private static ReputationRecord parse(String line) {
            String[] parts = line.split("\\|");
            if (parts.length < 5) {
                return null;
            }
            try {
                return new ReputationRecord(Long.parseLong(parts[0]), parts[1], UUID.fromString(parts[2]), Double.parseDouble(parts[4]));
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }
}
