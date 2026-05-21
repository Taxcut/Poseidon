package dev.codex.poseidon.mitigation;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.behavior.BehaviorSignal;
import dev.codex.poseidon.behavior.BehaviorSignalListener;
import dev.codex.poseidon.behavior.SignalCategory;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MitigationManager implements BehaviorSignalListener, Listener {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, ScoreState> scores = new ConcurrentHashMap<UUID, ScoreState>();
    private final Map<UUID, Long> lastSetback = new ConcurrentHashMap<UUID, Long>();
    private BukkitTask decayTask;

    public MitigationManager(PoseidonPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void start() {
        stop();
        if (!isEnabled()) {
            return;
        }

        long interval = Math.max(20L, plugin.getConfig().getLong("mitigations.interval-ticks", 20L));
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                decay();
            }
        }, interval, interval);
    }

    public void stop() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
    }

    public void clear(UUID uuid) {
        scores.remove(uuid);
        lastSetback.remove(uuid);
    }

    @Override
    public void onSignal(BehaviorSignal signal) {
        if (!isEnabled()) {
            return;
        }

        ScoreState state = scores.get(signal.getPlayerUuid());
        if (state == null) {
            state = new ScoreState();
            ScoreState previous = scores.putIfAbsent(signal.getPlayerUuid(), state);
            if (previous != null) {
                state = previous;
            }
        }

        long now = System.currentTimeMillis();
        synchronized (state) {
            state.applyDecay(now, decayPerSecond());
            state.score += signalWeight(signal);
            state.lastUpdate = now;
        }

        PlayerData data = dataManager.get(signal.getPlayerUuid());
        if (data == null) {
            return;
        }
        double effectiveScore = effectiveScore(state.score, data);
        MitigationLevel level = levelFor(effectiveScore);
        long expiresAt = now + plugin.getConfig().getLong("mitigations.duration-ms", 30000L);
        data.setMitigation(level, effectiveScore, expiresAt);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isEnforcing() || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        PlayerData data = dataManager.get(attacker.getUniqueId());
        if (data == null) {
            return;
        }

        MitigationLevel level = data.getMitigationLevel();
        if (!withinMitigatedRange(attacker, event, level)) {
            event.setCancelled(true);
            return;
        }

        double scale = damageScale(level);
        if (scale <= 0.0D) {
            event.setCancelled(true);
        } else if (scale < 1.0D) {
            event.setDamage(event.getDamage() * scale);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEnforcing() || !(event.getWhoClicked() instanceof Player)) {
            return;
        }

        PlayerData data = dataManager.get(((Player) event.getWhoClicked()).getUniqueId());
        if (data != null && atLeast(data.getMitigationLevel(), configuredLevel("mitigations.actions.cancel-inventory-at", MitigationLevel.MEDIUM))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnforcing()) {
            return;
        }

        PlayerData data = dataManager.get(event.getPlayer().getUniqueId());
        if (data != null && atLeast(data.getMitigationLevel(), configuredLevel("mitigations.actions.cancel-world-at", MitigationLevel.HEAVY))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isEnforcing() || !plugin.getConfig().getBoolean("mitigations.actions.movement-setback-heavy", false)) {
            return;
        }

        PlayerData data = dataManager.get(event.getPlayer().getUniqueId());
        if (data == null || data.getMitigationLevel() != MitigationLevel.HEAVY || event.getTo() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (plugin.getExemptionManager().isMovementExempt(event.getPlayer(), data, now)) {
            return;
        }
        Long last = lastSetback.get(event.getPlayer().getUniqueId());
        long cooldown = plugin.getConfig().getLong("mitigations.actions.setback-cooldown-ms", 1500L);
        if (last != null && now - last.longValue() < cooldown) {
            return;
        }

        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double dy = event.getTo().getY() - event.getFrom().getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > 0.80D || dy > 0.90D) {
            event.setTo(event.getFrom());
            lastSetback.put(event.getPlayer().getUniqueId(), Long.valueOf(now));
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("mitigations.enabled", true);
    }

    public boolean isEnforcing() {
        String mode = plugin.getConfig().getString("mitigations.mode", "monitor");
        return "enforce".equalsIgnoreCase(mode) || "production".equalsIgnoreCase(mode);
    }

    private void decay() {
        long now = System.currentTimeMillis();
        for (PlayerData data : dataManager.all()) {
            ScoreState state = scores.get(data.getUuid());
            if (state == null) {
                data.clearMitigation();
                scores.remove(data.getUuid());
                continue;
            }

            synchronized (state) {
                state.applyDecay(now, decayPerSecond());
                double effectiveScore = effectiveScore(state.score, data);
                data.setMitigation(levelFor(effectiveScore), effectiveScore, now + 1000L);
            }
        }
    }

    private double signalWeight(BehaviorSignal signal) {
        double base = Math.max(0.25D, signal.getAmount()) + Math.min(3.0D, signal.getViolation() * 0.08D);
        return base * categoryWeight(signal.getCategory());
    }

    private double categoryWeight(SignalCategory category) {
        String path = "mitigations.weights." + category.name().toLowerCase(java.util.Locale.US);
        return plugin.getConfig().getDouble(path, 1.0D);
    }

    private double damageScale(MitigationLevel level) {
        String key = level.name().toLowerCase(java.util.Locale.US);
        return plugin.getConfig().getDouble("mitigations.actions.combat-damage-scale." + key, 1.0D);
    }

    private boolean withinMitigatedRange(Player attacker, EntityDamageByEntityEvent event, MitigationLevel level) {
        if (!(event.getEntity() instanceof Player) || level == MitigationLevel.NONE) {
            return true;
        }

        String key = level.name().toLowerCase(java.util.Locale.US);
        double limit = plugin.getConfig().getDouble("mitigations.actions.combat-range-limit." + key, 6.0D);
        Location eye = attacker.getEyeLocation();
        Location target = ((Player) event.getEntity()).getLocation();
        double dx = eye.getX() - target.getX();
        double dy = eye.getY() - (target.getY() + 0.9D);
        double dz = eye.getZ() - target.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= limit;
    }

    private double effectiveScore(double localScore, PlayerData data) {
        double crossWeight = Math.max(0.0D, plugin.getConfig().getDouble("mitigations.cross-instance-weight", 0.40D));
        return localScore + data.getCrossInstanceRisk() * crossWeight;
    }

    private MitigationLevel levelFor(double score) {
        if (score >= plugin.getConfig().getDouble("mitigations.thresholds.heavy", 28.0D)) {
            return MitigationLevel.HEAVY;
        }
        if (score >= plugin.getConfig().getDouble("mitigations.thresholds.medium", 16.0D)) {
            return MitigationLevel.MEDIUM;
        }
        if (score >= plugin.getConfig().getDouble("mitigations.thresholds.light", 8.0D)) {
            return MitigationLevel.LIGHT;
        }
        return MitigationLevel.NONE;
    }

    private double decayPerSecond() {
        return Math.max(0.0D, plugin.getConfig().getDouble("mitigations.decay-per-second", 0.75D));
    }

    private MitigationLevel configuredLevel(String path, MitigationLevel fallback) {
        try {
            return MitigationLevel.valueOf(plugin.getConfig().getString(path, fallback.name()).toUpperCase(java.util.Locale.US));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean atLeast(MitigationLevel current, MitigationLevel required) {
        return current.ordinal() >= required.ordinal();
    }

    private static final class ScoreState {
        private double score;
        private long lastUpdate = System.currentTimeMillis();

        private void applyDecay(long now, double decayPerSecond) {
            long elapsed = Math.max(0L, now - lastUpdate);
            if (elapsed > 0L && decayPerSecond > 0.0D) {
                score = Math.max(0.0D, score - (elapsed / 1000.0D) * decayPerSecond);
            }
            lastUpdate = now;
        }
    }
}
