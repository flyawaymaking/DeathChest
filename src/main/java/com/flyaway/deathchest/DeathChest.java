package com.flyaway.deathchest;

import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import com.flyaway.deathchest.managers.HologramManager;
import com.flyaway.deathchest.listeners.ChestInteractionListener;
import com.flyaway.deathchest.listeners.DeathListener;
import com.flyaway.deathchest.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class DeathChest extends JavaPlugin {

    private static DeathChest instance;
    private ConfigManager configManager;
    private ChestManager chestManager;
    private BukkitRunnable scheduledTask;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.chestManager = new ChestManager(this);
        this.hologramManager = new HologramManager(this);

        configManager.loadConfig();

        MessageManager.init(this);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestInteractionListener(this), this);

        Objects.requireNonNull(getCommand("deathchest")).setExecutor(new DeathChestCommand(this));

        chestManager.loadChests();

        runScheduledTask();

        getLogger().info("DeathChest был включен!");
    }

    @Override
    public void onDisable() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
        }

        chestManager.disableChests();
        getLogger().info("DeathChest был выключен!");
    }

    private void runScheduledTask() {
        this.scheduledTask = new BukkitRunnable() {
            @Override
            public void run() {
                chestManager.runScheduledTasks();
            }
        };
        this.scheduledTask.runTaskTimer(this, 6000L, 6000L); // 5 минут
    }

    public void reloadConfiguration() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
        }

        configManager.reloadConfig();
        MessageManager.init(this);
        chestManager.reloadChests();
        hologramManager.reload();

        runScheduledTask();

        getLogger().info("Конфигурация DeathChest перезагружена!");
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
