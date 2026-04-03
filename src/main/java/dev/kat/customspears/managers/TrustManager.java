package dev.kat.customspears.managers;

import dev.kat.customspears.CustomSpears;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrustManager {

    private final CustomSpears plugin;
    // truster UUID -> set of trusted UUIDs
    private final Map<UUID, Set<UUID>> trustMap = new HashMap<>();

    public TrustManager(CustomSpears plugin) {
        this.plugin = plugin;
        load();
    }

    public void trust(UUID truster, UUID trusted) {
        trustMap.computeIfAbsent(truster, k -> new HashSet<>()).add(trusted);
        save();
    }

    public void untrust(UUID truster, UUID trusted) {
        if (trustMap.containsKey(truster)) {
            trustMap.get(truster).remove(trusted);
            save();
        }
    }

    public boolean isTrusted(UUID truster, UUID target) {
        if (!plugin.getConfig().getBoolean("trust.strict-mode", true)) return false;
        Set<UUID> trusted = trustMap.get(truster);
        return trusted != null && trusted.contains(target);
    }

    public Set<UUID> getTrusted(UUID truster) {
        return trustMap.getOrDefault(truster, Collections.emptySet());
    }

    public void clear(UUID truster) {
        trustMap.remove(truster);
        save();
    }

    private void load() {
        File dataFile = plugin.getDataManager().getDataFile();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (data.contains("trust")) {
            for (String key : data.getConfigurationSection("trust").getKeys(false)) {
                UUID truster = UUID.fromString(key);
                List<String> trustedList = data.getStringList("trust." + key);
                Set<UUID> trustedSet = new HashSet<>();
                for (String s : trustedList) trustedSet.add(UUID.fromString(s));
                trustMap.put(truster, trustedSet);
            }
        }
    }

    public void save() {
        File dataFile = plugin.getDataManager().getDataFile();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        for (Map.Entry<UUID, Set<UUID>> entry : trustMap.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID uuid : entry.getValue()) list.add(uuid.toString());
            data.set("trust." + entry.getKey(), list);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save trust data: " + e.getMessage());
        }
    }
}
