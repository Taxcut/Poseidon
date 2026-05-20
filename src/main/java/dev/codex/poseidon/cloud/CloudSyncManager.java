package dev.codex.poseidon.cloud;

import dev.codex.poseidon.PoseidonPlugin;
import dev.codex.poseidon.behavior.BehaviorSignal;
import dev.codex.poseidon.behavior.BehaviorSignalListener;
import dev.codex.poseidon.util.AsyncLineWriter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class CloudSyncManager implements BehaviorSignalListener {
    private final PoseidonPlugin plugin;
    private final AsyncLineWriter spoolWriter;
    private final Queue<BehaviorSignal> postQueue = new ConcurrentLinkedQueue<BehaviorSignal>();
    private final AtomicLong spooledSignals = new AtomicLong();
    private final AtomicLong sentSignals = new AtomicLong();
    private final AtomicLong failedSignals = new AtomicLong();
    private BukkitTask postTask;

    public CloudSyncManager(PoseidonPlugin plugin) {
        this.plugin = plugin;
        this.spoolWriter = new AsyncLineWriter(plugin,
                new File(plugin.getDataFolder(), plugin.getConfig().getString("cloud.spool-file", "cloud-signals.jsonl")),
                plugin.getConfig().getLong("cloud.flush-interval-ticks", 40L),
                plugin.getConfig().getInt("cloud.max-lines-per-flush", 512));
    }

    public void start() {
        if (!isEnabled()) {
            return;
        }
        spoolWriter.start();
        postTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                drainPostQueue();
            }
        }, 20L, Math.max(20L, plugin.getConfig().getLong("cloud.flush-interval-ticks", 40L)));
    }

    @Override
    public void onSignal(BehaviorSignal signal) {
        if (!isEnabled()) {
            return;
        }

        spoolWriter.enqueue(signal.toJson());
        spooledSignals.incrementAndGet();
        if (hasEndpoint()) {
            postQueue.add(signal);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("cloud.enabled", false);
    }

    public boolean hasEndpoint() {
        String endpoint = plugin.getConfig().getString("cloud.endpoint-url", "");
        return endpoint != null && !endpoint.trim().isEmpty();
    }

    public long getSpooledSignals() {
        return spooledSignals.get();
    }

    public long getSentSignals() {
        return sentSignals.get();
    }

    public long getFailedSignals() {
        return failedSignals.get();
    }

    public void stop() {
        if (postTask != null) {
            postTask.cancel();
            postTask = null;
        }
        spoolWriter.stop();
    }

    private void drainPostQueue() {
        if (!isEnabled() || !hasEndpoint()) {
            postQueue.clear();
            return;
        }

        int maxPosts = Math.max(1, plugin.getConfig().getInt("cloud.max-posts-per-flush", 2));
        for (int i = 0; i < maxPosts; i++) {
            BehaviorSignal signal = postQueue.poll();
            if (signal == null) {
                return;
            }
            post(signal);
        }
    }

    private void post(BehaviorSignal signal) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(plugin.getConfig().getString("cloud.endpoint-url", ""));
            connection = (HttpURLConnection) url.openConnection();
            int timeout = plugin.getConfig().getInt("cloud.timeout-ms", 1500);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("User-Agent", "Poseidon/" + plugin.getDescription().getVersion());

            String apiKey = plugin.getConfig().getString("cloud.api-key", "");
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            byte[] body = signal.toJson().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(body);
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                sentSignals.incrementAndGet();
            } else {
                failedSignals.incrementAndGet();
            }
        } catch (IOException exception) {
            failedSignals.incrementAndGet();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
