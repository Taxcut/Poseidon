package dev.codex.poseidon.server;

import dev.codex.poseidon.PoseidonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class TpsTracker {
    private final PoseidonPlugin plugin;
    private BukkitTask task;
    private long lastTick;
    private double tps = 20.0D;

    public TpsTracker(PoseidonPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        lastTick = System.currentTimeMillis();
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public double getTps() {
        return tps;
    }

    public boolean isLagging() {
        return tps < plugin.getConfig().getDouble("settings.lag.tps-threshold", 18.5D);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(1L, now - lastTick);
        lastTick = now;
        double instant = Math.min(20.0D, 1000.0D / elapsed);
        tps = tps * 0.92D + instant * 0.08D;
    }
}
