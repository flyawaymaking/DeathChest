package com.flyaway.deathchest.listeners;

import com.flyaway.deathchest.DeathChest;
import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import com.flyaway.deathchest.managers.MessageManager;
import net.kyori.adventure.text.Component;
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
import java.util.Map;

public class ChestInteractionListener implements Listener {

    private final ConfigManager configManager;
    private final ChestManager chestManager;

    public ChestInteractionListener(DeathChest plugin) {
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

        if (!player.hasPermission("deathchest.use")) {
            Component message = MessageManager.buildMessage("no-permission",
                    "<red>У вас нет прав для использования этой команды");
            player.sendMessage(message);
            return;
        }

        if (!chestManager.canAccessChest(player, deathChest)) {
            Component message = MessageManager.buildMessage("access-denied",
                    "<red>Этот сундук смерти принадлежит игроку: {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
            return;
        }

        if (!deathChest.getOwner().equals(player.getUniqueId())) {
            Component message = MessageManager.buildMessage("chest-accessed",
                    "Вы открываете сундук смерти игрока: {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
        }

        Inventory inventory = deathChest.getInventory();
        chestManager.registerOpenInventory(player, inventory, location);
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

        if (!player.hasPermission("deathchest.use")) {
            Component message = MessageManager.buildMessage("no-permission",
                    "<red>У вас нет прав для использования этой команды");
            player.sendMessage(message);
            return;
        }

        if (!chestManager.canAccessChest(player, deathChest)) {
            Component message = MessageManager.buildMessage("access-denied",
                    "<red>Этот сундук смерти принадлежит игроку: {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
            return;
        }

        if (!configManager.isPlayerBreakable()) {
            if (isInventoryEmpty(deathChest.getInventory())) {
                chestManager.removeDeathChest(location);
                Component message = MessageManager.buildMessage("chest-removed",
                        "<green>Сундук смерти исчез, так как вы забрали все предметы");
                player.sendMessage(message);
            } else {
                Component message = MessageManager.buildMessage("cannot-break",
                        "<red>Вы не можете сломать этот сундук смерти, пока в нём есть предметы");
                player.sendMessage(message);
            }
            return;
        }

        if (configManager.dropItemsWhenBroken()) {
            dropItems(deathChest.getInventory().getContents(), location);
        }

        chestManager.removeDeathChest(location);

        if (deathChest.getOwner().equals(player.getUniqueId())) {
            Component message = MessageManager.buildMessage("chest-broken-own",
                    "Вы сломали свой сундук смерти");
            player.sendMessage(message);
        } else {
            Component message = MessageManager.buildMessage("chest-broken-other",
                    "Вы сломали сундук смерти игрока {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        Location chestLocation = chestManager.getLocationByInventory(inventory);
        if (chestLocation == null) {
            return;
        }

        ChestManager.DeathChestData deathChest = chestManager.getDeathChest(chestLocation);
        if (deathChest == null) {
            return;
        }

        if (configManager.removeEmptyChests() && isInventoryEmpty(inventory)) {
            chestManager.removeDeathChest(chestLocation);
            Component message = MessageManager.buildMessage("chest-removed",
                    "<green>Сундук смерти исчез, так как вы забрали все предметы");
            player.sendMessage(message);
        } else {
            chestManager.unregisterOpenInventory(player, inventory);
            chestManager.updateDeathChest(deathChest);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (configManager.isExplosionProof()) {
            event.blockList().removeIf(block ->
                    block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())
            );
        } else {
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
            event.blockList().removeIf(block ->
                    block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())
            );
        } else {
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
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST && chestManager.isDeathChest(block.getLocation())) {
            event.setCancelled(true);
        }
    }

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
