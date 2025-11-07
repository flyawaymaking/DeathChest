package com.flyaway.deathchest.listeners;

import com.flyaway.deathchest.DeathChest;
import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestInteractionListener implements Listener {

    private final DeathChest plugin;
    private final ConfigManager configManager;
    private final ChestManager chestManager;

    public ChestInteractionListener(DeathChest plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chestManager = plugin.getChestManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }

        Location location = block.getLocation();
        if (!chestManager.isDeathChest(location)) {
            return;
        }

        event.setCancelled(true);

        ChestManager.DeathChestData deathChest = chestManager.getDeathChest(location);
        Player player = event.getPlayer();

        // Проверка разрешения deathchest.use
        if (!player.hasPermission("deathchest.use")) {
            player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("no-permission", "&cУ вас нет прав для использования этой команды"));
            return;
        }

        // Check access permissions
        if (!chestManager.canAccessChest(player, deathChest)) {
            String message = configManager.getMessage("access-denied", "&cЭтот сундук смерти принадлежит игроку: {player}")
                .replace("{player}", deathChest.getOwnerName());
            player.sendMessage(configManager.getPrefix() + " " + message);
            return;
        }

        // Send access message if accessing someone else's chest
        if (!deathChest.getOwner().equals(player.getUniqueId())) {
            String message = configManager.getMessage("chest-accessed", "Вы открываете сундук смерти игрока: {player}")
                .replace("{player}", deathChest.getOwnerName());
            player.sendMessage(configManager.getPrefix() + " " + message);
        }

        Inventory inventory = deathChest.getInventory();
        chestManager.registerOpenInventory(inventory, location);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }

        Location location = block.getLocation();
        if (!chestManager.isDeathChest(location)) {
            return;
        }

        event.setCancelled(true);

        ChestManager.DeathChestData deathChest = chestManager.getDeathChest(location);
        Player player = event.getPlayer();

        // Проверка разрешения deathchest.use
        if (!player.hasPermission("deathchest.use")) {
            player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("no-permission", "&cУ вас нет прав для использования этой команды"));
            return;
        }

        // Check access permissions
        if (!chestManager.canAccessChest(player, deathChest)) {
            String message = configManager.getMessage("access-denied", "&cЭтот сундук смерти принадлежит игроку: {player}")
                .replace("{player}", deathChest.getOwnerName());
            player.sendMessage(configManager.getPrefix() + " " + message);
            return;
        }

        // Запрещаем ломать сундук, если он открыт другими игроками
        if (chestManager.isInventoryOpen(location)) {
            String message = configManager.getMessage("chest-in-use", "&cНельзя сломать сундук, пока его кто-то использует");
            player.sendMessage(configManager.getPrefix() + " " + message);
            return;
        }

        // Check if breaking is allowed
        if (!configManager.isPlayerBreakable()) {
            // Only allow breaking if chest is empty
            if (isInventoryEmpty(deathChest.getInventory())) {
                chestManager.removeDeathChest(location);
                player.sendMessage(configManager.getPrefix() + " " + "Сундук смерти удален.");
            } else {
                String message = configManager.getMessage("cannot-break", "&cВы не можете сломать этот сундук смерти, пока в нём есть предметы");
                player.sendMessage(configManager.getPrefix() + " " + message);
            }
            return;
        }

        // Breaking is allowed - handle item drops
        if (configManager.dropItemsWhenBroken()) {
            dropItems(deathChest.getInventory().getContents(), location);
        }

        chestManager.removeDeathChest(location);

        if (deathChest.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("chest-broken-own", "Вы сломали свой сундук смерти"));
        } else {
            String message = configManager.getMessage("chest-broken-other", "Вы сломали сундук смерти игрока {player}")
                .replace("{player}", deathChest.getOwnerName());
            player.sendMessage(configManager.getPrefix() + " " + message);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        // Находим сундук по инвентарю
        Location chestLocation = chestManager.getOpenInventories().get(inventory);
        if (chestLocation == null) {
            return;
        }

        // Убираем из отслеживания открытых инвентарей
        chestManager.unregisterOpenInventory(inventory);

        ChestManager.DeathChestData deathChest = chestManager.getDeathChest(chestLocation);
        if (deathChest == null) {
            return;
        }

        // Сохраняем изменения в инвентаре
        chestManager.updateDeathChest(deathChest);

        // Check if chest should be removed when empty
        if (configManager.removeEmptyChests() && isInventoryEmpty(inventory)) {
            // Проверяем, что больше никто не использует этот сундук
            if (!chestManager.isInventoryOpen(chestLocation)) {
                chestManager.removeDeathChest(chestLocation);
                String message = configManager.getMessage("chest-removed", "&aСундук смерти исчез, так как вы забрали все предметы");
                player.sendMessage(configManager.getPrefix() + " " + message);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (configManager.isExplosionProof()) {
            // Remove death chests from explosion list to protect them
            event.blockList().removeIf(block ->
                block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())
            );
        } else {
            // Handle explosion - drop items and remove chest
            List<Block> toRemove = new ArrayList<>();
            for (Block block : event.blockList()) {
                if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
                    Location location = block.getLocation();
                    ChestManager.DeathChestData deathChest = chestManager.getDeathChest(location);

                    if (configManager.dropItemsWhenExploded()) {
                        dropItems(deathChest.getInventory().getContents(), location);
                    }

                    toRemove.add(block);
                    chestManager.removeDeathChest(location);
                }
            }
            event.blockList().removeAll(toRemove);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (configManager.isExplosionProof()) {
            // Remove death chests from explosion list to protect them
            event.blockList().removeIf(block ->
                block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())
            );
        } else {
            // Handle explosion - drop items and remove chest
            List<Block> toRemove = new ArrayList<>();
            for (Block block : event.blockList()) {
                if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
                    Location location = block.getLocation();
                    ChestManager.DeathChestData deathChest = chestManager.getDeathChest(location);

                    if (configManager.dropItemsWhenExploded()) {
                        dropItems(deathChest.getInventory().getContents(), location);
                    }

                    toRemove.add(block);
                    chestManager.removeDeathChest(location);
                }
            }
            event.blockList().removeAll(toRemove);
        }
    }

    // Защита от горения
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    // Защита от распространения огня
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    // Защита от поршней
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private void dropItems(ItemStack[] items, Location location) {
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }
}
