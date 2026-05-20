package dev.codex.poseidon.command;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PoseidonCommand implements CommandExecutor, TabCompleter {
    private final PoseidonPlugin plugin;
    private final PlayerDataManager dataManager;
    private final AlertManager alertManager;

    public PoseidonCommand(PoseidonPlugin plugin, PlayerDataManager dataManager, AlertManager alertManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.alertManager = alertManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("alerts")) {
            toggleAlerts(sender);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = normalizeSubCommand(args[0]);

        if (subCommand.equals("alerts")) {
            toggleAlerts(sender);
            return true;
        }

        if (subCommand.equals("info")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /" + label + " info <player>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cThat player is not online."));
                return true;
            }

            PlayerData data = dataManager.getOrCreate(target);
            sender.sendMessage(color("&8&m----------------&r &3Poseidon Debug &8&m----------------"));
            sender.sendMessage(color("&7Player: &f" + target.getName()));
            sender.sendMessage(color("&7Transaction ping: &f" + data.getTransactionPing() + "ms"));
            sender.sendMessage(color("&7Pending transactions: &f" + data.getPendingTransactionCount()));
            sender.sendMessage(color("&7Latency compensation: &f" + data.isLatencyCompensated()));
            sender.sendMessage(color("&7Last velocity: &f" + data.getLastVelocitySummary()));
            sender.sendMessage(color("&7Position history: &f" + data.getPositionHistorySize() + " samples"));
            sender.sendMessage(color("&7Mitigation: &f" + data.getMitigationLevel().name()
                    + " &7score=&f" + format(data.getMitigationScore())));
            sender.sendMessage(color("&7Model: &fscore=" + format(data.getModelScore())
                    + " confidence=" + format(data.getModelConfidence())));
            sender.sendMessage(color("&7Model features: &f" + data.getModelBreakdown()));
            sender.sendMessage(color("&7Cross-instance: &frisk=" + format(data.getCrossInstanceRisk())
                    + " records=" + data.getCrossInstanceRecords()));
            sender.sendMessage(color("&7Last move: &f" + format(data.getX()) + ", " + format(data.getY()) + ", " + format(data.getZ())));
            sender.sendMessage(color("&7Rotation: &f" + format(data.getYaw()) + ", " + format(data.getPitch())));
            sender.sendMessage(color("&7On ground: &f" + data.isOnGround()));
            if (data.getViolations().isEmpty()) {
                sender.sendMessage(color("&7Violations: &fnone"));
            } else {
                for (Map.Entry<String, Double> entry : data.getViolations().entrySet()) {
                    sender.sendMessage(color("&7" + entry.getKey() + ": &f" + format(entry.getValue())));
                }
            }
            sender.sendMessage(color("&8&m------------------------------------------------"));
            return true;
        }

        if (subCommand.equals("cloud")) {
            sender.sendMessage(color("&8&m----------------&r &3Poseidon Cloud &8&m----------------"));
            sender.sendMessage(color("&7Enabled: &f" + plugin.getCloudSyncManager().isEnabled()));
            sender.sendMessage(color("&7Instance: &f" + plugin.getConfig().getString("cloud.instance-id", "default")));
            sender.sendMessage(color("&7Endpoint configured: &f" + plugin.getCloudSyncManager().hasEndpoint()));
            sender.sendMessage(color("&7Spooled signals: &f" + plugin.getCloudSyncManager().getSpooledSignals()));
            sender.sendMessage(color("&7Sent signals: &f" + plugin.getCloudSyncManager().getSentSignals()));
            sender.sendMessage(color("&7Failed sends: &f" + plugin.getCloudSyncManager().getFailedSignals()));
            sender.sendMessage(color("&8&m------------------------------------------------"));
            return true;
        }

        if (subCommand.equals("version")) {
            sender.sendMessage(color("&8[&3Poseidon&8] &7Version &f" + plugin.getDescription().getVersion()
                    + " &7with &f" + plugin.getDescription().getDepend().size() + " &7runtime depend(s)."));
            return true;
        }

        if (subCommand.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(color("&8[&3Poseidon&8] &7Config reloaded. Restart recommended for check threshold changes."));
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("alerts")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(Arrays.asList("alerts", "cloud", "info", "reload", "version"), args[0]);
        }
        if (args.length == 2 && normalizeSubCommand(args[0]).equals("info")) {
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(color("&8&m----------------&r &3Poseidon &8&m----------------"));
        sender.sendMessage(color("&7/" + label + " alerts &8- &fToggle staff alerts."));
        sender.sendMessage(color("&7/" + label + " cloud &8- &fView cloud sync status."));
        sender.sendMessage(color("&7/" + label + " info <player> &8- &fView live player state."));
        sender.sendMessage(color("&7/" + label + " reload &8- &fReload config values."));
        sender.sendMessage(color("&7/" + label + " version &8- &fView plugin version."));
        sender.sendMessage(color("&8Aliases: &7a, i, c, rl, ver"));
        sender.sendMessage(color("&8&m------------------------------------------"));
    }

    private void toggleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can toggle personal alerts."));
            return;
        }

        Player player = (Player) sender;
        boolean enabled = alertManager.toggleAlerts(player);
        player.sendMessage(color("&8[&3Poseidon&8] &7Alerts " + (enabled ? "&aenabled" : "&cdisabled") + "&7."));
    }

    private static String normalizeSubCommand(String input) {
        if (input.equalsIgnoreCase("a") || input.equalsIgnoreCase("alert") || input.equalsIgnoreCase("alerts")
                || input.equalsIgnoreCase("togglealerts") || input.equalsIgnoreCase("toggle")) {
            return "alerts";
        }
        if (input.equalsIgnoreCase("i") || input.equalsIgnoreCase("info") || input.equalsIgnoreCase("debug")
                || input.equalsIgnoreCase("d") || input.equalsIgnoreCase("data") || input.equalsIgnoreCase("player")) {
            return "info";
        }
        if (input.equalsIgnoreCase("c") || input.equalsIgnoreCase("cloud") || input.equalsIgnoreCase("sync")
                || input.equalsIgnoreCase("status")) {
            return "cloud";
        }
        if (input.equalsIgnoreCase("r") || input.equalsIgnoreCase("rl") || input.equalsIgnoreCase("reload")
                || input.equalsIgnoreCase("refresh")) {
            return "reload";
        }
        if (input.equalsIgnoreCase("v") || input.equalsIgnoreCase("ver") || input.equalsIgnoreCase("version")
                || input.equalsIgnoreCase("about")) {
            return "version";
        }
        return input.toLowerCase(java.util.Locale.US);
    }

    private static List<String> filter(List<String> values, String input) {
        List<String> result = new ArrayList<String>();
        String lower = input.toLowerCase(java.util.Locale.US);
        for (String value : values) {
            if (value.toLowerCase(java.util.Locale.US).startsWith(lower)) {
                result.add(value);
            }
        }
        Collections.sort(result);
        return result;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
