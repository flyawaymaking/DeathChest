package com.flyaway.deathchest.managers;

import com.flyaway.deathchest.DeathChest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

public class MessageManager {
    private static ConfigManager configManager;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void init(DeathChest plugin) {
        MessageManager.configManager = plugin.getConfigManager();
    }

    public static Component buildMessage(String messageKey, String defaultMessage) {
        return buildMessage(messageKey, defaultMessage, Map.of());
    }

    public static Component buildMessage(String messageKey, String defaultMessage, Map<String, String> placeholders) {
        String prefix = configManager.getPrefix();
        return miniMessage.deserialize(prefix + " ").append(buildRawMessage(messageKey, defaultMessage, placeholders));
    }

    public static Component buildRawMessage(String messageKey, String defaultMessage, Map<String, String> placeholders) {
        String message = configManager.getMessage(messageKey, defaultMessage);

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return miniMessage.deserialize(message);
    }
}
