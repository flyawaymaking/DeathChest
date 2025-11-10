package com.flyaway.deathchest.listeners;

import com.flyaway.deathchest.DeathChest;
import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage miniMessage;

    public DeathListener(DeathChest plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chestManager = plugin.getChestManager();
        this.miniMessage = MiniMessage.miniMessage();
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
            Component message = buildMessage("chest-created",
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
        // Check mob-death-only setting
        return !configManager.isMobDeathOnly() || wasKilledByMob(player);
    }

    private boolean wasKilledByMob(Player player) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent entityEvent)) {
            return false;
        }

        Entity damager = entityEvent.getDamager();

        // Check if damager is a mob
        if (damager instanceof Monster || damager instanceof Animals) {
            return true;
        }

        // Check if damager is a projectile from a mob
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Monster || shooter instanceof Animals;
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

    /**
     * Строит компонент сообщения с префиксом и плейсхолдерами
     */
    private Component buildMessage(String messageKey, String defaultMessage, Map<String, String> placeholders) {
        String message = configManager.getMessage(messageKey, defaultMessage);
        String prefix = configManager.getPrefix();

        String fullMessage = prefix + " " + message;

        // Подставляем все {ключ} → значение
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            fullMessage = fullMessage.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        // Парсим MiniMessage для цветовых тегов и форматирования
        return miniMessage.deserialize(fullMessage);
    }
}
