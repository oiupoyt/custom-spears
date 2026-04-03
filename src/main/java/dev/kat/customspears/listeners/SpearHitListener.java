package dev.kat.customspears.listeners;

import dev.kat.customspears.CustomSpears;
import dev.kat.customspears.managers.CooldownManager;
import dev.kat.customspears.managers.SuppressManager;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpearHitListener implements Listener {

    private final CustomSpears plugin;

    public SpearHitListener(CustomSpears plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack held = attacker.getInventory().getItemInMainHand();
        SpearType type = CustomSpear.getSpearType(held);
        if (type == null) return;

        UUID attackerUUID = attacker.getUniqueId();
        CooldownManager cd = plugin.getCooldownManager();
        SuppressManager sm = plugin.getSuppressManager();
        TrustManager tm = plugin.getTrustManager();
        FileConfiguration cfg = plugin.getConfig();

        // Check if suppressed
        if (sm.isSuppressed(attackerUUID)) return;

        // Check if target is trusted (skip all ability effects)
        if (target instanceof Player targetPlayer && tm.isTrusted(attackerUUID, targetPlayer.getUniqueId())) return;

        double damage = event.getFinalDamage();

        switch (type) {
            case NIGHTMARE -> handleFrostbite(attacker, target, cfg, cd);
            case SANGUINE -> {
                handleLifesteal(attacker, damage, cfg);
                handleRupture(attacker, target, cfg, cd, tm);
            }
            case THUNDERSPEAR -> {
handleLightningChain(attacker, target, cfg, cd, tm);
            }
            case ASHEN -> {
                handleShatter(attacker, target, cfg, cd);
            }
            case PHANTOM -> {
                handlePuncture(attacker, target, cfg, cd);
            }
            case FORTUNE -> {
                handleCurse(attacker, target, cfg, cd, tm);
            }
        }

    }

    // ─── Frostbite ───────────────────────────────────────────────────────────

    private void handleFrostbite(Player attacker, LivingEntity target, FileConfiguration cfg, CooldownManager cd) {
        String key = "frostbite";
        if (cd.isOnCooldown(attacker.getUniqueId(), key)) return;

        int slowLevel = cfg.getInt("spears.nightmare.frostbite-slowness-level", 2);
        int slowDur = cfg.getInt("spears.nightmare.frostbite-slowness-duration", 5) * 20;
        int blindDur = cfg.getInt("spears.nightmare.frostbite-blindness-duration", 3) * 20;
        int cooldown = cfg.getInt("spears.nightmare.frostbite-cooldown", 8);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, slowLevel, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDur, 0, false, true, true));
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);

        cd.set(attacker.getUniqueId(), key, cooldown);
    }

    // ─── Lifesteal ────────────────────────────────────────────────────────────

    private void handleLifesteal(Player attacker, double damage, FileConfiguration cfg) {
        double percent = cfg.getDouble("spears.sanguine.lifesteal-percent", 8.0);
        double heal = damage * (percent / 100.0);
        double maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHp = Math.min(attacker.getHealth() + heal, maxHp);
        attacker.setHealth(newHp);
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
    }

    // ─── Rupture ─────────────────────────────────────────────────────────────

    private void handleRupture(Player attacker, LivingEntity target, FileConfiguration cfg, CooldownManager cd, TrustManager tm) {
        String key = "rupture";
        if (cd.isOnCooldown(attacker.getUniqueId(), key)) return;

        int delay = cfg.getInt("spears.sanguine.rupture-delay", 2) * 20;
        double damage = cfg.getDouble("spears.sanguine.rupture-damage", 6.0);
        int cooldown = cfg.getInt("spears.sanguine.rupture-cooldown", 10);

        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 0.7f);

        UUID targetUUID = target.getUniqueId();
        UUID attackerUUID = attacker.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                Entity entity = Bukkit.getEntity(targetUUID);
                if (!(entity instanceof LivingEntity living)) return;
                if (!living.isValid() || living.isDead()) return;
                if (living instanceof Player tp && tm.isTrusted(attackerUUID, tp.getUniqueId())) return;

                living.damage(damage, attacker);
                living.getWorld().spawnParticle(Particle.EXPLOSION, living.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                living.getWorld().playSound(living.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
            }
        }.runTaskLater(plugin, delay);

        cd.set(attacker.getUniqueId(), key, cooldown);
    }

    // ─── Lightning Chain ─────────────────────────────────────────────────────

    private void handleLightningChain(Player attacker, LivingEntity primary, FileConfiguration cfg, CooldownManager cd, TrustManager tm) {
        String key = "chain";
        if (cd.isOnCooldown(attacker.getUniqueId(), key)) return;

        double radius = cfg.getDouble("spears.thunderspear.chain-radius", 5.0);
        int maxTargets = cfg.getInt("spears.thunderspear.chain-max-targets", 4);
        double chainDamage = cfg.getDouble("spears.thunderspear.chain-damage", 4.0);
        int cooldown = cfg.getInt("spears.thunderspear.chain-cooldown", 6);

        // Strike the primary target
        strikeWithLightning(primary, attacker, chainDamage);

        // Find nearby entities to chain to
        List<Entity> nearby = primary.getNearbyEntities(radius, radius, radius);
        int count = 0;
        for (Entity entity : nearby) {
            if (count >= maxTargets) break;
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(attacker)) continue;
            if (entity.equals(primary)) continue;
            if (living instanceof Player tp && tm.isTrusted(attacker.getUniqueId(), tp.getUniqueId())) continue;

            strikeWithLightning(living, attacker, chainDamage);
            count++;
        }

        cd.set(attacker.getUniqueId(), key, cooldown);
    }

    private void strikeWithLightning(LivingEntity target, Player attacker, double extraDamage) {
        Location loc = target.getLocation();
        // Spawn cosmetic lightning (not actual damage lightning to avoid fire/item damage)
        loc.getWorld().strikeLightningEffect(loc);
        target.damage(extraDamage, attacker);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true, true));
    }

    // ─── Shatter ─────────────────────────────────────────────────────────────

    private void handleShatter(Player attacker, LivingEntity target, FileConfiguration cfg, CooldownManager cd) {
        String key = "shatter";
        if (cd.isOnCooldown(attacker.getUniqueId(), key)) return;

        int level = cfg.getInt("spears.ashen.shatter-level", 1);
        int duration = cfg.getInt("spears.ashen.shatter-duration", 8) * 20;
        int cooldown = cfg.getInt("spears.ashen.shatter-cooldown", 8);

        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, level, false, true, true));
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                20, 0.3, 0.5, 0.3, 0.1, Material.NETHERITE_BLOCK.createBlockData());
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);

        cd.set(attacker.getUniqueId(), key, cooldown);
    }

    // ─── Puncture ────────────────────────────────────────────────────────────

    private void handlePuncture(Player attacker, LivingEntity target, FileConfiguration cfg, CooldownManager cd) {
        String key = "puncture";
        if (cd.isOnCooldown(attacker.getUniqueId(), key)) return;
        if (!(target instanceof Player targetPlayer)) return; // PvP focused

        int reduction = cfg.getInt("spears.phantom.puncture-armor-reduction", 10);
        int duration = cfg.getInt("spears.phantom.puncture-duration", 6);
        int cooldown = cfg.getInt("spears.phantom.puncture-cooldown", 10);

        var armorAttr = targetPlayer.getAttribute(Attribute.ARMOR);
        if (armorAttr == null) return;

        AttributeModifier mod = new AttributeModifier(
                new NamespacedKey(plugin, "puncture_" + attacker.getUniqueId()),
                -reduction,
                AttributeModifier.Operation.ADD_NUMBER
        );
        armorAttr.addModifier(mod);

        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.2);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.7f, 1.2f);

        UUID targetUUID = targetPlayer.getUniqueId();
        NamespacedKey modKey = new NamespacedKey(plugin, "puncture_" + attacker.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(targetUUID);
                if (p == null) return;
                var attr = p.getAttribute(Attribute.ARMOR);
                if (attr == null) return;
                attr.getModifiers().stream()
                        .filter(m -> m.getKey().equals(modKey))
                        .forEach(attr::removeModifier);
            }
        }.runTaskLater(plugin, duration * 20L);

        cd.set(attacker.getUniqueId(), key, cooldown);
    }

    // ─── Curse ───────────────────────────────────────────────────────────────

    private void handleCurse(Player attacker, LivingEntity target, FileConfiguration cfg, CooldownManager cd, TrustManager tm) {
        if (!(target instanceof Player targetPlayer)) return;
        if (tm.isTrusted(attacker.getUniqueId(), targetPlayer.getUniqueId())) return;

        List<String> effectNames = cfg.getStringList("spears.fortune.negative-effects");
        int curseDuration = cfg.getInt("spears.fortune.curse-duration", 30) * 20;
        int curseCooldown = cfg.getInt("spears.fortune.curse-cooldown", 60);

        // Filter to effects not currently on cooldown
        List<PotionEffectType> available = new ArrayList<>();
        for (String name : effectNames) {
            PotionEffectType pet = getPotionEffectType(name);
            if (pet == null) continue;
            String cooldownKey = "curse:" + name;
            if (!cd.isOnCooldown(attacker.getUniqueId(), cooldownKey)) {
                available.add(pet);
            }
        }

        if (available.isEmpty()) return;

        PotionEffectType chosen = available.get(new Random().nextInt(available.size()));
        targetPlayer.addPotionEffect(new PotionEffect(chosen, curseDuration, 1, false, true, true));

        String cooldownKey = "curse:" + chosen.getKey().getKey().toUpperCase();
        cd.set(attacker.getUniqueId(), cooldownKey, curseCooldown);

        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_WITCH_THROW, 0.7f, 0.8f);
        targetPlayer.getWorld().spawnParticle(Particle.WITCH, targetPlayer.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
    }

    private PotionEffectType getPotionEffectType(String name) {
        try {
            return PotionEffectType.getByName(name);
        } catch (Exception e) {
            return null;
        }
    }
}
