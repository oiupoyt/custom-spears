package dev.kat.customspears.managers;

import dev.kat.customspears.CustomSpears;

import java.io.File;

public class DataManager {

    private final CustomSpears plugin;
    private final File dataFile;

    public DataManager(CustomSpears plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
    }

    public File getDataFile() { return dataFile; }

    public void save() {
        plugin.getTrustManager().save();
    }
}
