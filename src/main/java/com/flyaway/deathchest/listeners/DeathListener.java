package com.flyaway.deathchest.listeners;

import com.flyaway.deathchest.DeathChest;
import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private final DeathChest plugin;
    private final ConfigManager configManager;
    private final ChestManager chestManager;

    public DeathListener(DeathChest plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chestManager = plugin.getChestManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());

        // Проверка разрешения deathchest.use
        if (!player.hasPermission("deathchest.use")) {
            return;
        }

        // Проверка если нет вещей - не создаем сундук
        if (drops.isEmpty()) {
            return;
        }

        // Check if death chest should be created
        if (!shouldCreateDeathChest(player)) {
            return;
        }

        // Check world restrictions
        if (!isWorldAllowed(player.getWorld().getName())) {
            return;
        }

        // Find suitable location for chest
        Location chestLocation = findChestLocation(player.getLocation());
        if (chestLocation == null) {
            plugin.getLogger().warning("Не удалось найти подходящее место для сундука смерти игрока " + player.getName());
            return;
        }

        // Create death chest
        if (chestManager.createDeathChest(player, drops, chestLocation)) {
            // Clear original drops
            event.getDrops().clear();

            // Send message to player
            String message = configManager.getMessage("chest-created", "Ваш сундук смерти создан на координатах: X: {x} Y: {y} Z: {z}")
                .replace("{x}", String.valueOf(chestLocation.getBlockX()))
                .replace("{y}", String.valueOf(chestLocation.getBlockY()))
                .replace("{z}", String.valueOf(chestLocation.getBlockZ()));

            player.sendMessage(configManager.getPrefix() + " " + message);
        }
    }

    private boolean shouldCreateDeathChest(Player player) {
        // Check mob-death-only setting
        if (configManager.isMobDeathOnly() && !wasKilledByMob(player)) {
            return false;
        }

        return true;
    }

    private boolean wasKilledByMob(Player player) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent)) {
            return false;
        }

        EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) lastDamage;
        Entity damager = entityEvent.getDamager();

        // Check if damager is a mob
        if (damager instanceof Monster || damager instanceof Animals) {
            return true;
        }

        // Check if damager is a projectile from a mob
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Monster || shooter instanceof Animals) {
                return true;
            }
        }

        return false;
    }

    private boolean isWorldAllowed(String worldName) {
        List<String> allowedWorlds = configManager.getAllowedWorlds();
        List<String> blacklistedWorlds = configManager.getBlacklistedWorlds();

        // If allowed worlds is specified, only allow those worlds
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(worldName)) {
            return false;
        }

        // Check blacklist
        return !blacklistedWorlds.contains(worldName);
    }

    private Location findChestLocation(Location deathLocation) {
        Location location = deathLocation.clone();
        Block block = location.getBlock();

        // Try current position
        if (isSuitableForChest(block)) {
            return location;
        }

        // Try positions around death location
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 2; y++) {
                    Location testLoc = location.clone().add(x, y, z);
                    if (isSuitableForChest(testLoc.getBlock())) {
                        return testLoc;
                    }
                }
            }
        }

        return null;
    }

    private boolean isSuitableForChest(Block block) {
        Material type = block.getType();
        return (type.isAir() || type == Material.WATER || type == Material.LAVA) &&
               block.getRelative(0, 1, 0).getType().isAir();
    }
}
