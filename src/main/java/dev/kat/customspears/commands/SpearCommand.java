package dev.kat.customspears.commands;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.spears.CustomSpear;
import dev.kat.customspears.spears.SpearRegistry;
import dev.kat.customspears.spears.SpearType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SpearCommand implements CommandExecutor, TabCompleter {

    private final CustomSpears plugin;

    public SpearCommand(CustomSpears plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "recipe" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this command.");
                    return true;
                }
                if (args.length < 2) { player.sendMessage("§cUsage: /spears recipe <spear>"); return true; }
                CustomSpear spear = plugin.getSpearRegistry().getByName(args[1]);
                if (spear == null) { player.sendMessage("§cUnknown spear: " + args[1]); return true; }
                openRecipeGui(player, spear);
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /spears info <spear>"); return true; }
                CustomSpear spear = plugin.getSpearRegistry().getByName(args[1]);
                if (spear == null) { sender.sendMessage("§cUnknown spear: " + args[1]); return true; }
                sendInfo(sender, spear);
            }
            case "give" -> {
                if (!sender.hasPermission("customspears.give")) {
                    sender.sendMessage("§cYou don't have permission to do that.");
                    return true;
                }
                if (args.length < 2) { sender.sendMessage("§cUsage: /spears give <spear> [player]"); return true; }
                CustomSpear spear = plugin.getSpearRegistry().getByName(args[1]);
                if (spear == null) { sender.sendMessage("§cUnknown spear: " + args[1]); return true; }
                Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : (sender instanceof Player p ? p : null);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                target.getInventory().addItem(spear.buildItem());
                sender.sendMessage("§aGave " + spear.getDisplayName() + " §ato §e" + target.getName());
                target.sendMessage("§aYou received: " + spear.getDisplayName());
            }
            case "list" -> {
                sender.sendMessage("§e§lCustom Spears:");
                for (CustomSpear spear : plugin.getSpearRegistry().getAll()) {
                    sender.sendMessage("  §8- §r" + ChatColor.translateAlternateColorCodes('&', spear.getDisplayName())
                            + " §8(" + spear.getType().name().toLowerCase() + ")");
                }
            }
            case "reload" -> {
                if (!sender.hasPermission("customspears.reload")) {
                    sender.sendMessage("§cYou don't have permission.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getSpearRegistry().reload();
                sender.sendMessage("§aCustomSpears config reloaded.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§lCustomSpears Commands:");
        sender.sendMessage("  §e/spears list §8— List all custom spears");
        sender.sendMessage("  §e/spears recipe <spear> §8— View crafting recipe");
        sender.sendMessage("  §e/spears info <spear> §8— View detailed ability info");
        sender.sendMessage("  §e/spears give <spear> [player] §8— Give a spear (op)");
        sender.sendMessage("  §e/spears reload §8— Reload config (op)");
    }

    private void sendInfo(CommandSender sender, CustomSpear spear) {
        sender.sendMessage("§8§m                         ");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', spear.getDisplayName()));
        sender.sendMessage("§8Custom Model Data: §7" + spear.getCustomModelData());
        sender.sendMessage("§8Abilities:");
        for (String line : spear.getLore()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  " + line));
        }
        sender.sendMessage("§8§m                         ");
    }

    /**
     * Opens a chest GUI displaying the 3x3 crafting recipe visually.
     * Layout: slots 10-16 represent the crafting grid, slot 25 is the result.
     */
    private void openRecipeGui(Player player, CustomSpear spear) {
        Inventory gui = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&8Recipe: " + spear.getDisplayName()));

        // Fill background
        ItemStack bg = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.displayName(Component.text(" "));
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 54; i++) gui.setItem(i, bg.clone());

        // Place crafting grid (3x3) at slots 10, 11, 12, 19, 20, 21, 28, 29, 30
        List<ItemStack[]> recipes = spear.getRecipes();
        if (!recipes.isEmpty()) {
            ItemStack[] grid = recipes.get(0);
            int[] guiSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            for (int i = 0; i < 9; i++) {
                if (i < grid.length && grid[i] != null) {
                    gui.setItem(guiSlots[i], grid[i].clone());
                }
            }
        }

        // Arrow indicator at slot 23
        ItemStack arrow = new ItemStack(org.bukkit.Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.displayName(Component.text("§7➜"));
        arrow.setItemMeta(arrowMeta);
        gui.setItem(23, arrow);

        // Result at slot 25
        gui.setItem(25, spear.buildItem());

        // Info label at slot 49
        ItemStack info = new ItemStack(org.bukkit.Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&eTexture: &7custom_model_data = " + spear.getCustomModelData()));
        List<Component> infoLore = List.of(
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&7Add this model data in your resource pack"),
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&7to assign a custom texture to this spear.")
        );
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(49, info);

        player.openInventory(gui);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("recipe", "info", "give", "list", "reload");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("list")) {
            List<String> names = new ArrayList<>();
            for (CustomSpear spear : plugin.getSpearRegistry().getAll()) {
                names.add(spear.getConfigKey());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }
        return List.of();
    }
}
