package com.flyaway.deathchest.listeners;

import com.flyaway.deathchest.DeathChest;
import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage miniMessage;

    public ChestInteractionListener(DeathChest plugin) {
        this.configManager = plugin.getConfigManager();
        this.chestManager = plugin.getChestManager();
        this.miniMessage = MiniMessage.miniMessage();
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
            Component message = buildMessage("no-permission",
                    "<red>У вас нет прав для использования этой команды");
            player.sendMessage(message);
            return;
        }

        // Check access permissions
        if (!chestManager.canAccessChest(player, deathChest)) {
            Component message = buildMessage("access-denied",
                    "<red>Этот сундук смерти принадлежит игроку: {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
            return;
        }

        // Send access message if accessing someone else's chest
        if (!deathChest.getOwner().equals(player.getUniqueId())) {
            Component message = buildMessage("chest-accessed",
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

        // Проверка разрешения deathchest.use
        if (!player.hasPermission("deathchest.use")) {
            Component message = buildMessage("no-permission",
                    "<red>У вас нет прав для использования этой команды");
            player.sendMessage(message);
            return;
        }

        // Check access permissions
        if (!chestManager.canAccessChest(player, deathChest)) {
            Component message = buildMessage("access-denied",
                    "<red>Этот сундук смерти принадлежит игроку: {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
            return;
        }

        // Check if breaking is allowed
        if (!configManager.isPlayerBreakable()) {
            // Only allow breaking if chest is empty
            if (isInventoryEmpty(deathChest.getInventory())) {
                chestManager.removeDeathChest(location);
                Component message = buildMessage("chest-removed",
                        "<green>Сундук смерти исчез, так как вы забрали все предметы");
                player.sendMessage(message);
            } else {
                Component message = buildMessage("cannot-break",
                        "<red>Вы не можете сломать этот сундук смерти, пока в нём есть предметы");
                player.sendMessage(message);
            }
            return;
        }

        // Breaking is allowed - handle item drops
        if (configManager.dropItemsWhenBroken()) {
            dropItems(deathChest.getInventory().getContents(), location);
        }

        chestManager.removeDeathChest(location);

        if (deathChest.getOwner().equals(player.getUniqueId())) {
            Component message = buildMessage("chest-broken-own",
                    "Вы сломали свой сундук смерти");
            player.sendMessage(message);
        } else {
            Component message = buildMessage("chest-broken-other",
                    "Вы сломали сундук смерти игрока {player}",
                    Map.of("player", deathChest.getOwnerName()));
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        // Находим сундук по инвентарю
        Location chestLocation = chestManager.getLocationByInventory(inventory);
        if (chestLocation == null) {
            return;
        }

        ChestManager.DeathChestData deathChest = chestManager.getDeathChest(chestLocation);
        if (deathChest == null) {
            return;
        }

        if (configManager.removeEmptyChests() && isInventoryEmpty(inventory)) {
            // Check if chest should be removed when empty
            chestManager.removeDeathChest(chestLocation);
            Component message = buildMessage("chest-removed",
                    "<green>Сундук смерти исчез, так как вы забрали все предметы");
            player.sendMessage(message);
        } else {
            // Убираем из отслеживания открытых инвентарей
            chestManager.unregisterOpenInventory(player, inventory);
            // Сохраняем изменения в инвентаре
            chestManager.updateDeathChest(deathChest);
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

    /**
     * Строит компонент сообщения с префиксом и плейсхолдерами
     */
    private Component buildMessage(String messageKey, String defaultMessage) {
        return buildMessage(messageKey, defaultMessage, Map.of());
    }

    private Component buildMessage(String messageKey, String defaultMessage, Map<String, String> placeholders) {
        String message = configManager.getMessage(messageKey, defaultMessage);
        String prefix = configManager.getPrefix();

        String fullMessage = prefix + " " + message;

        // Подставляем все {ключ} → значение
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                fullMessage = fullMessage.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        // Парсим MiniMessage для цветовых тегов и форматирования
        return miniMessage.deserialize(fullMessage);
    }
}
