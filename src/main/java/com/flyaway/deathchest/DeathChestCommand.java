package com.flyaway.deathchest;

import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeathChestCommand implements CommandExecutor, TabCompleter {

    private final DeathChest plugin;
    private final ConfigManager configManager;
    private final ChestManager chestManager;

    public DeathChestCommand(DeathChest plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chestManager = plugin.getChestManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда может быть использована только игроками.");
            return true;
        }

        Player player = (Player) sender;

        // Проверка разрешения deathchest.use
        if (!player.hasPermission("deathchest.use")) {
            player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("no-permission", "&cУ вас нет прав для использования этой команды"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                listChests(player);
                break;

            case "reload":
                if (player.hasPermission("deathchest.reload")) {
                    configManager.reloadConfig();
                    player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("reload-success", "&aКонфигурация перезагружена!"));
                } else {
                    player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("no-permission", "&cУ вас нет прав для использования этой команды"));
                }
                break;

            case "version":
                player.sendMessage(configManager.getPrefix() + " §fDeathChest v1.0.0");
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Команды DeathChest ===");
        player.sendMessage("§f/deathchest list §7- Показать ваши сундуки смерти");
        player.sendMessage("§f/deathchest reload §7- Перезагрузить конфигурацию (требуются права)");
        player.sendMessage("§f/deathchest version §7- Показать версию плагина");
        player.sendMessage("§f/deathchest help §7- Показать эту справку");
    }

    private void listChests(Player player) {
        Map<Location, ChestManager.DeathChestData> chests = chestManager.getDeathChests();
        List<String> playerChests = new ArrayList<>();

        for (Map.Entry<Location, ChestManager.DeathChestData> entry : chests.entrySet()) {
            Location location = entry.getKey();
            ChestManager.DeathChestData chest = entry.getValue();

            if (chest.getOwner().equals(player.getUniqueId())) {
                String world = location.getWorld().getName();
                int x = location.getBlockX();
                int y = location.getBlockY();
                int z = location.getBlockZ();

                // Calculate time since creation
                Duration duration = Duration.between(
                    Instant.ofEpochMilli(chest.getCreationTime()),
                    Instant.now()
                );

                String timeAgo = formatDuration(duration);
                playerChests.add(String.format("§eМир: §f%s §eX: §f%d §eY: §f%d §eZ: §f%d §7(%s назад)",
                    world, x, y, z, timeAgo));
            }
        }

        if (playerChests.isEmpty()) {
            player.sendMessage(configManager.getPrefix() + " " + configManager.getMessage("no-chests", "&aУ вас нет активных сундуков смерти"));
        } else {
            player.sendMessage("§6=== Ваши сундуки смерти ===");
            for (int i = 0; i < playerChests.size(); i++) {
                player.sendMessage((i + 1) + ". " + playerChests.get(i));
            }
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%d %s и %d %s",
                days, days == 1 ? "день" : "дней",
                hours, hours == 1 ? "час" : "часов");
        } else if (hours > 0) {
            return String.format("%d %s и %d %s",
                hours, hours == 1 ? "час" : "часов",
                minutes, minutes == 1 ? "минута" : "минут");
        } else {
            return String.format("%d %s",
                minutes, minutes == 1 ? "минута" : "минут");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            completions.add("help");
            completions.add("version");
            if (sender.hasPermission("deathchest.reload")) {
                completions.add("reload");
            }
        }

        return completions;
    }
}
