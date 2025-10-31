package com.flyaway.deathchest;

import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import com.flyaway.deathchest.listeners.ChestInteractionListener;
import com.flyaway.deathchest.listeners.DeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathChest extends JavaPlugin {

    private static DeathChest instance;
    private ConfigManager configManager;
    private ChestManager chestManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.chestManager = new ChestManager(this);

        // Load configuration
        configManager.loadConfig();

        // Register listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestInteractionListener(this), this);

        // Register commands
        getCommand("deathchest").setExecutor(new DeathChestCommand(this));

        // Load existing chests
        chestManager.loadChests();

        getLogger().info("DeathChest был включен!");
    }

    @Override
    public void onDisable() {
        // Save chest data
        chestManager.saveChests();
        getLogger().info("DeathChest был выключен!");
    }

    public static DeathChest getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }
}
