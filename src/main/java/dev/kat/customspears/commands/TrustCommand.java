package dev.kat.customspears.commands;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.managers.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TrustCommand implements CommandExecutor, TabCompleter {

    private final CustomSpears plugin;

    public TrustCommand(CustomSpears plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        TrustManager tm = plugin.getTrustManager();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /trust add <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { player.sendMessage("§cPlayer not found or offline."); return true; }
                if (target.equals(player)) { player.sendMessage("§cYou can't trust yourself."); return true; }
                tm.trust(player.getUniqueId(), target.getUniqueId());
                player.sendMessage("§a" + target.getName() + " §ais now trusted — they are immune to your spear abilities.");
                target.sendMessage("§e" + player.getName() + " §7has trusted you. You are immune to their spear abilities.");
            }
            case "remove" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /trust remove <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                UUID targetUUID = target != null ? target.getUniqueId() : null;

                // Allow removing by name even if offline
                if (targetUUID == null) {
                    player.sendMessage("§cPlayer not found. They must be online to be removed.");
                    return true;
                }

                tm.untrust(player.getUniqueId(), targetUUID);
                player.sendMessage("§e" + target.getName() + " §7is no longer trusted.");
                if (target.isOnline()) {
                    target.sendMessage("§e" + player.getName() + " §7has revoked your trust.");
                }
            }
            case "list" -> {
                Set<UUID> trusted = tm.getTrusted(player.getUniqueId());
                if (trusted.isEmpty()) {
                    player.sendMessage("§7You have no trusted players.");
                    return true;
                }
                player.sendMessage("§e§lTrusted players:");
                for (UUID uuid : trusted) {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = p != null ? p.getName() : uuid.toString();
                    String status = p != null ? "§a(online)" : "§8(offline)";
                    player.sendMessage("  §8- §7" + name + " " + status);
                }
            }
            case "clear" -> {
                tm.clear(player.getUniqueId());
                player.sendMessage("§7Your trust list has been cleared.");
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e§lTrust Commands:");
        player.sendMessage("  §e/trust add <player> §8— Make a player immune to your spear abilities");
        player.sendMessage("  §e/trust remove <player> §8— Remove trust from a player");
        player.sendMessage("  §e/trust list §8— View your trusted players");
        player.sendMessage("  §e/trust clear §8— Clear all trusted players");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("add", "remove", "list", "clear");
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (sender instanceof Player pl && !p.equals(pl)) names.add(p.getName());
            });
            return names;
        }
        return List.of();
    }
}
