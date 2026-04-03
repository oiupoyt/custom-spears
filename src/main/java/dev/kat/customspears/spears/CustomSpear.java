package dev.kat.customspears.spears;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CustomSpear {

    private final SpearType type;
    private final String configKey;
    private final String displayName;
    private final int customModelData;
    private final List<String> lore;
    private final List<ItemStack[]> recipes; // each recipe is a 9-slot crafting array

    public CustomSpear(SpearType type, String configKey, String displayName, int customModelData, List<String> lore, List<ItemStack[]> recipes) {
        this.type = type;
        this.configKey = configKey;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.lore = lore;
        this.recipes = recipes;
    }

    public ItemStack buildItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_SPEAR);
        ItemMeta meta = item.getItemMeta();

        // Name
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));

        // Lore
        List<Component> loreComponents = lore.stream()
                .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                .toList();
        meta.lore(loreComponents);

        // Custom model data for resource pack texture
        meta.setCustomModelData(customModelData);

        // PDC tag to identify this as a custom spear
        NamespacedKey key = new NamespacedKey("customspears", "spear_type");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());

        meta.setUnbreakable(true);

        item.setItemMeta(meta);
        return item;
    }

    public SpearType getType() { return type; }
    public String getConfigKey() { return configKey; }
    public String getDisplayName() { return displayName; }
    public int getCustomModelData() { return customModelData; }
    public List<String> getLore() { return lore; }
    public List<ItemStack[]> getRecipes() { return recipes; }

    /**
     * Returns the SpearType from an ItemStack's PDC, or null if not a custom spear.
     */
    public static SpearType getSpearType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("customspears", "spear_type");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return null;
        String val = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        try {
            return SpearType.valueOf(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isCustomSpear(ItemStack item) {
        return getSpearType(item) != null;
    }
}
