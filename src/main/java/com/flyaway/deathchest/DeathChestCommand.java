package com.flyaway.deathchest;

import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage miniMessage;

    public DeathChestCommand(DeathChest plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chestManager = plugin.getChestManager();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда может быть использована только игроками.");
            return true;
        }

        // Проверка разрешения deathchest.use
        if (!player.hasPermission("deathchest.use")) {
            Component message = buildMessage("no-permission",
                    "<red>У вас нет прав для использования этой команды");
            player.sendMessage(message);
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
                    plugin.reloadConfiguration();
                    Component message = buildMessage("reload-success",
                            "<green>Конфигурация перезагружена!");
                    player.sendMessage(message);
                } else {
                    Component message = buildMessage("no-permission",
                            "<red>У вас нет прав для использования этой команды");
                    player.sendMessage(message);
                }
                break;

            case "version":
                Component versionMessage = miniMessage.deserialize(
                        configManager.getPrefix() + " <white>DeathChest v1.1.0"
                );
                player.sendMessage(versionMessage);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        Component header = miniMessage.deserialize("<gradient:gold:white>=== Команды DeathChest ===");
        player.sendMessage(header);
        player.sendMessage(miniMessage.deserialize("<white>/deathchest list <gray>- Показать ваши сундуки смерти"));
        player.sendMessage(miniMessage.deserialize("<white>/deathchest reload <gray>- Перезагрузить конфигурацию (требуются права)"));
        player.sendMessage(miniMessage.deserialize("<white>/deathchest version <gray>- Показать версию плагина"));
        player.sendMessage(miniMessage.deserialize("<white>/deathchest help <gray>- Показать эту справку"));
    }

    private void listChests(Player player) {
        Map<Location, ChestManager.DeathChestData> chests = chestManager.getDeathChests();
        List<Component> playerChests = new ArrayList<>();

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

                Component chestInfo = miniMessage.deserialize(
                        "<yellow>Мир: <white>" + world +
                                " <yellow>X: <white>" + x +
                                " <yellow>Y: <white>" + y +
                                " <yellow>Z: <white>" + z +
                                " <gray>(" + timeAgo + " назад)"
                );
                playerChests.add(chestInfo);
            }
        }

        if (playerChests.isEmpty()) {
            Component message = buildMessage("no-chests",
                    "<green>У вас нет активных сундуков смерти");
            player.sendMessage(message);
        } else {
            Component header = miniMessage.deserialize("<gradient:gold:white>=== Ваши сундуки смерти ===");
            player.sendMessage(header);
            for (int i = 0; i < playerChests.size(); i++) {
                Component numberedEntry = miniMessage.deserialize(
                        "<white>" + (i + 1) + ". "
                ).append(playerChests.get(i));
                player.sendMessage(numberedEntry);
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

    /**
     * Строит компонент сообщения с префиксом
     */
    private Component buildMessage(String messageKey, String defaultMessage) {
        String message = configManager.getMessage(messageKey, defaultMessage);
        String prefix = configManager.getPrefix();

        // Объединяем префикс и сообщение
        String fullMessage = prefix + " " + message;

        return miniMessage.deserialize(fullMessage);
    }
}
