package dev.codex.poseidon.check;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.packet.PacketContext;

public abstract class Check {
    private final String name;
    private final CheckSettings settings;

    protected Check(String name, boolean enabled, double alertVl) {
        this(name, enabled, alertVl, CheckCategory.EXPERIMENTAL, CheckSeverity.MEDIUM, true, "Legacy Poseidon check.");
    }

    protected Check(String name,
                    boolean enabled,
                    double alertVl,
                    CheckCategory category,
                    CheckSeverity severity,
                    boolean experimental,
                    String description) {
        this.name = name;
        this.settings = CheckSettings.fixed(enabled, alertVl, category, severity, experimental, description);
    }

    protected Check(String name,
                    PoseidonPlugin plugin,
                    boolean defaultEnabled,
                    double defaultAlertVl,
                    CheckCategory defaultCategory,
                    CheckSeverity defaultSeverity,
                    boolean defaultExperimental,
                    String defaultDescription) {
        this.name = name;
        this.settings = CheckSettings.load(plugin, name, defaultEnabled, defaultAlertVl,
                defaultCategory, defaultSeverity, defaultExperimental, defaultDescription);
    }

    public abstract void handle(PacketContext context);

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return settings.isEnabled();
    }

    public double getAlertVl() {
        return settings.getAlertVl();
    }

    public CheckSettings getSettings() {
        return settings;
    }

    public boolean shouldAlert() {
        return settings.isAlert();
    }

    public boolean shouldPunish() {
        return settings.isPunish();
    }

    public double getMaxVl() {
        return settings.getMaxVl();
    }

    public double getDecay() {
        return settings.getDecay();
    }

    public double getBuffer() {
        return settings.getBuffer();
    }

    public CheckCategory getCategory() {
        return settings.getCategory();
    }

    public CheckSeverity getSeverity() {
        return settings.getSeverity();
    }

    public boolean isExperimental() {
        return settings.isExperimental();
    }

    public String getDescription() {
        return settings.getDescription();
    }

}
