package dev.codex.poseidon.model;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.behavior.BehaviorSignal;
import dev.codex.poseidon.behavior.BehaviorSignalListener;
import dev.codex.poseidon.behavior.SignalCategory;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OnlineBehaviorModel implements BehaviorSignalListener {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, ModelProfile> profiles = new ConcurrentHashMap<UUID, ModelProfile>();
    private BukkitTask decayTask;

    public OnlineBehaviorModel(PoseidonPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void start() {
        stop();
        if (!isEnabled()) {
            return;
        }

        long interval = Math.max(20L, plugin.getConfig().getLong("model.interval-ticks", 20L));
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                decayAll();
            }
        }, interval, interval);
    }

    public void stop() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
    }

    @Override
    public void onSignal(BehaviorSignal signal) {
        if (!isEnabled()) {
            return;
        }
        if ("ModelA".equalsIgnoreCase(signal.getCheckName())) {
            return;
        }

        ModelProfile profile = profiles.get(signal.getPlayerUuid());
        if (profile == null) {
            profile = new ModelProfile();
            ModelProfile previous = profiles.putIfAbsent(signal.getPlayerUuid(), profile);
            if (previous != null) {
                profile = previous;
            }
        }

        long now = System.currentTimeMillis();
        synchronized (profile) {
            profile.applyDecay(now, decayPerSecond());
            profile.add(signal.getCategory(), signalWeight(signal));
            profile.samples++;
            profile.lastUpdate = now;
            updateData(signal.getPlayerUuid(), profile, now);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("model.enabled", true);
    }

    private void decayAll() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, ModelProfile> entry : profiles.entrySet()) {
            ModelProfile profile = entry.getValue();
            synchronized (profile) {
                profile.applyDecay(now, decayPerSecond());
                updateData(entry.getKey(), profile, now);
            }
        }
    }

    private void updateData(UUID uuid, ModelProfile profile, long now) {
        PlayerData data = dataManager.get(uuid);
        if (data == null) {
            return;
        }
        data.setModelScore(score(profile), confidence(profile), now, profile.breakdown());
    }

    private double score(ModelProfile profile) {
        double weightedSum = profile.movement * weight(SignalCategory.MOVEMENT)
                + profile.combat * weight(SignalCategory.COMBAT)
                + profile.inventory * weight(SignalCategory.INVENTORY)
                + profile.world * weight(SignalCategory.WORLD)
                + profile.packet * weight(SignalCategory.PACKET)
                + profile.timing * weight(SignalCategory.TIMING)
                + profile.latency * weight(SignalCategory.LATENCY)
                + profile.behavior * weight(SignalCategory.BEHAVIOR);
        double bias = plugin.getConfig().getDouble("model.bias", 7.5D);
        double scale = Math.max(1.0D, plugin.getConfig().getDouble("model.scale", 16.0D));
        return 1.0D / (1.0D + Math.exp(-((weightedSum - bias) / scale)));
    }

    private double confidence(ModelProfile profile) {
        return Math.min(1.0D, profile.samples / 20.0D);
    }

    private double signalWeight(BehaviorSignal signal) {
        return Math.max(0.25D, signal.getAmount()) + Math.min(4.0D, signal.getViolation() * 0.10D);
    }

    private double weight(SignalCategory category) {
        String key = category.name().toLowerCase(java.util.Locale.US);
        return plugin.getConfig().getDouble("model.weights." + key, 1.0D);
    }

    private double decayPerSecond() {
        return Math.max(0.0D, plugin.getConfig().getDouble("model.decay-per-second", 0.55D));
    }

    private static final class ModelProfile {
        private double movement;
        private double combat;
        private double inventory;
        private double world;
        private double packet;
        private double timing;
        private double latency;
        private double behavior;
        private double samples;
        private long lastUpdate = System.currentTimeMillis();

        private void add(SignalCategory category, double amount) {
            if (category == SignalCategory.MOVEMENT) {
                movement += amount;
            } else if (category == SignalCategory.COMBAT) {
                combat += amount;
            } else if (category == SignalCategory.INVENTORY) {
                inventory += amount;
            } else if (category == SignalCategory.WORLD) {
                world += amount;
            } else if (category == SignalCategory.PACKET) {
                packet += amount;
            } else if (category == SignalCategory.TIMING) {
                timing += amount;
            } else if (category == SignalCategory.LATENCY) {
                latency += amount;
            } else {
                behavior += amount;
            }
        }

        private void applyDecay(long now, double decayPerSecond) {
            long elapsed = Math.max(0L, now - lastUpdate);
            if (elapsed <= 0L || decayPerSecond <= 0.0D) {
                lastUpdate = now;
                return;
            }

            double decay = (elapsed / 1000.0D) * decayPerSecond;
            movement = Math.max(0.0D, movement - decay);
            combat = Math.max(0.0D, combat - decay);
            inventory = Math.max(0.0D, inventory - decay);
            world = Math.max(0.0D, world - decay);
            packet = Math.max(0.0D, packet - decay);
            timing = Math.max(0.0D, timing - decay);
            latency = Math.max(0.0D, latency - decay);
            behavior = Math.max(0.0D, behavior - decay);
            samples = Math.max(0.0D, samples - decay * 0.25D);
            lastUpdate = now;
        }

        private String breakdown() {
            return "movement=" + format(movement)
                    + ",combat=" + format(combat)
                    + ",inventory=" + format(inventory)
                    + ",world=" + format(world)
                    + ",packet=" + format(packet)
                    + ",timing=" + format(timing)
                    + ",latency=" + format(latency)
                    + ",behavior=" + format(behavior);
        }

        private static String format(double value) {
            return String.format(java.util.Locale.US, "%.2f", value);
        }
    }
}
