package dev.codex.poseidon.check;

import dev.codex.poseidon.PoseidonPlugin;

public final class CheckSettings {
    private final boolean enabled;
    private final boolean alert;
    private final boolean punish;
    private final double alertVl;
    private final double maxVl;
    private final double decay;
    private final double buffer;
    private final boolean setback;
    private final String mitigation;
    private final boolean experimental;
    private final String description;
    private final CheckCategory category;
    private final CheckSeverity severity;

    private CheckSettings(boolean enabled,
                          boolean alert,
                          boolean punish,
                          double alertVl,
                          double maxVl,
                          double decay,
                          double buffer,
                          boolean setback,
                          String mitigation,
                          boolean experimental,
                          String description,
                          CheckCategory category,
                          CheckSeverity severity) {
        this.enabled = enabled;
        this.alert = alert;
        this.punish = punish;
        this.alertVl = alertVl;
        this.maxVl = maxVl;
        this.decay = decay;
        this.buffer = buffer;
        this.setback = setback;
        this.mitigation = mitigation;
        this.experimental = experimental;
        this.description = description;
        this.category = category;
        this.severity = severity;
    }

    public static CheckSettings load(PoseidonPlugin plugin,
                                     String name,
                                     boolean defaultEnabled,
                                     double defaultAlertVl,
                                     CheckCategory defaultCategory,
                                     CheckSeverity defaultSeverity,
                                     boolean defaultExperimental,
                                     String defaultDescription) {
        String path = "checks." + name + ".";
        CheckCategory category = parseCategory(
                plugin.getConfig().getString(path + "category", defaultCategory.name()),
                defaultCategory);
        boolean experimental = plugin.getConfig().getBoolean(path + "experimental", defaultExperimental);
        if (experimental && category != CheckCategory.EXPERIMENTAL
                && plugin.getConfig().getString(path + "category", null) == null) {
            category = CheckCategory.EXPERIMENTAL;
        }

        return new CheckSettings(
                plugin.getConfig().getBoolean(path + "enabled", defaultEnabled),
                plugin.getConfig().getBoolean(path + "alert", true),
                plugin.getConfig().getBoolean(path + "punish", false),
                plugin.getConfig().getDouble(path + "alert-vl", defaultAlertVl),
                plugin.getConfig().getDouble(path + "max-vl", 100.0D),
                plugin.getConfig().getDouble(path + "decay", 0.15D),
                plugin.getConfig().getDouble(path + "buffer", 0.0D),
                plugin.getConfig().getBoolean(path + "setback", false),
                plugin.getConfig().getString(path + "mitigation", "none"),
                experimental,
                plugin.getConfig().getString(path + "description", defaultDescription),
                category,
                parseSeverity(plugin.getConfig().getString(path + "severity", defaultSeverity.name()), defaultSeverity));
    }

    public static CheckSettings fixed(boolean enabled,
                                      double alertVl,
                                      CheckCategory category,
                                      CheckSeverity severity,
                                      boolean experimental,
                                      String description) {
        return new CheckSettings(enabled, true, false, alertVl, 100.0D, 0.15D, 0.0D, false,
                "none", experimental, description, category, severity);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAlert() {
        return alert;
    }

    public boolean isPunish() {
        return punish;
    }

    public double getAlertVl() {
        return alertVl;
    }

    public double getMaxVl() {
        return maxVl;
    }

    public double getDecay() {
        return decay;
    }

    public double getBuffer() {
        return buffer;
    }

    public boolean isSetback() {
        return setback;
    }

    public String getMitigation() {
        return mitigation;
    }

    public boolean isExperimental() {
        return experimental;
    }

    public String getDescription() {
        return description;
    }

    public CheckCategory getCategory() {
        return category;
    }

    public CheckSeverity getSeverity() {
        return severity;
    }

    private static CheckCategory parseCategory(String value, CheckCategory fallback) {
        try {
            return CheckCategory.valueOf(value.toUpperCase(java.util.Locale.US));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static CheckSeverity parseSeverity(String value, CheckSeverity fallback) {
        try {
            return CheckSeverity.valueOf(value.toUpperCase(java.util.Locale.US));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
