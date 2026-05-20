package dev.codex.poseidon.processor;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class RuntimeStateSampler {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private BukkitTask task;

    public RuntimeStateSampler(PoseidonPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void start() {
        stop();
        long interval = Math.max(1L, plugin.getConfig().getLong("settings.runtime-sampler-interval-ticks", 2L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = dataManager.getOrCreate(player);
                    dataManager.updateEntityMapping(player);
                    data.updateRuntimeState(player);
                    data.updateEnvironmentState(
                            isInLiquid(player),
                            isInWeb(player),
                            isOnIce(player),
                            isOnClimbable(player),
                            isOnSoulSand(player),
                            isOnSlime(player),
                            isNearComplexCollision(player));
                }
            }
        }, 1L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static boolean isInLiquid(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material eyes = player.getEyeLocation().getBlock().getType();
        return isLiquid(feet) || isLiquid(eyes);
    }

    private static boolean isInWeb(Player player) {
        return player.getLocation().getBlock().getType() == Material.WEB
                || player.getEyeLocation().getBlock().getType() == Material.WEB;
    }

    private static boolean isOnIce(Player player) {
        Material below = blockBelow(player).getType();
        return below == Material.ICE || below == Material.PACKED_ICE;
    }

    private static boolean isOnClimbable(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material eyes = player.getEyeLocation().getBlock().getType();
        return isClimbable(feet) || isClimbable(eyes);
    }

    private static boolean isOnSoulSand(Player player) {
        return blockBelow(player).getType() == Material.SOUL_SAND;
    }

    private static boolean isOnSlime(Player player) {
        return blockBelow(player).getType() == Material.SLIME_BLOCK;
    }

    private static boolean isNearComplexCollision(Player player) {
        Location base = player.getLocation();
        for (double x = -0.31D; x <= 0.31D; x += 0.31D) {
            for (double z = -0.31D; z <= 0.31D; z += 0.31D) {
                Material below = base.clone().add(x, -0.01D, z).getBlock().getType();
                if (isComplexCollision(below)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Block blockBelow(Player player) {
        Location location = player.getLocation().clone().subtract(0.0D, 0.01D, 0.0D);
        return location.getBlock();
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER
                || material == Material.STATIONARY_WATER
                || material == Material.LAVA
                || material == Material.STATIONARY_LAVA;
    }

    private static boolean isClimbable(Material material) {
        return material == Material.LADDER || material == Material.VINE;
    }

    private static boolean isComplexCollision(Material material) {
        String name = material.name();
        return name.contains("STAIRS")
                || name.contains("STEP")
                || name.contains("SLAB")
                || name.contains("FENCE")
                || name.contains("WALL")
                || material == Material.CARPET
                || material == Material.CAKE_BLOCK
                || material == Material.BED_BLOCK
                || material == Material.TRAP_DOOR
                || material == Material.PISTON_BASE
                || material == Material.PISTON_EXTENSION
                || material == Material.PISTON_STICKY_BASE;
    }
}
