package com.flyaway.deathchest.managers;

import com.flyaway.deathchest.DeathChest;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HologramManager {

    private final DeathChest plugin;
    private boolean enabled;

    public HologramManager(DeathChest plugin) {
        this.plugin = plugin;
        checkDependency();
    }

    private void checkDependency() {
        this.enabled = plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;

        if (isEnabled()) {
            plugin.getLogger().info("§aDecentHolograms найден — поддержка голограмм активирована.");
        } else {
            plugin.getLogger().warning("§eDecentHolograms не найден — голограммы будут отключены.");
        }
    }

    public String createHologram(Location location, String ownerName) {
        if (!isEnabled()) return null;

        Block block = location.getBlock();
        if (block.getType() != Material.CHEST) return null;

        Location holoLoc = location.clone().add(0.5, 1.5, 0.5);
        String id = "deathchest_" + UUID.randomUUID().toString().substring(0, 8) + "_" +
                location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

        // Удаляем старую голограмму если существует
        if (DHAPI.getHologram(id) != null) {
            DHAPI.removeHologram(id);
        }

        Hologram hologram = DHAPI.createHologram(id, holoLoc, getHologramLines(ownerName));
        hologram.showAll();

        return id;
    }

    public void removeHologram(String id) {
        if (!isEnabled() || id == null) return;
        Hologram hologram = DHAPI.getHologram(id);
        if (hologram != null) {
            hologram.delete();
        }
    }

    public void updateHologram(String id, String ownerName) {
        if (!isEnabled() || id == null) return;

        Hologram hologram = DHAPI.getHologram(id);
        if (hologram != null) {
            DHAPI.setHologramLines(hologram, getHologramLines(ownerName));
        }
    }

    private List<String> getHologramLines(String ownerName) {
        return Arrays.asList(
                "§e☠ §6Сундук смерти §e☠",
                "§7Игрока: §f" + ownerName
        );
    }

    /**
     * Перезагружает менеджер голограмм
     * - Проверяет доступность DecentHolograms
     * - Восстанавливает голограммы для всех активных сундуков
     */
    public void reload() {
        plugin.getLogger().info("Перезагрузка менеджера голограмм...");

        // Проверяем доступность DecentHolograms
        boolean wasEnabled = isEnabled();
        checkDependency();

        if (!isEnabled()) {
            plugin.getLogger().warning("DecentHolograms не доступен - голограммы отключены");
            return;
        }

        // Если плагин был только что включен, восстанавливаем все голограммы
        if (isEnabled() && !wasEnabled) {
            plugin.getLogger().info("DecentHolograms снова доступен - восстанавливаем голограммы...");
            restoreAllHolograms();
        } else if (isEnabled()) {
            plugin.getLogger().info("DecentHolograms доступен - голограммы активны");
        }
    }

    /**
     * Восстанавливает голограммы для всех активных сундуков смерти
     */
    private void restoreAllHolograms() {
        ChestManager chestManager = plugin.getChestManager();
        int restoredCount = 0;

        for (ChestManager.DeathChestData chest : chestManager.getDeathChests().values()) {
            Location location = chest.getLocation();
            Block block = location.getBlock();

            // Проверяем, что блок все еще сундук
            if (block.getType() != Material.CHEST) {
                continue;
            }

            // Если у сундука нет голограммы, но она должна быть отображена
            if (chest.getHologramId() == null && plugin.getConfigManager().showNameOnChest()) {
                String hologramId = createHologram(location, chest.getOwnerName());
                chest.setHologramId(hologramId);
                restoredCount++;
            }
        }

        if (restoredCount > 0) {
            plugin.getLogger().info("Восстановлено " + restoredCount + " голограмм для сундуков смерти");
        }
    }

    /**
     * Проверяет, доступен ли менеджер голограмм
     */
    public boolean isEnabled() {
        return enabled;
    }
}
