package dev.kat.customspears.managers;

import dev.kat.customspears.CustomSpears;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuppressManager {

    private final CustomSpears plugin;
    // suppressed player UUID -> task that will unsuppress them
    private final Map<UUID, BukkitTask> suppressed = new HashMap<>();

    public SuppressManager(CustomSpears plugin) {
        this.plugin = plugin;
    }

    public void suppress(UUID target, int durationSeconds) {
        // Cancel existing suppress if re-applied
        if (suppressed.containsKey(target)) {
            suppressed.get(target).cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            suppressed.remove(target);
            Player p = Bukkit.getPlayer(target);
            if (p != null) {
                p.sendActionBar(net.kyori.adventure.text.Component.text(
                        "§aYour spear abilities have been restored!"));
            }
        }, durationSeconds * 20L);

        suppressed.put(target, task);

        Player p = Bukkit.getPlayer(target);
        if (p != null) {
            p.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§cYour spear has been suppressed for " + durationSeconds + "s!"));
        }
    }

    public boolean isSuppressed(UUID player) {
        return suppressed.containsKey(player);
    }

    public void unsuppress(UUID player) {
        BukkitTask task = suppressed.remove(player);
        if (task != null) task.cancel();
    }

    public void clearAll() {
        suppressed.values().forEach(BukkitTask::cancel);
        suppressed.clear();
    }

    public long getRemainingTicks(UUID player) {
        // Approximate — we store tasks not expiry times, so return -1 if suppressed
        return suppressed.containsKey(player) ? -1 : 0;
    }
}
