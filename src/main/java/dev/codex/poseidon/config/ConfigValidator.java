package dev.codex.poseidon.config;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.check.Check;
import dev.codex.poseidon.check.CheckManager;

public final class ConfigValidator {
    private static final String[] REQUIRED_CHECK_KEYS = {
            "enabled", "alert", "punish", "alert-vl", "max-vl", "decay",
            "buffer", "setback", "mitigation", "experimental", "description",
            "category", "severity"
    };

    private ConfigValidator() {
    }

    public static int validate(PoseidonPlugin plugin, CheckManager checkManager) {
        int warnings = 0;
        for (Check check : checkManager.getChecks()) {
            String base = "checks." + check.getName() + ".";
            for (String key : REQUIRED_CHECK_KEYS) {
                if (!plugin.getConfig().contains(base + key)) {
                    plugin.getLogger().warning("Missing config key " + base + key);
                    warnings++;
                }
            }
        }
        return warnings;
    }
}
