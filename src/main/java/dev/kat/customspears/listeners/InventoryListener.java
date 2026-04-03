package dev.kat.customspears.listeners;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.spears.CustomSpear;
import dev.kat.customspears.spears.SpearType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final CustomSpears plugin;

    public InventoryListener(CustomSpears plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent picking up a custom spear if the player already has one.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("general.one-spear-per-inventory", true)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();
        if (!CustomSpear.isCustomSpear(item.getItemStack())) return;

        SpearType pickedType = CustomSpear.getSpearType(item.getItemStack());

        if (hasCustomSpear(player)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§cYou can only carry one custom spear at a time!"));
        }
    }

    /**
     * Prevent moving a second custom spear into inventory via clicking in any inventory screen.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("general.one-spear-per-inventory", true)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Check if the item being moved is a custom spear
        ItemStack moving = null;
        if (cursor != null && CustomSpear.isCustomSpear(cursor)) moving = cursor;
        else if (current != null && CustomSpear.isCustomSpear(current) && event.isShiftClick()) moving = current;

        if (moving == null) return;

        // Count existing custom spears in player inventory
        int count = countCustomSpears(player);

        // If they already have one and this would add another, cancel
        if (count >= 1) {
            // Allow moving the same spear around (not adding a new one)
            // We check: if the spear in cursor is not already in their inventory, block it
            SpearType movingType = CustomSpear.getSpearType(moving);
            SpearType existingType = getFirstCustomSpearType(player);

            if (existingType != null && existingType != movingType) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("§cYou can only carry one custom spear at a time!"));
            } else if (existingType != null && existingType == movingType && count >= 1 && !isFromPlayerInventory(event, player)) {
                event.setCancelled(true);
                player.sendActionBar(Component.text("§cYou can only carry one custom spear at a time!"));
            }
        }
    }

    private boolean isFromPlayerInventory(InventoryClickEvent event, Player player) {
        return event.getClickedInventory() != null &&
                event.getClickedInventory().equals(player.getInventory());
    }

    private boolean hasCustomSpear(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CustomSpear.isCustomSpear(item)) return true;
        }
        return false;
    }

    private int countCustomSpears(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CustomSpear.isCustomSpear(item)) count++;
        }
        return count;
    }

    private SpearType getFirstCustomSpearType(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CustomSpear.isCustomSpear(item)) {
                return CustomSpear.getSpearType(item);
            }
        }
        return null;
    }
}
