package dev.kat.customspears.listeners;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.managers.CooldownManager;
import dev.kat.customspears.managers.TrustManager;
import dev.kat.customspears.spears.CustomSpear;
import dev.kat.customspears.spears.SpearType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PassiveListener implements Listener {

    private final CustomSpears plugin;
    // Track active wraith forms to avoid double triggering
    private final Set<UUID> wraith = new HashSet<>();
    // Track bloodthirst stacks: player UUID -> bonus max HP added (to remove later)
    private final Map<UUID, Double> bloodthirstBonus = new HashMap<>();
    // Track held item for passive effect cleanup
    private final Map<UUID, SpearType> lastHeld = new HashMap<>();

    public PassiveListener(CustomSpears plugin) {
        this.plugin = plugin;
    }

    // ─── Warding (Nightmare) — reduce incoming damage ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageReceived(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        SpearType type = CustomSpear.getSpearType(held);
        if (type == null) return;

        FileConfiguration cfg = plugin.getConfig();

        // Warding passive (Nightmare)
        if (type == SpearType.NIGHTMARE) {
            double reduction = cfg.getDouble("spears.nightmare.warding-reduction-percent", 15.0) / 100.0;
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }

        // Storm Surge passive (Thunderspear)
        if (type == SpearType.THUNDERSPEAR) {
            double triggerHp = cfg.getDouble("spears.thunderspear.storm-surge-trigger-hearts", 5.0);
            double currentHp = player.getHealth() - event.getFinalDamage();
            CooldownManager cd = plugin.getCooldownManager();

            if (currentHp <= triggerHp && !cd.isOnCooldown(player.getUniqueId(), "storm_surge")) {
                triggerStormSurge(player, cfg, cd);
            }
        }

        // Wraith Form passive (Phantom)
        if (type == SpearType.PHANTOM) {
            double triggerHp = cfg.getDouble("spears.phantom.wraith-trigger-hearts", 3.0);
            double currentHp = player.getHealth() - event.getFinalDamage();
            CooldownManager cd = plugin.getCooldownManager();

            if (currentHp <= triggerHp && !wraith.contains(player.getUniqueId())
                    && !cd.isOnCooldown(player.getUniqueId(), "wraith")) {
                triggerWraithForm(player, cfg, cd);
            }
        }
    }

    // ─── Storm Surge ─────────────────────────────────────────────────────────

    private void triggerStormSurge(Player player, FileConfiguration cfg, CooldownManager cd) {
        double radius = cfg.getDouble("spears.thunderspear.storm-surge-radius", 10.0);
        int cooldown = cfg.getInt("spears.thunderspear.storm-surge-cooldown", 30);
        int slowLevel = cfg.getInt("spears.thunderspear.storm-surge-slowness-level", 1);
        int slowDur = cfg.getInt("spears.thunderspear.storm-surge-slowness-duration", 4) * 20;
        int blindDur = cfg.getInt("spears.thunderspear.storm-surge-blindness-duration", 3) * 20;
        TrustManager tm = plugin.getTrustManager();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.8f);
        player.sendActionBar(net.kyori.adventure.text.Component.text("§e⚡ Storm Surge triggered!"));

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) continue;
            if (entity instanceof Player tp && tm.isTrusted(player.getUniqueId(), tp.getUniqueId())) continue;

            entity.getWorld().strikeLightningEffect(entity.getLocation());
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, slowLevel, false, true, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDur, 0, false, true, true));
        }

        cd.set(player.getUniqueId(), "storm_surge", cooldown);
    }

    // ─── Wraith Form ─────────────────────────────────────────────────────────

    private void triggerWraithForm(Player player, FileConfiguration cfg, CooldownManager cd) {
        int duration = cfg.getInt("spears.phantom.wraith-duration", 15);
        int cooldown = cfg.getInt("spears.phantom.wraith-cooldown", 300);
        UUID uuid = player.getUniqueId();

        wraith.add(uuid);
        cd.set(uuid, "wraith", cooldown);

        // Full invisibility — hide armor + item for all nearby players
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0, false, false, false));
        for (Player nearby : player.getWorld().getPlayers()) {
            if (!nearby.equals(player)) nearby.hidePlayer(plugin, player);
        }

        player.sendActionBar(net.kyori.adventure.text.Component.text("§f👻 Wraith Form active for " + duration + "s!"));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1f, 1.5f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.3);

        new BukkitRunnable() {
            @Override
            public void run() {
                wraith.remove(uuid);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                for (Player nearby : player.getWorld().getPlayers()) {
                    nearby.showPlayer(plugin, player);
                }
                player.sendActionBar(net.kyori.adventure.text.Component.text("§7Wraith Form ended. Cooldown: §e" + (cooldown / 60) + "m"));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 0.7f, 1.3f);
            }
        }.runTaskLater(plugin, duration * 20L);
    }

    // ─── Bloodthirst (Ashen) — kills grant bonus max HP ──────────────────────

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;

        ItemStack held = killer.getInventory().getItemInMainHand();
        if (CustomSpear.getSpearType(held) != SpearType.ASHEN) return;

        FileConfiguration cfg = plugin.getConfig();
        double hpPerKill = cfg.getDouble("spears.ashen.bloodthirst-hp-per-kill", 2.0);
        double maxBonus = cfg.getDouble("spears.ashen.bloodthirst-max-bonus-hp", 10.0);
        int duration = cfg.getInt("spears.ashen.bloodthirst-duration", 30);

        UUID uuid = killer.getUniqueId();
        double current = bloodthirstBonus.getOrDefault(uuid, 0.0);
        if (current >= maxBonus) return;

        double newBonus = Math.min(current + hpPerKill, maxBonus);
        double diff = newBonus - current;
        bloodthirstBonus.put(uuid, newBonus);

        // Apply max health modifier
        var attr = killer.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        NamespacedKey modKey = new NamespacedKey(plugin, "bloodthirst");

        // Remove old modifier first
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(modKey))
                .forEach(attr::removeModifier);

        attr.addModifier(new AttributeModifier(modKey, newBonus, AttributeModifier.Operation.ADD_NUMBER));

        killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
        killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.8f);

        // Schedule removal after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                bloodthirstBonus.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return;
                var a = p.getAttribute(Attribute.MAX_HEALTH);
                if (a == null) return;
                a.getModifiers().stream()
                        .filter(m -> m.getKey().equals(modKey))
                        .forEach(a::removeModifier);
                // Clamp health if it now exceeds max
                if (p.getHealth() > a.getValue()) p.setHealth(a.getValue());
            }
        }.runTaskLater(plugin, duration * 20L);
    }

    // ─── Fortune passive — Luck + Hero of the Village while held ─────────────

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Remove luck passives from previous spear
        SpearType prev = lastHeld.get(player.getUniqueId());
        if (prev == SpearType.FORTUNE) {
            player.removePotionEffect(PotionEffectType.LUCK);
            player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        }

        // Apply for new held item
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack held = player.getInventory().getItemInMainHand();
            SpearType type = CustomSpear.getSpearType(held);
            lastHeld.put(player.getUniqueId(), type);

            if (type == SpearType.FORTUNE) {
                applyFortunePassive(player);
            }
        }, 1L);
    }

    private void applyFortunePassive(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        int luckLevel = cfg.getInt("spears.fortune.luck-level", 1);
        int heroLevel = cfg.getInt("spears.fortune.hero-level", 0);

        // Apply indefinite (very long duration) effects while holding
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, luckLevel, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, heroLevel, true, false, false));
    }

    // Also re-apply fortune on join / world change if needed — tick every 5s as safety net
    // This is handled in InventoryListener ticking logic instead.
}
