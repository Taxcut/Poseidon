package dev.codex.poseidon.action;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.behavior.BehaviorSignal;
import dev.codex.poseidon.behavior.BehaviorSignalListener;
import dev.codex.poseidon.check.Check;
import org.bukkit.Bukkit;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionManager implements BehaviorSignalListener {
    private final PoseidonPlugin plugin;
    private final Map<String, Queue<Long>> recentFlags = new ConcurrentHashMap<String, Queue<Long>>();
    private final Map<String, Long> lastCommand = new ConcurrentHashMap<String, Long>();

    public ActionManager(PoseidonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSignal(final BehaviorSignal signal) {
        if (!plugin.getConfig().getBoolean("punishments.enabled", false)) {
            return;
        }

        final Check check = plugin.getCheckManager().getCheck(signal.getCheckName());
        if (check == null || !check.shouldPunish()) {
            return;
        }
        if (check.isExperimental() && !plugin.getConfig().getBoolean("punishments.allow-experimental", false)) {
            return;
        }
        if (signal.getViolation() < plugin.getConfig().getDouble("checks." + check.getName() + ".punish-vl", check.getMaxVl())) {
            return;
        }

        long now = System.currentTimeMillis();
        String key = signal.getPlayerUuid().toString() + ":" + check.getName();
        Queue<Long> queue = recentFlags.get(key);
        if (queue == null) {
            queue = new ArrayDeque<Long>();
            Queue<Long> previous = recentFlags.putIfAbsent(key, queue);
            if (previous != null) {
                queue = previous;
            }
        }

        synchronized (queue) {
            if (!ActionRequirement.recordAndTest(queue, now,
                    plugin.getConfig().getLong("punishments.require-recent.window-ms", 60000L),
                    plugin.getConfig().getInt("punishments.require-recent.flags", 3),
                    plugin.getConfig().getInt("punishments.require-recent.max-stored", 12))) {
                return;
            }
        }

        long cooldown = plugin.getConfig().getLong("punishments.command-cooldown-ms", 300000L);
        Long last = lastCommand.get(key);
        if (last != null && now - last.longValue() < cooldown) {
            return;
        }
        lastCommand.put(key, Long.valueOf(now));

        if (Bukkit.isPrimaryThread()) {
            execute(signal, check);
        } else {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    execute(signal, check);
                }
            });
        }
    }

    public void clear(UUID uuid) {
        String prefix = uuid.toString() + ":";
        for (String key : recentFlags.keySet()) {
            if (key.startsWith(prefix)) {
                recentFlags.remove(key);
            }
        }
        for (String key : lastCommand.keySet()) {
            if (key.startsWith(prefix)) {
                lastCommand.remove(key);
            }
        }
    }

    private void execute(BehaviorSignal signal, Check check) {
        List<String> actions = plugin.getConfig().getStringList("checks." + check.getName() + ".actions");
        if (actions.isEmpty()) {
            actions = plugin.getConfig().getStringList("punishments.default-actions");
        }
        for (String action : actions) {
            executeAction(action, signal, check);
        }

        List<String> commands = plugin.getConfig().getStringList("checks." + check.getName() + ".commands");
        if (commands.isEmpty()) {
            commands = plugin.getConfig().getStringList("punishments.default-commands");
        }

        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            String rendered = placeholders(command, signal, check);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered);
        }
    }

    private void executeAction(String action, BehaviorSignal signal, Check check) {
        if (action == null || action.trim().isEmpty()) {
            return;
        }
        String lower = action.toLowerCase(java.util.Locale.US);
        if (lower.equals("alert") || lower.equals("log") || lower.equals("staff-debug")
                || lower.equals("cancel") || lower.equals("setback")) {
            return;
        }
        if (lower.startsWith("command:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders(action.substring("command:".length()).trim(), signal, check));
        } else if (lower.startsWith("kick:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + signal.getPlayerName() + " "
                    + placeholders(action.substring("kick:".length()).trim(), signal, check));
        } else if (lower.startsWith("ban:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + signal.getPlayerName() + " "
                    + placeholders(action.substring("ban:".length()).trim(), signal, check));
        }
    }

    private String placeholders(String command, BehaviorSignal signal, Check check) {
        return command
                .replace("{player}", signal.getPlayerName())
                .replace("{uuid}", signal.getPlayerUuid().toString())
                .replace("{check}", check.getName())
                .replace("{type}", check.getCategory().name())
                .replace("{vl}", format(signal.getViolation()))
                .replace("{ping}", String.valueOf(signal.getTransactionPing()))
                .replace("{tps}", format(plugin.getTpsTracker().getTps()))
                .replace("{reason}", check.getName() + " " + check.getCategory().name());
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
