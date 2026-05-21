package dev.codex.poseidon.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncLineWriter {
    private final JavaPlugin plugin;
    private final File file;
    private final long intervalTicks;
    private final int maxLinesPerFlush;
    private final long maxBytes;
    private final int maxBackups;
    private final Queue<String> queue = new ConcurrentLinkedQueue<String>();
    private final AtomicBoolean flushing = new AtomicBoolean();
    private BukkitTask task;

    public AsyncLineWriter(JavaPlugin plugin, File file, long intervalTicks, int maxLinesPerFlush) {
        this(plugin, file, intervalTicks, maxLinesPerFlush, 0L, 0);
    }

    public AsyncLineWriter(JavaPlugin plugin, File file, long intervalTicks, int maxLinesPerFlush, long maxBytes, int maxBackups) {
        this.plugin = plugin;
        this.file = file;
        this.intervalTicks = Math.max(1L, intervalTicks);
        this.maxLinesPerFlush = Math.max(1, maxLinesPerFlush);
        this.maxBytes = Math.max(0L, maxBytes);
        this.maxBackups = Math.max(0, maxBackups);
    }

    public void start() {
        if (task != null) {
            return;
        }
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, intervalTicks, intervalTicks);
    }

    public void enqueue(String line) {
        if (line != null && !line.isEmpty()) {
            queue.add(line);
        }
    }

    public int pending() {
        return queue.size();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        while (!queue.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        if (!flushing.compareAndSet(false, true)) {
            return;
        }

        try {
            List<String> lines = new ArrayList<String>(maxLinesPerFlush);
            for (int i = 0; i < maxLinesPerFlush; i++) {
                String line = queue.poll();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }

            if (lines.isEmpty()) {
                return;
            }

            File folder = file.getParentFile();
            if (folder != null && !folder.exists() && !folder.mkdirs()) {
                return;
            }

            BufferedWriter writer = null;
            try {
                rotateIfNeeded();
                writer = new BufferedWriter(new FileWriter(file, true));
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to write " + file.getName() + ": " + exception.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } finally {
            flushing.set(false);
        }
    }

    private void rotateIfNeeded() {
        if (maxBytes <= 0L || maxBackups <= 0 || !file.exists() || file.length() < maxBytes) {
            return;
        }

        File oldest = backup(maxBackups);
        if (oldest.exists() && !oldest.delete()) {
            plugin.getLogger().warning("Failed to delete old log backup " + oldest.getName());
            return;
        }

        for (int i = maxBackups - 1; i >= 1; i--) {
            File current = backup(i);
            if (current.exists() && !current.renameTo(backup(i + 1))) {
                plugin.getLogger().warning("Failed to rotate log backup " + current.getName());
                return;
            }
        }

        if (!file.renameTo(backup(1))) {
            plugin.getLogger().warning("Failed to rotate log " + file.getName());
        }
    }

    private File backup(int index) {
        File parent = file.getParentFile();
        return parent == null ? new File(file.getName() + "." + index) : new File(parent, file.getName() + "." + index);
    }
}
