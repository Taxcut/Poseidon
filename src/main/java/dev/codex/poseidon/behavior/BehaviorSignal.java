package dev.codex.poseidon.behavior;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class BehaviorSignal {
    private final UUID playerUuid;
    private final String playerName;
    private final String checkName;
    private final SignalCategory category;
    private final double violation;
    private final double amount;
    private final String detail;
    private final long timestamp;
    private final long transactionPing;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String instanceId;

    private BehaviorSignal(Builder builder) {
        this.playerUuid = builder.playerUuid;
        this.playerName = builder.playerName;
        this.checkName = builder.checkName;
        this.category = builder.category;
        this.violation = builder.violation;
        this.amount = builder.amount;
        this.detail = builder.detail;
        this.timestamp = builder.timestamp;
        this.transactionPing = builder.transactionPing;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.yaw = builder.yaw;
        this.pitch = builder.pitch;
        this.instanceId = builder.instanceId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCheckName() {
        return checkName;
    }

    public SignalCategory getCategory() {
        return category;
    }

    public double getViolation() {
        return violation;
    }

    public double getAmount() {
        return amount;
    }

    public String getDetail() {
        return detail;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getTransactionPing() {
        return transactionPing;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String toJson() {
        return "{"
                + "\"time\":" + timestamp + ","
                + "\"instance\":\"" + escape(instanceId) + "\","
                + "\"player\":\"" + escape(playerName) + "\","
                + "\"uuid\":\"" + playerUuid + "\","
                + "\"check\":\"" + escape(checkName) + "\","
                + "\"category\":\"" + category.name() + "\","
                + "\"amount\":" + jsonNumber(amount) + ","
                + "\"vl\":" + jsonNumber(violation) + ","
                + "\"detail\":\"" + escape(detail) + "\","
                + "\"transactionPing\":" + transactionPing + ","
                + "\"x\":" + jsonNumber(x) + ","
                + "\"y\":" + jsonNumber(y) + ","
                + "\"z\":" + jsonNumber(z) + ","
                + "\"yaw\":" + jsonNumber(yaw) + ","
                + "\"pitch\":" + jsonNumber(pitch)
                + "}";
    }

    public static Builder builder(Player player) {
        return new Builder(player.getUniqueId(), player.getName());
    }

    private static String jsonNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.format(Locale.US, "%.4f", value);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                builder.append('\\').append(c);
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else if (c == '\t') {
                builder.append("\\t");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static final class Builder {
        private final UUID playerUuid;
        private final String playerName;
        private String checkName;
        private SignalCategory category = SignalCategory.UNKNOWN;
        private double violation;
        private double amount;
        private String detail = "";
        private long timestamp;
        private long transactionPing;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private String instanceId = "default";

        private Builder(UUID playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
        }

        public Builder checkName(String checkName) {
            this.checkName = checkName;
            this.category = SignalCategory.fromCheckName(checkName);
            return this;
        }

        public Builder violation(double violation) {
            this.violation = violation;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder transactionPing(long transactionPing) {
            this.transactionPing = transactionPing;
            return this;
        }

        public Builder position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public Builder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public BehaviorSignal build() {
            return new BehaviorSignal(this);
        }
    }
}
