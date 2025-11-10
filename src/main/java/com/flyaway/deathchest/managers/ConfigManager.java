package com.flyaway.deathchest.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // Chest creation settings
    public boolean isMobDeathOnly() {
        return config.getBoolean("chest-creation.mob-death-only", false);
    }

    public List<String> getAllowedWorlds() {
        return config.getStringList("chest-creation.allowed-worlds");
    }

    public List<String> getBlacklistedWorlds() {
        return config.getStringList("chest-creation.blacklisted-worlds");
    }

    // Chest interaction settings
    public boolean allowAccessOthersChests() {
        return config.getBoolean("chest-interactions.allow-access-others-chests", false);
    }

    public boolean isPlayerBreakable() {
        return config.getBoolean("chest-interactions.player-breakable", false);
    }

    public boolean isExplosionProof() {
        return config.getBoolean("chest-interactions.explosion-proof", true);
    }

    public boolean dropItemsWhenExploded() {
        return config.getBoolean("chest-interactions.items-drop-when-exploded", true);
    }

    public boolean dropItemsWhenBroken() {
        return config.getBoolean("chest-interactions.items-drop-when-broken", true);
    }

    public boolean removeEmptyChests() {
        return config.getBoolean("chest-interactions.remove-empty-chests", true);
    }

    // Appearance settings
    public boolean showNameOnChest() {
        return config.getBoolean("chest-appearance.name-on-chest", true);
    }

    public String getChestTitle() {
        return config.getString("chest-appearance.chest-title", "Сундук смерти: {player}");
    }

    public int getExpirationTime() {
        return config.getInt("chest-appearance.expiration-time", 0);
    }

    public String getMessage(String path, String def) {
        return config.getString("messages." + path, def);
    }

    public String getPrefix() {
        return getMessage("prefix", "<gradient:gold:white>[DeathChest]</gradient>");
    }
}
