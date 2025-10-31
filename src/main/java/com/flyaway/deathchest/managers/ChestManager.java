package com.flyaway.deathchest.managers;

import com.flyaway.deathchest.DeathChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChestManager {

    private final DeathChest plugin;
    private final Map<Location, DeathChestData> deathChests;
    private final File chestsFile;

    public ChestManager(DeathChest plugin) {
        this.plugin = plugin;
        this.deathChests = new HashMap<>();
        this.chestsFile = new File(plugin.getDataFolder(), "chests.yml");
    }

    public static class DeathChestData {
        private final UUID owner;
        private final String ownerName;
        private final Inventory inventory;
        private final long creationTime;
        private final DeathChest plugin;

        public DeathChestData(DeathChest plugin, UUID owner, String ownerName, int size) {
            this.plugin = plugin;
            this.owner = owner;
            this.ownerName = ownerName;
            String title = plugin.getConfigManager().getChestTitle().replace("{player}", ownerName);
            this.inventory = plugin.getServer().createInventory(null, size, title);
            this.creationTime = System.currentTimeMillis();
        }

        // Getters
        public UUID getOwner() { return owner; }
        public String getOwnerName() { return ownerName; }
        public Inventory getInventory() { return inventory; }
        public long getCreationTime() { return creationTime; }
    }

    public boolean createDeathChest(Player player, List<ItemStack> items, Location location) {
        try {
            Block block = location.getBlock();

            // Check if location is suitable
            if (!block.getType().isAir() && block.getType() != Material.WATER && block.getType() != Material.LAVA) {
                return false;
            }

            // Create chest
            block.setType(Material.CHEST);

            // Create death chest object
            int size = Math.min(((items.size() + 8) / 9) * 9, 54); // Round up to nearest multiple of 9, max 54

            DeathChestData deathChest = new DeathChestData(plugin, player.getUniqueId(), player.getName(), size);

            // Add items to inventory
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    deathChest.getInventory().addItem(item);
                }
            }

            // Store chest
            deathChests.put(block.getLocation(), deathChest);

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось создать сундук смерти для " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public DeathChestData getDeathChest(Location location) {
        return deathChests.get(location);
    }

    public boolean isDeathChest(Location location) {
        return deathChests.containsKey(location);
    }

    public void removeDeathChest(Location location) {
        DeathChestData chest = deathChests.remove(location);
        if (chest != null) {
            location.getBlock().setType(Material.AIR);
        }
    }

    public boolean canAccessChest(Player player, DeathChestData chest) {
        if (chest.getOwner().equals(player.getUniqueId())) {
            return true;
        }
        return plugin.getConfigManager().allowAccessOthersChests();
    }

    public void loadChests() {
        if (!chestsFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(chestsFile);
        deathChests.clear();

        for (String key : config.getKeys(false)) {
            try {
                String[] parts = key.split(";");
                if (parts.length != 4) continue;

                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);

                // Load chest data from config
                UUID owner = UUID.fromString(config.getString(key + ".owner"));
                String ownerName = config.getString(key + ".ownerName");
                long creationTime = config.getLong(key + ".creationTime");

                // Create inventory
                List<ItemStack> items = (List<ItemStack>) config.getList(key + ".items");
                if (items == null) continue;

                int size = Math.min(((items.size() + 8) / 9) * 9, 54);
                DeathChestData deathChest = new DeathChestData(plugin, owner, ownerName, size);

                // Convert list to array and set contents
                ItemStack[] contents = items.toArray(new ItemStack[0]);
                deathChest.getInventory().setContents(contents);

                deathChests.put(location, deathChest);

                // Restore chest block if chunk is loaded
                if (location.getChunk().isLoaded()) {
                    location.getBlock().setType(Material.CHEST);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось загрузить сундук смерти: " + key + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Загружено " + deathChests.size() + " сундуков смерти");
    }

    public void saveChests() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<Location, DeathChestData> entry : deathChests.entrySet()) {
            Location loc = entry.getKey();
            DeathChestData chest = entry.getValue();

            String key = loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();

            config.set(key + ".owner", chest.getOwner().toString());
            config.set(key + ".ownerName", chest.getOwnerName());
            config.set(key + ".creationTime", chest.getCreationTime());
            config.set(key + ".items", Arrays.asList(chest.getInventory().getContents()));
        }

        try {
            config.save(chestsFile);
            plugin.getLogger().info("Сохранено " + deathChests.size() + " сундуков смерти");
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить сундуки смерти: " + e.getMessage());
        }
    }

    public Map<Location, DeathChestData> getDeathChests() {
        return Collections.unmodifiableMap(deathChests);
    }

    // Очистка просроченных сундуков
    public void cleanupExpiredChests() {
        int expirationTime = plugin.getConfigManager().getExpirationTime();
        if (expirationTime <= 0) return;

        long currentTime = System.currentTimeMillis();
        long expirationMillis = expirationTime * 60 * 1000L;

        Iterator<Map.Entry<Location, DeathChestData>> iterator = deathChests.entrySet().iterator();
        int removedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<Location, DeathChestData> entry = iterator.next();
            DeathChestData chest = entry.getValue();

            if (currentTime - chest.getCreationTime() > expirationMillis) {
                // Время истекло - удаляем сундук
                Location location = entry.getKey();
                location.getBlock().setType(Material.AIR);
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Удалено " + removedCount + " просроченных сундуков смерти");
        }
    }
}
