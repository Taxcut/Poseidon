package dev.codex.poseidon.command;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.alert.AlertManager;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.data.PlayerData;
import dev.codex.poseidon.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
            if (!has(sender, "poseidon.alerts")) {
                return true;
            }
            toggleAlerts(sender, null);
            return true;
        }

        String subCommand = args.length == 0 ? "help" : normalizeSubCommand(args[0]);
        if (subCommand.equals("help")) {
            sendHelp(sender, label);
            return true;
        }
        if (subCommand.equals("reload")) {
            if (!has(sender, "poseidon.reload")) {
                return true;
            }
            plugin.reloadPoseidon();
            sender.sendMessage(color("&8[&3Poseidon&8] &aConfiguration and checks reloaded."));
            return true;
        }
        if (subCommand.equals("alerts")) {
            if (!has(sender, "poseidon.alerts")) {
                return true;
            }
            toggleAlerts(sender, args.length >= 2 ? args[1] : null);
            return true;
        }
        if (subCommand.equals("verbose")) {
            if (!has(sender, "poseidon.verbose")) {
                return true;
            }
            handleVerbose(sender, args);
            return true;
        }
        if (subCommand.equals("info")) {
            if (!has(sender, "poseidon.staff")) {
                return true;
            }
            handleInfo(sender, args);
            return true;
        }
        if (subCommand.equals("checks")) {
            if (!has(sender, "poseidon.staff")) {
                return true;
            }
            handleChecks(sender);
            return true;
        }
        if (subCommand.equals("check")) {
            if (!has(sender, "poseidon.staff")) {
                return true;
            }
            handleCheck(sender, args);
            return true;
        }
        if (subCommand.equals("toggle")) {
            if (!has(sender, "poseidon.admin")) {
                return true;
            }
            handleToggle(sender, args);
            return true;
        }
        if (subCommand.equals("violations")) {
            if (!has(sender, "poseidon.staff")) {
                return true;
            }
            handleViolations(sender, args);
            return true;
        }
        if (subCommand.equals("reset")) {
            if (!has(sender, "poseidon.admin")) {
                return true;
            }
            handleReset(sender, args);
            return true;
        }
        if (subCommand.equals("profile")) {
            if (!has(sender, "poseidon.admin")) {
                return true;
            }
            handleProfile(sender, args);
            return true;
        }
        if (subCommand.equals("logs")) {
            if (!has(sender, "poseidon.staff")) {
                return true;
            }
            handleLogs(sender, args);
            return true;
        }
        if (subCommand.equals("cloud")) {
            if (!has(sender, "poseidon.staff")) {
                return true;
            }
            handleCloud(sender);
            return true;
        }
        if (subCommand.equals("version")) {
            sender.sendMessage(color("&8[&3Poseidon&8] &7Version &f" + plugin.getDescription().getVersion()
                    + " &7checks=&f" + plugin.getCheckManager().size()
                    + " &7tps=&f" + format(plugin.getTpsTracker().getTps())));
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("alerts")) {
            return args.length == 1 ? filter(Arrays.asList("on", "off"), args[0]) : Collections.<String>emptyList();
        }
        if (args.length == 1) {
            return filter(Arrays.asList("help", "reload", "info", "checks", "check", "toggle", "verbose",
                    "violations", "reset", "profile", "logs", "alerts", "cloud", "version"), args[0]);
        }
        String sub = normalizeSubCommand(args[0]);
        if (args.length == 2 && (sub.equals("check") || sub.equals("toggle"))) {
            return filter(checkNames(), args[1]);
        }
        if (args.length == 2 && (sub.equals("info") || sub.equals("violations") || sub.equals("reset")
                || sub.equals("logs") || sub.equals("verbose"))) {
            return filter(playerNames(), args[1]);
        }
        if (args.length == 2 && sub.equals("profile")) {
            return filter(Arrays.asList("monitor", "staff-test", "production"), args[1]);
        }
        if (args.length == 2 && sub.equals("alerts")) {
            return filter(Arrays.asList("on", "off"), args[1]);
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(color("&8&m----------------&r &3Poseidon &8&m----------------"));
        sender.sendMessage(color("&7/" + label + " help &8- &fShow this menu."));
        sender.sendMessage(color("&7/" + label + " reload &8- &fReload config, checks, and managers."));
        sender.sendMessage(color("&7/" + label + " info [player] &8- &fView runtime or player state."));
        sender.sendMessage(color("&7/" + label + " checks &8- &fList registered checks."));
        sender.sendMessage(color("&7/" + label + " check <check> &8- &fInspect one check."));
        sender.sendMessage(color("&7/" + label + " toggle <check> &8- &fToggle a check and save config."));
        sender.sendMessage(color("&7/" + label + " verbose [player] &8- &fToggle verbose alerts."));
        sender.sendMessage(color("&7/" + label + " violations <player> &8- &fShow violation map."));
        sender.sendMessage(color("&7/" + label + " reset <player> &8- &fClear violations."));
        sender.sendMessage(color("&7/" + label + " profile <monitor|staff-test|production> &8- &fApply profile."));
        sender.sendMessage(color("&7/" + label + " logs <player> &8- &fShow recent matching flag logs."));
        sender.sendMessage(color("&7/" + label + " alerts <on|off> &8- &fSet personal alerts."));
        sender.sendMessage(color("&8&m------------------------------------------"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&8[&3Poseidon&8] &7Checks=&f" + plugin.getCheckManager().size()
                    + " &7players=&f" + dataManager.all().size()
                    + " &7tps=&f" + format(plugin.getTpsTracker().getTps())
                    + " &7packetAvg=&f" + format(plugin.getPacketManager().averagePacketMicros()) + "us"
                    + " &7logQueue=&f" + alertManager.getPendingFlagLogLines()
                    + " &7replayQueue=&f" + plugin.getReplayRecorder().pending()
                    + " &7mode=&f" + plugin.getConfig().getString("mitigations.mode", "monitor")));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cThat player is not online."));
            return;
        }
        PlayerData data = dataManager.getOrCreate(target);
        sender.sendMessage(color("&8&m----------------&r &3Poseidon Debug &8&m----------------"));
        sender.sendMessage(color("&7Player: &f" + target.getName() + " &8(" + target.getUniqueId() + ")"));
        sender.sendMessage(color("&7Transaction ping: &f" + data.getTransactionPing() + "ms &7pending=&f" + data.getPendingTransactionCount()));
        sender.sendMessage(color("&7Latency compensation: &f" + data.isLatencyCompensated()));
        sender.sendMessage(color("&7Last velocity: &f" + data.getLastVelocitySummary()));
        sender.sendMessage(color("&7Position history: &f" + data.getPositionHistorySize() + " samples"));
        sender.sendMessage(color("&7Mitigation: &f" + data.getMitigationLevel().name() + " &7score=&f" + format(data.getMitigationScore())));
        sender.sendMessage(color("&7Model: &fscore=" + format(data.getModelScore()) + " confidence=" + format(data.getModelConfidence())));
        sender.sendMessage(color("&7Cross-instance: &frisk=" + format(data.getCrossInstanceRisk()) + " records=" + data.getCrossInstanceRecords()));
        sender.sendMessage(color("&7World: &f" + data.getLastWorldName() + " &7ground=&f" + data.isOnGround()
                + " &7sprint=&f" + data.isSprinting() + " &7sneak=&f" + data.isSneaking()));
        sender.sendMessage(color("&7Position: &f" + format(data.getX()) + ", " + format(data.getY()) + ", " + format(data.getZ())));
        sender.sendMessage(color("&7Rotation: &f" + format(data.getYaw()) + ", " + format(data.getPitch())));
        sender.sendMessage(color("&7Environment: &fliquid=" + data.isInLiquid() + ",web=" + data.isInWeb()
                + ",ice=" + data.isOnIce() + ",complex=" + data.isNearComplexCollision()));
        sender.sendMessage(color("&8&m------------------------------------------------"));
    }

    private void handleChecks(CommandSender sender) {
        sender.sendMessage(color("&8&m----------------&r &3Poseidon Checks &8&m----------------"));
        for (Check check : plugin.getCheckManager().getChecks()) {
            sender.sendMessage(color((check.isEnabled() ? "&a" : "&c") + check.getName()
                    + " &8- &7" + check.getCategory().name()
                    + " &8/&7 " + check.getSeverity().name()
                    + (check.isExperimental() ? " &8[EXP]" : "")
                    + " &8VL=&f" + format(check.getAlertVl())));
        }
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /poseidon check <check>"));
            return;
        }
        Check check = plugin.getCheckManager().getCheck(args[1]);
        if (check == null) {
            sender.sendMessage(color("&cUnknown check."));
            return;
        }
        sender.sendMessage(color("&8&m----------------&r &3" + check.getName() + " &8&m----------------"));
        sender.sendMessage(color("&7Enabled: &f" + check.isEnabled() + " &7Alert: &f" + check.shouldAlert()
                + " &7Punish: &f" + check.shouldPunish()));
        sender.sendMessage(color("&7Category: &f" + check.getCategory().name() + " &7Severity: &f" + check.getSeverity().name()
                + " &7Experimental: &f" + check.isExperimental()));
        sender.sendMessage(color("&7Alert VL: &f" + format(check.getAlertVl()) + " &7Max VL: &f" + format(check.getMaxVl())
                + " &7Decay: &f" + format(check.getDecay()) + " &7Buffer: &f" + format(check.getBuffer())));
        sender.sendMessage(color("&7Average time: &f" + format(plugin.getCheckManager().averageMicros(check.getName())) + "us"));
        sender.sendMessage(color("&7Description: &f" + check.getDescription()));
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /poseidon toggle <check>"));
            return;
        }
        Check check = plugin.getCheckManager().getCheck(args[1]);
        if (check == null) {
            sender.sendMessage(color("&cUnknown check."));
            return;
        }
        boolean enabled = !check.isEnabled();
        plugin.getCheckManager().setEnabled(check.getName(), enabled);
        sender.sendMessage(color("&8[&3Poseidon&8] &7" + check.getName() + " is now " + (enabled ? "&aenabled" : "&cdisabled") + "&7."));
    }

    private void handleVerbose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use verbose alerts."));
            return;
        }
        Player staff = (Player) sender;
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cThat player is not online."));
                return;
            }
            alertManager.setVerboseTarget(staff, target);
            sender.sendMessage(color("&8[&3Poseidon&8] &7Verbose alerts focused on &f" + target.getName() + "&7."));
            return;
        }
        boolean enabled = alertManager.toggleVerbose(staff);
        sender.sendMessage(color("&8[&3Poseidon&8] &7Verbose alerts " + (enabled ? "&aenabled" : "&cdisabled") + "&7."));
    }

    private void handleViolations(CommandSender sender, String[] args) {
        PlayerData data = targetData(sender, args, "violations");
        if (data == null) {
            return;
        }
        Map<String, Double> violations = data.getViolations();
        if (violations.isEmpty()) {
            sender.sendMessage(color("&8[&3Poseidon&8] &7No violations for &f" + data.getName() + "&7."));
            return;
        }
        sender.sendMessage(color("&8&m----------------&r &3Violations &8&m----------------"));
        for (Map.Entry<String, Double> entry : violations.entrySet()) {
            sender.sendMessage(color("&7" + entry.getKey() + ": &f" + format(entry.getValue().doubleValue())));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        PlayerData data = targetData(sender, args, "reset");
        if (data == null) {
            return;
        }
        data.resetViolations();
        sender.sendMessage(color("&8[&3Poseidon&8] &7Cleared violations for &f" + data.getName() + "&7."));
    }

    private void handleProfile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /poseidon profile <monitor|staff-test|production>"));
            return;
        }
        if (!plugin.applyProfile(args[1])) {
            sender.sendMessage(color("&cUnknown profile."));
            return;
        }
        sender.sendMessage(color("&8[&3Poseidon&8] &7Applied profile &f" + args[1] + "&7."));
    }

    private void handleLogs(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /poseidon logs <player>"));
            return;
        }
        File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("settings.flag-log.file", "flags.jsonl"));
        if (!file.exists()) {
            sender.sendMessage(color("&8[&3Poseidon&8] &7No flag log exists yet."));
            return;
        }
        List<String> matches = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase(java.util.Locale.US).contains(args[1].toLowerCase(java.util.Locale.US))) {
                    matches.add(line);
                    if (matches.size() > 8) {
                        matches.remove(0);
                    }
                }
            }
        } catch (IOException exception) {
            sender.sendMessage(color("&cCould not read logs: " + exception.getMessage()));
            return;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (matches.isEmpty()) {
            sender.sendMessage(color("&8[&3Poseidon&8] &7No recent log lines matched &f" + args[1] + "&7."));
            return;
        }
        sender.sendMessage(color("&8&m----------------&r &3Recent Logs &8&m----------------"));
        for (String match : matches) {
            sender.sendMessage(ChatColor.GRAY + trim(match, 220));
        }
    }

    private void handleCloud(CommandSender sender) {
        sender.sendMessage(color("&8&m----------------&r &3Poseidon Cloud &8&m----------------"));
        sender.sendMessage(color("&7Enabled: &f" + plugin.getCloudSyncManager().isEnabled()));
        sender.sendMessage(color("&7Instance: &f" + plugin.getConfig().getString("cloud.instance-id", "default")));
        sender.sendMessage(color("&7Endpoint configured: &f" + plugin.getCloudSyncManager().hasEndpoint()));
        sender.sendMessage(color("&7Spooled signals: &f" + plugin.getCloudSyncManager().getSpooledSignals()));
        sender.sendMessage(color("&7Sent signals: &f" + plugin.getCloudSyncManager().getSentSignals()));
        sender.sendMessage(color("&7Failed sends: &f" + plugin.getCloudSyncManager().getFailedSignals()));
    }

    private PlayerData targetData(CommandSender sender, String[] args, String usage) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /poseidon " + usage + " <player>"));
            return null;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cThat player is not online."));
            return null;
        }
        return dataManager.getOrCreate(target);
    }

    private void toggleAlerts(CommandSender sender, String value) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can toggle personal alerts."));
            return;
        }
        Player player = (Player) sender;
        boolean enabled;
        if (value == null) {
            enabled = alertManager.toggleAlerts(player);
        } else {
            enabled = !value.equalsIgnoreCase("off") && !value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("disable");
            alertManager.setAlerts(player, enabled);
        }
        player.sendMessage(color("&8[&3Poseidon&8] &7Alerts " + (enabled ? "&aenabled" : "&cdisabled") + "&7."));
    }

    private boolean has(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("poseidon.admin")) {
            return true;
        }
        sender.sendMessage(color("&cYou do not have permission."));
        return false;
    }

    private List<String> checkNames() {
        List<String> names = new ArrayList<String>();
        for (Check check : plugin.getCheckManager().getChecks()) {
            names.add(check.getName());
        }
        return names;
    }

    private static List<String> playerNames() {
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static String normalizeSubCommand(String input) {
        if (input.equalsIgnoreCase("h") || input.equalsIgnoreCase("?") || input.equalsIgnoreCase("help")) {
            return "help";
        }
        if (input.equalsIgnoreCase("a") || input.equalsIgnoreCase("alert") || input.equalsIgnoreCase("alerts")
                || input.equalsIgnoreCase("togglealerts") || input.equalsIgnoreCase("togglealert")) {
            return "alerts";
        }
        if (input.equalsIgnoreCase("i") || input.equalsIgnoreCase("info") || input.equalsIgnoreCase("debug")) {
            return "info";
        }
        if (input.equalsIgnoreCase("rl") || input.equalsIgnoreCase("reload") || input.equalsIgnoreCase("refresh")) {
            return "reload";
        }
        if (input.equalsIgnoreCase("v") || input.equalsIgnoreCase("verbose")) {
            return "verbose";
        }
        if (input.equalsIgnoreCase("c") || input.equalsIgnoreCase("check")) {
            return "check";
        }
        if (input.equalsIgnoreCase("list") || input.equalsIgnoreCase("checks")) {
            return "checks";
        }
        if (input.equalsIgnoreCase("t") || input.equalsIgnoreCase("toggle")) {
            return "toggle";
        }
        if (input.equalsIgnoreCase("violations") || input.equalsIgnoreCase("vl")) {
            return "violations";
        }
        if (input.equalsIgnoreCase("r") || input.equalsIgnoreCase("reset")) {
            return "reset";
        }
        if (input.equalsIgnoreCase("p") || input.equalsIgnoreCase("profile")) {
            return "profile";
        }
        if (input.equalsIgnoreCase("log") || input.equalsIgnoreCase("logs")) {
            return "logs";
        }
        if (input.equalsIgnoreCase("cloud") || input.equalsIgnoreCase("sync")) {
            return "cloud";
        }
        if (input.equalsIgnoreCase("ver") || input.equalsIgnoreCase("version")) {
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

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
