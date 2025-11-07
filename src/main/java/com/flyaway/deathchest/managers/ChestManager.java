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
        private String hologramId; // ID голограммы
        private final Location location; // Добавляем ссылку на местоположение

        public DeathChestData(DeathChest plugin, UUID owner, String ownerName, int size, Location location) {
            this(plugin, owner, ownerName, size, null, location);
        }

        public DeathChestData(DeathChest plugin, UUID owner, String ownerName, int size, String hologramId, Location location) {
            this.plugin = plugin;
            this.owner = owner;
            this.ownerName = ownerName;
            this.hologramId = hologramId;
            this.location = location;
            String title = plugin.getConfigManager().getChestTitle().replace("{player}", ownerName);
            this.inventory = plugin.getServer().createInventory(null, size, title);
            this.creationTime = System.currentTimeMillis();
        }

        // Getters & Setters
        public UUID getOwner() { return owner; }
        public String getOwnerName() { return ownerName; }
        public Inventory getInventory() { return inventory; }
        public long getCreationTime() { return creationTime; }
        public String getHologramId() { return hologramId; }
        public void setHologramId(String id) { this.hologramId = id; }
        public Location getLocation() { return location; }
    }

    public boolean createDeathChest(Player player, List<ItemStack> items, Location location) {
        try {
            Block block = location.getBlock();

            // Проверка, можно ли поставить сундук
            if (!block.getType().isAir() && block.getType() != Material.WATER && block.getType() != Material.LAVA) {
                return false;
            }

            block.setType(Material.CHEST);

            int size = Math.min(((items.size() + 8) / 9) * 9, 54);
            DeathChestData deathChest = new DeathChestData(plugin, player.getUniqueId(), player.getName(), size, location);

            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    deathChest.getInventory().addItem(item);
                }
            }

            deathChests.put(block.getLocation(), deathChest);

            if (plugin.getConfigManager().showNameOnChest()) {
                String hologramId = plugin.getHologramManager().createHologram(block.getLocation(), player.getName());
                deathChest.setHologramId(hologramId);
            }
            // Сохраняем новый сундук
            saveDeathChest(deathChest);
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

    public boolean canAccessChest(Player player, DeathChestData chest) {
        if (chest.getOwner().equals(player.getUniqueId())) {
            return true;
        }
        return plugin.getConfigManager().allowAccessOthersChests();
    }

    public void removeDeathChest(Location location) {
        DeathChestData chest = deathChests.remove(location);
        if (chest != null) {
            location.getBlock().setType(Material.AIR);

            // Удаляем из файла
            removeDeathChestFromFile(location);

            // Удаляем голограмму
            if (chest.getHologramId() != null) {
                plugin.getHologramManager().removeHologram(chest.getHologramId());
            }
        }
    }

    public void updateDeathChest(DeathChestData chest) {
        // Сохраняем изменения в файл
        saveDeathChest(chest);
    }

    private void saveDeathChest(DeathChestData chest) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(chestsFile);

        String key = getLocationKey(chest.getLocation());

        config.set(key + ".owner", chest.getOwner().toString());
        config.set(key + ".ownerName", chest.getOwnerName());
        config.set(key + ".creationTime", chest.getCreationTime());
        config.set(key + ".items", Arrays.asList(chest.getInventory().getContents()));
        config.set(key + ".hologramId", chest.getHologramId());

        try {
            config.save(chestsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить сундук смерти: " + e.getMessage());
        }
    }

    private void removeDeathChestFromFile(Location location) {
        if (!chestsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(chestsFile);
        String key = getLocationKey(location);

        config.set(key, null);

        try {
            config.save(chestsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось удалить сундук смерти из файла: " + e.getMessage());
        }
    }

    private String getLocationKey(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    public void loadChests() {
        if (!chestsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(chestsFile);
        deathChests.clear();

        for (String key : config.getKeys(false)) {
            try {
                String[] parts = key.split(";");
                if (parts.length != 4) continue;

                String world = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);

                Location loc = new Location(plugin.getServer().getWorld(world), x, y, z);

                Block block = loc.getBlock();
                if (block.getType() != Material.CHEST) {
                    block.setType(Material.CHEST);
                }

                UUID owner = UUID.fromString(config.getString(key + ".owner"));
                String ownerName = config.getString(key + ".ownerName");
                long creationTime = config.getLong(key + ".creationTime");
                String hologramId = config.getString(key + ".hologramId", null);

                List<ItemStack> items = (List<ItemStack>) config.getList(key + ".items");
                if (items == null) continue;

                int size = Math.min(((items.size() + 8) / 9) * 9, 54);
                DeathChestData chest = new DeathChestData(plugin, owner, ownerName, size, hologramId, loc);

                chest.getInventory().setContents(items.toArray(new ItemStack[0]));
                deathChests.put(loc, chest);
                if (plugin.getConfigManager().showNameOnChest() && chest.getHologramId() == null) {
                    String newHologramId = plugin.getHologramManager().createHologram(loc, ownerName);
                    chest.setHologramId(newHologramId);
                    // Сохраняем обновленные данные с hologramId
                    saveDeathChest(chest);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при загрузке сундука смерти: " + key + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Загружено " + deathChests.size() + " сундуков смерти");
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
                Location loc = entry.getKey();
                loc.getBlock().setType(Material.AIR);
                iterator.remove();

                // Удаляем из файла
                removeDeathChestFromFile(loc);

                if (chest.getHologramId() != null) {
                    plugin.getHologramManager().removeHologram(chest.getHologramId());
                }

                removedCount++;
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Удалено " + removedCount + " просроченных сундуков смерти");
        }
    }

    public void runScheduledTasks() {
        cleanupExpiredChests();
    }

    public void disableChests() {
        plugin.getLogger().info("Отключение сундуков смерти...");

        // Удаляем голограммы
        for (DeathChestData data : deathChests.values()) {
            if (data.getHologramId() != null) {
                plugin.getHologramManager().removeHologram(data.getHologramId());
            }
        }

        // Очищаем карту, чтобы освободить память
        deathChests.clear();

        plugin.getLogger().info("Сундуки смерти успешно отключены.");
    }

    public void reloadChests() {
        disableChests();
        loadChests();
        plugin.getLogger().info("Перезагрузка сундуков смерти завершена.");
    }
}
