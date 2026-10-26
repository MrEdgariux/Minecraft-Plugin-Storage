package lt.mredgariux.saugykla.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class MessagesTest {
    @TempDir
    Path folder;

    private JavaPlugin plugin;
    private YamlConfiguration config;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        config = new YamlConfiguration();
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getServer()).thenReturn(mock(Server.class));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("SaugyklaMessagesTest"));
        when(plugin.getResource(anyString())).thenAnswer(invocation ->
                getClass().getClassLoader().getResourceAsStream(invocation.getArgument(0)));
    }

    @Test
    void bundledCatalogDefinesEveryKeyAndSendsFormattedComponents() {
        Messages messages = new Messages(plugin);
        YamlConfiguration english = YamlConfiguration.loadConfiguration(folder.resolve("langs/en.yml").toFile());
        Set<String> expected = Arrays.stream(MessageKey.values()).map(MessageKey::key).collect(Collectors.toSet());
        Set<String> actual = english.getKeys(true).stream()
                .filter(key -> !english.isConfigurationSection(key)).collect(Collectors.toSet());
        assertEquals(expected, actual);

        CommandSender console = mock(CommandSender.class);
        messages.send(console, MessageKey.DEBUG_REGION, "id", "start", "end", 3);
        assertEquals("- id - start - end - 3", sentText(console));

        Player player = mock(Player.class);
        messages.send(player, MessageKey.STORAGE_VERTICAL_FULL, "OAK_LOG");
        assertTrue(sentText(player).contains("OAK_LOG"));
        assertFalse(sentText(player).contains("%0%"));
    }

    @Test
    void selectedLanguageFallsBackForMissingKeys() throws IOException {
        Files.createDirectories(folder.resolve("langs"));
        Files.writeString(folder.resolve("langs/lt.yml"), "resources:\n  saved: '&aIšsaugota!'\n");
        config.set("language", "lt");
        Messages messages = new Messages(plugin);

        CommandSender translated = mock(CommandSender.class);
        messages.send(translated, MessageKey.RESOURCES_SAVED);
        assertEquals("Išsaugota!", sentText(translated));

        CommandSender fallback = mock(CommandSender.class);
        messages.send(fallback, MessageKey.CONSOLE);
        assertEquals("You cannot do that!", sentText(fallback));
    }

    @Test
    void existingLanguageEditsSurviveRestartAndLegacyMigration() throws IOException {
        new Messages(plugin);
        Path file = folder.resolve("langs/en.yml");
        YamlConfiguration english = YamlConfiguration.loadConfiguration(file.toFile());
        english.set("command.console", "&bCustom console");
        english.save(file.toFile());
        config.set("translations.console", "Old config value");
        Messages restarted = new Messages(plugin);
        CommandSender console = mock(CommandSender.class);
        restarted.send(console, MessageKey.CONSOLE);
        assertEquals("Custom console", sentText(console));
    }

    @Test
    void legacyConfigValuesMigrateWithPositionalArguments() {
        config.set("translations.console", "&eOnly players");
        config.set("translations.resources_not_allowed", "&cNo %s here");
        Messages messages = new Messages(plugin);
        CommandSender console = mock(CommandSender.class);
        messages.send(console, MessageKey.CONSOLE);
        assertEquals("Only players", sentText(console));

        Player player = mock(Player.class);
        messages.send(player, MessageKey.RESOURCE_NOT_ALLOWED, "DIRT");
        assertEquals("No DIRT here", sentText(player));
        assertEquals("&eOnly players", config.getString("translations.console"));
    }

    @Test
    void unavailableLanguageFallsBackToEnglish() {
        config.set("language", "missing");
        Messages messages = new Messages(plugin);
        CommandSender console = mock(CommandSender.class);
        messages.send(console, MessageKey.CONSOLE);
        assertEquals("You cannot do that!", sentText(console));
    }

    private String sentText(CommandSender sender) {
        ArgumentCaptor<Component> component = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(component.capture());
        return PlainTextComponentSerializer.plainText().serialize(component.getValue());
    }
}
