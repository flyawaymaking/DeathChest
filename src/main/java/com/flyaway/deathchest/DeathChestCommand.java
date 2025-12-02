package com.flyaway.deathchest;

import com.flyaway.deathchest.managers.ChestManager;
import com.flyaway.deathchest.managers.ConfigManager;
import com.flyaway.deathchest.managers.MessageManager;
import net.kyori.adventure.text.Component;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageManager.buildMessage("player-only",
                    "<red>Эта команда может быть использована только игроками."));
            return true;
        }

        if (!player.hasPermission("deathchest.use")) {
            player.sendMessage(MessageManager.buildMessage("no-permission",
                    "<red>У вас нет прав для использования этой команды"));
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
                    MessageManager.init(plugin);
                    player.sendMessage(MessageManager.buildMessage("reload-success",
                            "<green>Конфигурация перезагружена!"));
                } else {
                    player.sendMessage(MessageManager.buildMessage("no-permission",
                            "<red>У вас нет прав для использования этой команды"));
                }
                break;

            case "version":
                String version = plugin.getPluginMeta().getVersion();
                player.sendMessage(MessageManager.buildMessage("version",
                        "v" + version, Map.of("version", version)));
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageManager.buildRawMessage("help", "<red>help-msg not found", null));
    }

    private void listChests(Player player) {
        Map<Location, ChestManager.DeathChestData> chests = chestManager.getDeathChests();
        List<Component> playerChests = new ArrayList<>();

        for (Map.Entry<Location, ChestManager.DeathChestData> entry : chests.entrySet()) {
            Location location = entry.getKey();
            ChestManager.DeathChestData chest = entry.getValue();

            if (chest.getOwner().equals(player.getUniqueId())) {
                String world = location.getWorld().getName();
                String x = String.valueOf(location.getBlockX());
                String y = String.valueOf(location.getBlockY());
                String z = String.valueOf(location.getBlockZ());

                Duration duration = Duration.between(
                        Instant.ofEpochMilli(chest.getCreationTime()),
                        Instant.now()
                );

                String timeAgo = formatDuration(duration);

                playerChests.add(MessageManager.buildRawMessage("list-format",
                        "<red>list-format not found", Map.of(
                                "world", world, "x", x, "y", y, "z", z, "time", timeAgo)));
            }
        }

        if (playerChests.isEmpty()) {
            player.sendMessage(MessageManager.buildMessage("no-chests", "<green>У вас нет активных сундуков смерти"));
        } else {
            player.sendMessage(MessageManager.buildRawMessage("list-header",
                    "<gradient:gold:white>=== Ваши сундуки смерти ===</gradient>", null));
            for (int i = 0; i < playerChests.size(); i++) {
                player.sendMessage(MessageManager.buildRawMessage("list-numbered",
                        "<white>" + (i + 1) + ". ", Map.of("number", String.valueOf(i + 1))
                ).append(playerChests.get(i)));
            }
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%d %s, %d %s",
                    days, configManager.getTimeAgo("days"),
                    hours, configManager.getTimeAgo("hours"));
        } else if (hours > 0) {
            return String.format("%d %s, %d %s",
                    hours, configManager.getTimeAgo("hours"),
                    minutes, configManager.getTimeAgo("minutes"));
        } else {
            return String.format("%d %s",
                    minutes, configManager.getTimeAgo("minutes"));
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
