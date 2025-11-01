package com.flyaway.deathchest;

import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import com.flyaway.deathchest.managers.HologramManager;
import com.flyaway.deathchest.listeners.ChestInteractionListener;
import com.flyaway.deathchest.listeners.DeathListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathChest extends JavaPlugin {

    private static DeathChest instance;
    private ConfigManager configManager;
    private ChestManager chestManager;
    private BukkitRunnable scheduledTask;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.chestManager = new ChestManager(this);
        this.hologramManager = new HologramManager(this);

        // Load configuration
        configManager.loadConfig();

        // Register listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestInteractionListener(this), this);

        // Register commands
        getCommand("deathchest").setExecutor(new DeathChestCommand(this));

        // Load existing chests
        chestManager.loadChests();

        // Запускаем периодическую задачу
        this.scheduledTask = new BukkitRunnable() {
            @Override
            public void run() {
                chestManager.runScheduledTasks();
            }
        };
        this.scheduledTask.runTaskTimer(this, 6000L, 6000L); // 5 минут

        getLogger().info("DeathChest был включен!");
    }

    @Override
    public void onDisable() {
        // Отменяем задачу если она запущена
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
        }

        // Final save chest data and disable chests
        chestManager.disableChests();
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

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}
