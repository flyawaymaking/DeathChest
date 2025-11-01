package com.flyaway.deathchest.managers;

import com.flyaway.deathchest.DeathChest;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class HologramManager {

    private final DeathChest plugin;
    private final boolean enabled;

    public HologramManager(DeathChest plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;

        if (enabled) {
            plugin.getLogger().info("§aDecentHolograms найден — поддержка голограмм активирована.");
        } else {
            plugin.getLogger().warning("§eDecentHolograms не найден — голограммы будут отключены.");
        }
    }

    public String createHologram(Location location, String ownerName) {
        if (!enabled) return null;

        Block block = location.getBlock();
        if (block.getType() != Material.CHEST) return null;

        Location holoLoc = location.clone().add(0.5, 1.5, 0.5);
        String id = "deathchest_" + ownerName + "_" +
                location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();

        if (DHAPI.getHologram(id) != null) {
            DHAPI.removeHologram(id);
        }

        Hologram hologram = DHAPI.createHologram(id, holoLoc, List.of(
                "§e☠ §6Сундук смерти §e☠",
                "§7Игрока: §f" + ownerName
        ));
        hologram.showAll();

        return id;
    }

    public void removeHologram(String id) {
        if (!enabled || id == null) return;
        Hologram hologram = DHAPI.getHologram(id);
        if (hologram != null) hologram.delete();
    }
}
