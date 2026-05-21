package dev.codex.poseidon.check;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.impl.BadPacketsACheck;
import dev.codex.poseidon.check.impl.BadPacketsBCheck;
import dev.codex.poseidon.check.impl.BadPacketsCCheck;
import dev.codex.poseidon.check.impl.AutoClickerACheck;
import dev.codex.poseidon.check.impl.AutoClickerBCheck;
import dev.codex.poseidon.check.impl.AutoClickerCCheck;
import dev.codex.poseidon.check.impl.AutoClickerDCheck;
import dev.codex.poseidon.check.impl.AimConsistencyCheck;
import dev.codex.poseidon.check.impl.AimDivisorCheck;
import dev.codex.poseidon.check.impl.AimSmoothCheck;
import dev.codex.poseidon.check.impl.AimSnapCheck;
import dev.codex.poseidon.check.impl.BlockRaytraceCheck;
import dev.codex.poseidon.check.impl.CombatInventoryCheck;
import dev.codex.poseidon.check.impl.FastClickCheck;
import dev.codex.poseidon.check.impl.FastPlaceCheck;
import dev.codex.poseidon.check.impl.ImpossiblePlaceCheck;
import dev.codex.poseidon.check.impl.InventoryACheck;
import dev.codex.poseidon.check.impl.InventoryMoveCheck;
import dev.codex.poseidon.check.impl.LatencyAbuseACheck;
import dev.codex.poseidon.check.impl.MoveACheck;
import dev.codex.poseidon.check.impl.ModelACheck;
import dev.codex.poseidon.check.impl.PacketSpamACheck;
import dev.codex.poseidon.check.impl.PayloadACheck;
import dev.codex.poseidon.check.impl.ReachACheck;
import dev.codex.poseidon.check.impl.ScaffoldRotationCheck;
import dev.codex.poseidon.check.impl.ScaffoldTowerCheck;
import dev.codex.poseidon.check.impl.SimulationACheck;
import dev.codex.poseidon.check.impl.SignExploitACheck;
import dev.codex.poseidon.check.impl.TimerACheck;
import dev.codex.poseidon.check.impl.VelocityACheck;
import dev.codex.poseidon.check.impl.WorldInteractionACheck;
import dev.codex.poseidon.packet.PacketContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class CheckManager {
    private final List<Check> checks = new ArrayList<Check>();
    private final Map<String, AtomicLong> totalNanos = new ConcurrentHashMap<String, AtomicLong>();
    private final Map<String, AtomicLong> executions = new ConcurrentHashMap<String, AtomicLong>();
    private final Map<String, Long> lastErrorLog = new ConcurrentHashMap<String, Long>();
    private PoseidonPlugin plugin;
    private AlertManager alertManager;

    public CheckManager(PoseidonPlugin plugin, AlertManager alertManager) {
        this.plugin = plugin;
        this.alertManager = alertManager;
        reload();
    }

    public void reload() {
        checks.clear();
        totalNanos.clear();
        executions.clear();
        checks.add(new BadPacketsACheck(plugin, alertManager));
        checks.add(new BadPacketsBCheck(plugin, alertManager));
        checks.add(new BadPacketsCCheck(plugin, alertManager));
        checks.add(new TimerACheck(plugin, alertManager));
        checks.add(new LatencyAbuseACheck(plugin, alertManager));
        checks.add(new PacketSpamACheck(plugin, alertManager));
        checks.add(new PayloadACheck(plugin, alertManager));
        checks.add(new SignExploitACheck(plugin, alertManager));
        checks.add(new MoveACheck(plugin, alertManager));
        checks.add(new SimulationACheck(plugin, alertManager));
        checks.add(new VelocityACheck(plugin, alertManager));
        checks.add(new ModelACheck(plugin, alertManager));
        checks.add(new ReachACheck(plugin, alertManager));
        checks.add(new AutoClickerACheck(plugin, alertManager));
        checks.add(new AutoClickerBCheck(plugin, alertManager));
        checks.add(new AutoClickerCCheck(plugin, alertManager));
        checks.add(new AutoClickerDCheck(plugin, alertManager));
        checks.add(new AimSnapCheck(plugin, alertManager));
        checks.add(new AimSmoothCheck(plugin, alertManager));
        checks.add(new AimDivisorCheck(plugin, alertManager));
        checks.add(new AimConsistencyCheck(plugin, alertManager));
        checks.add(new InventoryMoveCheck(plugin, alertManager));
        checks.add(new FastClickCheck(plugin, alertManager));
        checks.add(new CombatInventoryCheck(plugin, alertManager));
        checks.add(new FastPlaceCheck(plugin, alertManager));
        checks.add(new ImpossiblePlaceCheck(plugin, alertManager));
        checks.add(new ScaffoldRotationCheck(plugin, alertManager));
        checks.add(new ScaffoldTowerCheck(plugin, alertManager));
        checks.add(new BlockRaytraceCheck(plugin, alertManager));
        checks.add(new InventoryACheck(plugin, alertManager));
        checks.add(new WorldInteractionACheck(plugin, alertManager));
    }

    public void handle(PacketContext context) {
        if (context.getPlayer().hasPermission("poseidon.bypass")) {
            return;
        }
        for (Check check : checks) {
            if (check.isEnabled()) {
                long started = System.nanoTime();
                try {
                    check.handle(context);
                } catch (RuntimeException exception) {
                    logCheckError(check, exception);
                } finally {
                    long elapsed = System.nanoTime() - started;
                    counter(totalNanos, check.getName()).addAndGet(elapsed);
                    counter(executions, check.getName()).incrementAndGet();
                }
            }
        }
    }

    public int size() {
        return checks.size();
    }

    public List<Check> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    public Check getCheck(String name) {
        for (Check check : checks) {
            if (check.getName().equalsIgnoreCase(name)) {
                return check;
            }
        }
        return null;
    }

    public boolean setEnabled(String name, boolean enabled) {
        Check check = getCheck(name);
        if (check == null) {
            return false;
        }
        plugin.getConfig().set("checks." + check.getName() + ".enabled", Boolean.valueOf(enabled));
        plugin.saveConfig();
        reload();
        return true;
    }

    public void decayViolations(dev.codex.poseidon.data.PlayerData data, double fallbackAmount) {
        for (Check check : checks) {
            double decay = check.getDecay() > 0.0D ? check.getDecay() : fallbackAmount;
            data.decayViolation(check.getName(), decay);
        }
    }

    public double averageMicros(String checkName) {
        AtomicLong nanos = totalNanos.get(checkName);
        AtomicLong count = executions.get(checkName);
        if (nanos == null || count == null || count.get() <= 0L) {
            return 0.0D;
        }
        return nanos.get() / (double) count.get() / 1000.0D;
    }

    private static AtomicLong counter(Map<String, AtomicLong> map, String key) {
        AtomicLong counter = map.get(key);
        if (counter != null) {
            return counter;
        }
        AtomicLong created = new AtomicLong();
        AtomicLong previous = map.putIfAbsent(key, created);
        return previous == null ? created : previous;
    }

    private void logCheckError(Check check, RuntimeException exception) {
        long now = System.currentTimeMillis();
        Long last = lastErrorLog.get(check.getName());
        if (last != null && now - last.longValue() < 10000L) {
            return;
        }
        lastErrorLog.put(check.getName(), Long.valueOf(now));
        plugin.getLogger().warning("Check " + check.getName() + " failed: " + exception.getClass().getSimpleName()
                + ": " + exception.getMessage());
    }
}
