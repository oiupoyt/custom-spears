package dev.kat.customspears.spears;

import dev.kat.customspears.CustomSpears;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SpearRegistry {

    private final CustomSpears plugin;
    private final Map<SpearType, CustomSpear> spears = new EnumMap<>(SpearType.class);

    public SpearRegistry(CustomSpears plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        spears.clear();
        FileConfiguration cfg = plugin.getConfig();
        boolean showLore = cfg.getBoolean("ui.show-lore-descriptions", true);

        register(SpearType.NIGHTMARE, cfg, showLore);
        register(SpearType.SANGUINE, cfg, showLore);
        register(SpearType.THUNDERSPEAR, cfg, showLore);
        register(SpearType.ASHEN, cfg, showLore);
        register(SpearType.PHANTOM, cfg, showLore);
        register(SpearType.FORTUNE, cfg, showLore);
    }

    private void register(SpearType type, FileConfiguration cfg, boolean showLore) {
        String key = configKey(type);
        String name = cfg.getString("spears." + key + ".name", "&fUnknown Spear");
        int cmd = cfg.getInt("spears." + key + ".custom-model-data", 1000);
        List<String> lore = buildLore(type, cfg, showLore);
        List<ItemStack[]> recipes = buildRecipes(type);
        spears.put(type, new CustomSpear(type, key, name, cmd, lore, recipes));
    }

    private String configKey(SpearType type) {
        return switch (type) {
            case NIGHTMARE -> "nightmare";
            case SANGUINE -> "sanguine";
            case THUNDERSPEAR -> "thunderspear";
            case ASHEN -> "ashen";
            case PHANTOM -> "phantom";
            case FORTUNE -> "fortune";
        };
    }

    private List<String> buildLore(SpearType type, FileConfiguration cfg, boolean showLore) {
        String k = "spears." + configKey(type) + ".";
        List<String> lore = new ArrayList<>();
        lore.add("&8&m                    ");

        switch (type) {
            case NIGHTMARE -> {
                lore.add("&7&lPassive &8» &fWarding");
                if (showLore) lore.add("  &7Reduces incoming damage by &e" + cfg.getDouble(k + "warding-reduction-percent", 15) + "%");
                lore.add("&7&lOn Hit &8» &fFrostbite");
                if (showLore) lore.add("  &7Applies &bSlowness " + toRoman(cfg.getInt(k + "frostbite-slowness-level", 2) + 1) + " &7& &8Blindness &7to enemy");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "frostbite-cooldown", 8) + "s");
                lore.add("&7&lRight-Click &8» &fRecall");
                if (showLore) lore.add("  &7Marks your position. Right-click again to return.");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "recall-cooldown", 20) + "s");
            }
            case SANGUINE -> {
                lore.add("&7&lPassive &8» &fLifesteal");
                if (showLore) lore.add("  &7Heals &c" + cfg.getDouble(k + "lifesteal-percent", 8) + "% &7of damage dealt");
                lore.add("&7&lOn Hit &8» &fRupture");
                if (showLore) lore.add("  &7Delayed burst of &c" + cfg.getDouble(k + "rupture-damage", 6) + " &7damage after &e" + cfg.getInt(k + "rupture-delay", 2) + "s");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "rupture-cooldown", 10) + "s");
                lore.add("&7&lRight-Click &8» &fSuppress");
                if (showLore) lore.add("  &7Disables enemy spear & prevents jumping for &e" + cfg.getInt(k + "suppress-duration", 30) + "s");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "suppress-cooldown", 25) + "s");
            }
            case THUNDERSPEAR -> {
                lore.add("&7&lPassive &8» &fStorm Surge");
                if (showLore) lore.add("  &7At &c≤" + (int) cfg.getDouble(k + "storm-surge-trigger-hearts", 5) / 2 + " &7hearts: AOE lightning in &e" + cfg.getDouble(k + "storm-surge-radius", 10) + " &7blocks");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "storm-surge-cooldown", 30) + "s");
                lore.add("&7&lOn Hit &8» &fLightning Chain");
                if (showLore) lore.add("  &7Chains lightning to up to &e" + cfg.getInt(k + "chain-max-targets", 4) + " &7enemies within &e" + cfg.getDouble(k + "chain-radius", 5) + " &7blocks");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "chain-cooldown", 6) + "s");
                lore.add("&7&lRight-Click &8» &fVault");
                if (showLore) lore.add("  &7Launches you forward and upward");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "vault-cooldown", 12) + "s");
            }
            case ASHEN -> {
                lore.add("&7&lPassive &8» &fBloodthirst");
                if (showLore) lore.add("  &7Kills grant +" + cfg.getDouble(k + "bloodthirst-hp-per-kill", 2) + " max HP for &e" + cfg.getInt(k + "bloodthirst-duration", 30) + "s");
                lore.add("&7&lOn Hit &8» &fShatter");
                if (showLore) lore.add("  &7Applies Mining Fatigue to enemy");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "shatter-cooldown", 8) + "s");
                lore.add("&7&lRight-Click &8» &fGround Slam");
                if (showLore) lore.add("  &7Knocks back all enemies within &e" + cfg.getDouble(k + "ground-slam-radius", 6) + " &7blocks");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "ground-slam-cooldown", 15) + "s");
            }
            case PHANTOM -> {
                lore.add("&7&lPassive &8» &fWraith Form");
                if (showLore) lore.add("  &7At &c≤" + (int)(cfg.getDouble(k + "wraith-trigger-hearts", 3) / 2) + " &7hearts: Full invisibility for &e" + cfg.getInt(k + "wraith-duration", 15) + "s");
                lore.add("  &8Cooldown: &e" + (cfg.getInt(k + "wraith-cooldown", 300) / 60) + "m");
                lore.add("&7&lOn Hit &8» &fPuncture");
                if (showLore) lore.add("  &7Strips &e" + cfg.getInt(k + "puncture-armor-reduction", 10) + " &7armor points for &e" + cfg.getInt(k + "puncture-duration", 6) + "s");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "puncture-cooldown", 10) + "s");
                lore.add("&7&lRight-Click &8» &fPhantom Dash");
                if (showLore) lore.add("  &7Speed burst + brief invisibility forward dash");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "phantom-dash-cooldown", 14) + "s");
            }
            case FORTUNE -> {
                lore.add("&7&lPassive &8» &fLuck & Hero of the Village");
                if (showLore) lore.add("  &7Grants Luck & Hero of the Village while held");
                lore.add("&7&lOn Hit &8» &fCurse");
                if (showLore) lore.add("  &7Applies a random negative effect for &e" + cfg.getInt(k + "curse-duration", 30) + "s");
                lore.add("  &8Per-effect cooldown: &e" + cfg.getInt(k + "curse-cooldown", 60) + "s");
                lore.add("&7&lRight-Click &8» &fBoon");
                if (showLore) lore.add("  &7Grants yourself a random positive effect for &e" + cfg.getInt(k + "boon-duration", 30) + "s");
                lore.add("  &8Cooldown: &e" + cfg.getInt(k + "boon-cooldown", 45) + "s");
            }
        }

        lore.add("&8&m                    ");
        lore.add("&8Custom Spears &7| &ePvP");
        return lore;
    }

    /**
     * Shapeless recipes using netherite spear + a unique material per spear.
     * You can adjust these or replace with shaped recipes as needed.
     * Slot layout (3x3): 0-8, top-left to bottom-right.
     */
    private List<ItemStack[]> buildRecipes(SpearType type) {
        // Each recipe: [9] slots for a shaped 3x3 crafting grid
        // N = netherite spear, unique material per spear
        // Recipe pattern: diagonal with unique material
        ItemStack spear = new ItemStack(Material.NETHERITE_SPEAR);
        ItemStack[] grid = new ItemStack[9];

        switch (type) {
            case NIGHTMARE -> {
                // Netherite Spear + Amethyst Shard + Crying Obsidian
                grid[0] = new ItemStack(Material.CRYING_OBSIDIAN);
                grid[4] = spear.clone();
                grid[8] = new ItemStack(Material.AMETHYST_SHARD);
            }
            case SANGUINE -> {
                // Netherite Spear + Nether Star + Redstone Block
                grid[0] = new ItemStack(Material.REDSTONE_BLOCK);
                grid[4] = spear.clone();
                grid[8] = new ItemStack(Material.NETHER_STAR);
            }
            case THUNDERSPEAR -> {
                // Netherite Spear + Lightning Rod + Gold Block
                grid[0] = new ItemStack(Material.LIGHTNING_ROD);
                grid[4] = spear.clone();
                grid[8] = new ItemStack(Material.GOLD_BLOCK);
            }
            case ASHEN -> {
                // Netherite Spear + Ancient Debris + Wither Skull
                grid[0] = new ItemStack(Material.ANCIENT_DEBRIS);
                grid[4] = spear.clone();
                grid[8] = new ItemStack(Material.WITHER_SKELETON_SKULL);
            }
            case PHANTOM -> {
                // Netherite Spear + Phantom Membrane + Eye of Ender
                grid[0] = new ItemStack(Material.PHANTOM_MEMBRANE);
                grid[4] = spear.clone();
                grid[8] = new ItemStack(Material.ENDER_EYE);
            }
            case FORTUNE -> {
                // Netherite Spear + Emerald Block + Nether Star
                grid[0] = new ItemStack(Material.EMERALD_BLOCK);
                grid[4] = spear.clone();
                grid[8] = new ItemStack(Material.NETHER_STAR);
            }
        }

        return List.of(grid);
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n);
        };
    }

    public CustomSpear get(SpearType type) { return spears.get(type); }
    public Collection<CustomSpear> getAll() { return spears.values(); }

    public CustomSpear getByName(String name) {
        for (CustomSpear spear : spears.values()) {
            if (spear.getType().name().equalsIgnoreCase(name) ||
                spear.getConfigKey().equalsIgnoreCase(name)) {
                return spear;
            }
        }
        return null;
    }
}
