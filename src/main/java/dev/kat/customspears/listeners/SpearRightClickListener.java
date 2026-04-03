package dev.kat.customspears.listeners;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.managers.CooldownManager;
import dev.kat.customspears.managers.SuppressManager;
import dev.kat.customspears.managers.TrustManager;
import dev.kat.customspears.spears.CustomSpear;
import dev.kat.customspears.spears.SpearType;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class SpearRightClickListener implements Listener {

    private final CustomSpears plugin;
    // Recall marks: player UUID -> marked location
    private final Map<UUID, Location> recallMarks = new HashMap<>();

    public SpearRightClickListener(CustomSpears plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        SpearType type = CustomSpear.getSpearType(held);
        if (type == null) return;

        UUID uuid = player.getUniqueId();
        SuppressManager sm = plugin.getSuppressManager();
        CooldownManager cd = plugin.getCooldownManager();
        FileConfiguration cfg = plugin.getConfig();

        if (sm.isSuppressed(uuid)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§cYour spear is suppressed!"));
            return;
        }

        switch (type) {
            case NIGHTMARE -> handleRecall(player, cfg, cd);
            case SANGUINE -> handleSuppress(player, cfg, cd);
            case THUNDERSPEAR -> handleVault(player, cfg, cd);
            case ASHEN -> handleGroundSlam(player, cfg, cd);
            case PHANTOM -> handlePhantomDash(player, cfg, cd);
            case FORTUNE -> handleBoon(player, cfg, cd);
        }
    }

    // ─── Recall ───────────────────────────────────────────────────────────────

    private void handleRecall(Player player, FileConfiguration cfg, CooldownManager cd) {
        UUID uuid = player.getUniqueId();
        int cooldown = cfg.getInt("spears.nightmare.recall-cooldown", 20);
        int expiry = cfg.getInt("spears.nightmare.recall-mark-expiry", 300);

        if (recallMarks.containsKey(uuid)) {
            // Second right-click: teleport back
            Location mark = recallMarks.remove(uuid);
            player.teleport(mark);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 1, 0.5, 0.3);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            player.sendActionBar(net.kyori.adventure.text.Component.text("§5Recalled!"));
            cd.set(uuid, "recall", cooldown);
        } else {
            // First right-click: set mark
            if (cd.isOnCooldown(uuid, "recall")) {
                long rem = cd.getRemaining(uuid, "recall");
                player.sendActionBar(net.kyori.adventure.text.Component.text("§cRecall on cooldown: §e" + rem + "s"));
                return;
            }
            recallMarks.put(uuid, player.getLocation().clone());
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 15, 0.3, 0.5, 0.3, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            player.sendActionBar(net.kyori.adventure.text.Component.text("§5Mark set! Right-click again to recall."));

            // Expire the mark after configured time
            UUID id = uuid;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (recallMarks.containsKey(id)) {
                    recallMarks.remove(id);
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) p.sendActionBar(net.kyori.adventure.text.Component.text("§7Recall mark expired."));
                }
            }, expiry * 20L);
        }
    }

    // ─── Suppress ────────────────────────────────────────────────────────────

    private void handleSuppress(Player player, FileConfiguration cfg, CooldownManager cd) {
        UUID uuid = player.getUniqueId();
        String key = "suppress";
        if (cd.isOnCooldown(uuid, key)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§cSuppress on cooldown: §e" + cd.getRemaining(uuid, key) + "s"));
            return;
        }

        // Find target player in crosshair (raytrace)
        Player target = getTargetPlayer(player, 5.0);
        if (target == null) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§cNo target in range!"));
            return;
        }

        TrustManager tm = plugin.getTrustManager();
        if (tm.isTrusted(uuid, target.getUniqueId())) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§aThis player is trusted — ability blocked."));
            return;
        }

        int duration = cfg.getInt("spears.sanguine.suppress-duration", 30);
        int cooldown = cfg.getInt("spears.sanguine.suppress-cooldown", 25);

        // Disable jumping via walk speed trick isn't reliable, use a repeating task instead
        plugin.getSuppressManager().suppress(target.getUniqueId(), duration);

        // Prevent jumping by applying jump boost -1 (cancels jumps effectively)
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration * 20, 128, false, false, false));

        player.sendActionBar(net.kyori.adventure.text.Component.text("§c" + target.getName() + "'s spear has been suppressed!"));
        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.5);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1f, 0.8f);

        cd.set(uuid, key, cooldown);
    }

    // ─── Vault ───────────────────────────────────────────────────────────────

    private void handleVault(Player player, FileConfiguration cfg, CooldownManager cd) {
        UUID uuid = player.getUniqueId();
        String key = "vault";
        if (cd.isOnCooldown(uuid, key)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§eVault on cooldown: §e" + cd.getRemaining(uuid, key) + "s"));
            return;
        }

        double fwd = cfg.getDouble("spears.thunderspear.vault-forward-power", 1.4);
        double up = cfg.getDouble("spears.thunderspear.vault-upward-power", 0.9);
        int cooldown = cfg.getInt("spears.thunderspear.vault-cooldown", 12);

        Vector dir = player.getLocation().getDirection().normalize();
        dir.setY(0).normalize().multiply(fwd).setY(up);
        player.setVelocity(dir);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.3, 0.1, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.3f);

        cd.set(uuid, key, cooldown);
    }

    // ─── Ground Slam ─────────────────────────────────────────────────────────

    private void handleGroundSlam(Player player, FileConfiguration cfg, CooldownManager cd) {
        UUID uuid = player.getUniqueId();
        String key = "ground_slam";
        if (cd.isOnCooldown(uuid, key)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§8Ground Slam on cooldown: §e" + cd.getRemaining(uuid, key) + "s"));
            return;
        }

        double radius = cfg.getDouble("spears.ashen.ground-slam-radius", 6.0);
        double knockback = cfg.getDouble("spears.ashen.ground-slam-knockback", 1.5);
        int cooldown = cfg.getInt("spears.ashen.ground-slam-cooldown", 15);
        TrustManager tm = plugin.getTrustManager();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) continue;
            if (entity instanceof Player tp && tm.isTrusted(uuid, tp.getUniqueId())) continue;

            Vector direction = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            direction.setY(0.4).multiply(knockback);
            entity.setVelocity(direction);
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3, 0.5, 0.1, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 50, 1, 0.1, 1, 0.3,
                player.getLocation().getBlock().getType() == Material.AIR
                        ? Material.DIRT.createBlockData()
                        : player.getLocation().getBlock().getBlockData());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);

        cd.set(uuid, key, cooldown);
    }

    // ─── Phantom Dash ────────────────────────────────────────────────────────

    private void handlePhantomDash(Player player, FileConfiguration cfg, CooldownManager cd) {
        UUID uuid = player.getUniqueId();
        String key = "phantom_dash";
        if (cd.isOnCooldown(uuid, key)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§fPhantom Dash on cooldown: §e" + cd.getRemaining(uuid, key) + "s"));
            return;
        }

        int speedLevel = cfg.getInt("spears.phantom.phantom-dash-speed-level", 2);
        int speedDur = cfg.getInt("spears.phantom.phantom-dash-speed-duration", 40);
        int invisTicks = cfg.getInt("spears.phantom.phantom-dash-invisibility-ticks", 60);
        int cooldown = cfg.getInt("spears.phantom.phantom-dash-cooldown", 14);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDur, speedLevel, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, invisTicks, 0, false, false, false));

        // Hide armor/item during dash for all nearby players
        for (Player nearby : player.getWorld().getPlayers()) {
            if (!nearby.equals(player) && nearby.canSee(player)) {
                nearby.hidePlayer(plugin, player);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player nearby : player.getWorld().getPlayers()) {
                if (!nearby.equals(player)) {
                    nearby.showPlayer(plugin, player);
                }
            }
        }, invisTicks);

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 20, 0.3, 0.8, 0.3, 0.2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);

        cd.set(uuid, key, cooldown);
    }

    // ─── Boon ────────────────────────────────────────────────────────────────

    private void handleBoon(Player player, FileConfiguration cfg, CooldownManager cd) {
        UUID uuid = player.getUniqueId();
        String key = "boon";
        if (cd.isOnCooldown(uuid, key)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§6Boon on cooldown: §e" + cd.getRemaining(uuid, key) + "s"));
            return;
        }

        List<String> effectNames = cfg.getStringList("spears.fortune.positive-effects");
        int duration = cfg.getInt("spears.fortune.boon-duration", 30) * 20;
        int cooldown = cfg.getInt("spears.fortune.boon-cooldown", 45);

        if (effectNames.isEmpty()) return;

        String chosenName = effectNames.get(new Random().nextInt(effectNames.size()));
        PotionEffectType pet;
        try {
            pet = PotionEffectType.getByName(chosenName);
        } catch (Exception e) {
            return;
        }
        if (pet == null) return;

        player.addPotionEffect(new PotionEffect(pet, duration, 1, false, true, true));
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§6Boon: §e" + formatEffect(chosenName) + " §6granted!"));

        cd.set(uuid, key, cooldown);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Player getTargetPlayer(Player player, double range) {
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;
            // Simple proximity check — can be upgraded to raytrace
            return target;
        }
        return null;
    }

    private String formatEffect(String name) {
        return name.charAt(0) + name.substring(1).toLowerCase().replace("_", " ");
    }
}
