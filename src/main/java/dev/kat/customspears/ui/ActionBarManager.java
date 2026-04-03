package dev.kat.customspears.ui;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.managers.CooldownManager;
import dev.kat.customspears.managers.SuppressManager;
import dev.kat.customspears.spears.CustomSpear;
import dev.kat.customspears.spears.SpearType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

public class ActionBarManager {

    private final CustomSpears plugin;
    private BukkitTask task;

    public ActionBarManager(CustomSpears plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        int interval = plugin.getConfig().getInt("ui.cooldown-bar-update-ticks", 2);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack held = player.getInventory().getItemInMainHand();
            SpearType type = CustomSpear.getSpearType(held);
            if (type == null) continue;

            UUID uuid = player.getUniqueId();
            CooldownManager cd = plugin.getCooldownManager();
            SuppressManager sm = plugin.getSuppressManager();

            String color = plugin.getConfig().getString("ui.cooldown-bar-color", "&e");
            boolean suppressed = sm.isSuppressed(uuid);

            StringBuilder bar = new StringBuilder();

            if (suppressed) {
                bar.append("&c&lSUPPRESSED &8| &7Spear abilities disabled");
            } else {
                Map<String, Long> cooldowns = cd.getAllCooldowns(uuid);

                if (cooldowns.isEmpty()) {
                    bar.append(color).append("✦ All abilities ready");
                } else {
                    bar.append(color).append("✦ ");
                    boolean first = true;
                    for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
                        if (!first) bar.append(" &8| ").append(color);
                        String abilityLabel = formatAbilityLabel(entry.getKey());
                        bar.append(abilityLabel).append(" &7").append(entry.getValue()).append("s");
                        first = false;
                    }
                }
            }

            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(bar.toString()));
        }
    }

    private String formatAbilityLabel(String ability) {
        // Capitalise and clean up ability key for display
        // e.g. "frostbite" -> "Frostbite", "curse:POISON" -> "Curse(Poison)"
        if (ability.startsWith("curse:")) {
            String effect = ability.substring(6);
            effect = effect.charAt(0) + effect.substring(1).toLowerCase();
            return "Curse(" + effect + ")";
        }
        if (ability.contains("_")) {
            String[] parts = ability.split("_");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1)).append(" ");
            return sb.toString().trim();
        }
        return ability.substring(0, 1).toUpperCase() + ability.substring(1);
    }
}
