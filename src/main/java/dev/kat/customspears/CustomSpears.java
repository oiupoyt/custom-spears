package dev.kat.customspears;

import dev.kat.customspears.commands.SpearCommand;
import dev.kat.customspears.commands.TrustCommand;
import dev.kat.customspears.listeners.*;
import dev.kat.customspears.managers.CooldownManager;
import dev.kat.customspears.managers.DataManager;
import dev.kat.customspears.managers.SuppressManager;
import dev.kat.customspears.managers.TrustManager;
import dev.kat.customspears.spears.SpearRegistry;
import dev.kat.customspears.ui.ActionBarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomSpears extends JavaPlugin {

    private static CustomSpears instance;

    private SpearRegistry spearRegistry;
    private CooldownManager cooldownManager;
    private TrustManager trustManager;
    private SuppressManager suppressManager;
    private DataManager dataManager;
    private ActionBarManager actionBarManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Init managers
        dataManager = new DataManager(this);
        spearRegistry = new SpearRegistry(this);
        cooldownManager = new CooldownManager();
        trustManager = new TrustManager(this);
        suppressManager = new SuppressManager(this);
        actionBarManager = new ActionBarManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new SpearHitListener(this), this);
        getServer().getPluginManager().registerEvents(new SpearRightClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PassiveListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        // Register commands
        getCommand("spears").setExecutor(new SpearCommand(this));
        getCommand("trust").setExecutor(new TrustCommand(this));

        // Start action bar task
        actionBarManager.startTask();

        getLogger().info("CustomSpears enabled! Running on Paper 1.21.11.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.save();
        if (suppressManager != null) suppressManager.clearAll();
        getLogger().info("CustomSpears disabled.");
    }

    public static CustomSpears getInstance() { return instance; }
    public SpearRegistry getSpearRegistry() { return spearRegistry; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public SuppressManager getSuppressManager() { return suppressManager; }
    public DataManager getDataManager() { return dataManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
}
