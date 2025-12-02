package com.flyaway.deathchest.listeners;

import com.flyaway.deathchest.DeathChest;
import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import com.flyaway.deathchest.managers.MessageManager;
import net.kyori.adventure.text.Component;
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
import java.util.Map;

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
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack original : event.getDrops()) {
            if (original != null && original.getType() != Material.AIR) {
                drops.add(original.clone());
            }
        }

        if (!player.hasPermission("deathchest.use")) {
            return;
        }

        if (drops.isEmpty()) {
            return;
        }

        if (!shouldCreateDeathChest(player)) {
            return;
        }

        if (!isWorldAllowed(player.getWorld().getName())) {
            return;
        }

        Location chestLocation = findChestLocation(player.getLocation());
        if (chestLocation == null) {
            plugin.getLogger().warning("Не удалось найти подходящее место для сундука смерти игрока " + player.getName());
            return;
        }

        if (chestManager.createDeathChest(player, drops, chestLocation)) {
            event.getDrops().clear();

            Component message = MessageManager.buildMessage("chest-created",
                    "Ваш сундук смерти создан на координатах: X: {x} Y: {y} Z: {z}",
                    Map.of(
                            "x", String.valueOf(chestLocation.getBlockX()),
                            "y", String.valueOf(chestLocation.getBlockY()),
                            "z", String.valueOf(chestLocation.getBlockZ())
                    ));

            player.sendMessage(message);
        }
    }

    private boolean shouldCreateDeathChest(Player player) {
        return !configManager.isMobDeathOnly() || wasKilledByMob(player);
    }

    private boolean wasKilledByMob(Player player) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent entityEvent)) {
            return false;
        }

        Entity damager = entityEvent.getDamager();

        if (damager instanceof LivingEntity && !(damager instanceof Player)) {
            return true;
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof LivingEntity && !(shooter instanceof Player);
        }

        return false;
    }

    private boolean isWorldAllowed(String worldName) {
        List<String> allowedWorlds = configManager.getAllowedWorlds();
        List<String> blacklistedWorlds = configManager.getBlacklistedWorlds();

        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(worldName)) {
            return false;
        }

        return !blacklistedWorlds.contains(worldName);
    }

    private Location findChestLocation(Location deathLocation) {
        Location location = deathLocation.clone();

        if (isSuitableForChest(location.getBlock())) {
            return location;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -2; y <= 2; y++) {
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
        return type.isAir()
                || type == Material.SHORT_GRASS
                || type == Material.TALL_GRASS
                || type == Material.FERN
                || type == Material.LARGE_FERN
                || type == Material.DEAD_BUSH
                || type == Material.SEAGRASS
                || type == Material.TALL_SEAGRASS
                || type == Material.VINE
                || type == Material.SNOW
                || type == Material.WATER
                || type == Material.LAVA;
    }
}
