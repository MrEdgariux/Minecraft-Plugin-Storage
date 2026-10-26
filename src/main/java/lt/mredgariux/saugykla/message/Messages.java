package lt.mredgariux.saugykla.message;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import lt.mredgariux.messages.chat.ChatManager;
import lt.mredgariux.messages.language.LanguageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** One CoreMessages instance shared by commands, services, and listeners. */
public final class Messages {
    private final ChatManager chat;

    public Messages(JavaPlugin plugin) {
        migrateLegacyTranslations(plugin);
        LanguageManager language = new LanguageManager(plugin, MessageKey.class, "en");
        language.loadLanguages();
        String selected = plugin.getConfig().getString("language", "en");
        try {
            language.setDefaultLanguage(selected);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Cannot select language '" + selected + "'; using en: "
                    + exception.getMessage());
        }
        chat = new ChatManager(language, plugin.getServer(), null);
    }

    public void send(CommandSender sender, MessageKey key, Object... arguments) {
        chat.sendMessage(sender, key, arguments);
    }

    private static void migrateLegacyTranslations(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "langs/en.yml");
        ConfigurationSection legacy = plugin.getConfig().getConfigurationSection("translations");
        if (file.exists() || legacy == null) {
            return;
        }
        Map<String, MessageKey> keys = Map.ofEntries(
                Map.entry("console", MessageKey.CONSOLE),
                Map.entry("reset", MessageKey.RESET_SUCCESS),
                Map.entry("reset_fail", MessageKey.RESET_FAILED),
                Map.entry("unknown_item", MessageKey.UNKNOWN_ITEM),
                Map.entry("not_exist", MessageKey.ITEM_NOT_STORED),
                Map.entry("chunks_start_look_at_the_block", MessageKey.REGION_LOOK_AT_BLOCK),
                Map.entry("chunks_start_success", MessageKey.REGION_START_SELECTED),
                Map.entry("chunk_created", MessageKey.REGION_CREATED),
                Map.entry("chunk_overlaps", MessageKey.REGION_CREATE_FAILED),
                Map.entry("barrels_mixing_items", MessageKey.BARREL_WRONG_ITEM),
                Map.entry("barrels_destroyed_empty", MessageKey.BARREL_EMPTY_REMOVED),
                Map.entry("barrels_destroy_success", MessageKey.BARREL_REMOVED),
                Map.entry("barrels_destroy_fail", MessageKey.BARREL_REMOVE_FAILED),
                Map.entry("resources_barrel_not_enough", MessageKey.BARRELS_REQUIRED),
                Map.entry("resources_sign_not_enough", MessageKey.SIGNS_REQUIRED),
                Map.entry("resources_not_allowed", MessageKey.RESOURCE_NOT_ALLOWED),
                Map.entry("resources_saved", MessageKey.RESOURCES_SAVED));
        try (var stream = Objects.requireNonNull(plugin.getResource("langs/en.yml"),
                "Missing bundled langs/en.yml")) {
            YamlConfiguration translations = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            keys.forEach((oldKey, key) -> {
                if (legacy.isString(oldKey)) {
                    translations.set(key.key(), legacy.getString(oldKey).replace("%s", "%0%"));
                }
            });
            File folder = file.getParentFile();
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("Cannot create " + folder);
            }
            translations.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot migrate legacy translations to " + file, exception);
        }
    }
}
