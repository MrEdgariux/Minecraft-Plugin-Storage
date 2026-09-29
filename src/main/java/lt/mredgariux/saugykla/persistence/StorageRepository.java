package lt.mredgariux.saugykla.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StorageRepository {
    private static final String LEGACY_FILE_NAME = "chests.yml";
    private static final String FILE_NAME = "chests_v2.yml";

    private final JavaPlugin plugin;
    private final File legacyFile;
    private final File file;

    public StorageRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        legacyFile = new File(plugin.getDataFolder(), LEGACY_FILE_NAME);
        file = new File(plugin.getDataFolder(), FILE_NAME);
    }

    public Map<Material, List<Location>> load() {
        if (!file.exists() && !migrateLegacyData()) {
            plugin.getLogger().info("No chest data found; starting with empty storage.");
            return new EnumMap<>(Material.class);
        }

        Map<Material, List<Location>> storage = new EnumMap<>(Material.class);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (String key : configuration.getKeys(false)) {
            Material material = Material.getMaterial(key);
            if (material == null) {
                plugin.getLogger().warning("Skipping unknown material in " + FILE_NAME + ": " + key);
                continue;
            }
            List<Location> locations = new ArrayList<>();
            for (String serializedLocation : configuration.getStringList(key)) {
                LocationCodec.deserialize(serializedLocation).ifPresentOrElse(
                        locations::add,
                        () -> plugin.getLogger().warning(
                                "Skipping invalid or unavailable location for " + key + ": " + serializedLocation));
            }
            if (!locations.isEmpty()) {
                storage.put(material, locations);
            }
        }
        plugin.getLogger().info("Loaded storage locations for " + storage.size() + " materials.");
        return storage;
    }

    public boolean save(Map<Material, List<Location>> storage) {
        if (!ensureDataFolder()) {
            return false;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        storage.forEach((material, locations) -> configuration.set(
                material.name(), locations.stream().map(LocationCodec::serialize).toList()));
        try {
            configuration.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + FILE_NAME + '.', exception);
            return false;
        }
    }

    public boolean delete() {
        boolean currentDeleted = !file.exists() || file.delete();
        boolean legacyDeleted = !legacyFile.exists() || legacyFile.delete();
        if (!currentDeleted || !legacyDeleted) {
            plugin.getLogger().warning("Could not delete all chest data files.");
        }
        return currentDeleted && legacyDeleted;
    }

    private boolean migrateLegacyData() {
        if (!legacyFile.exists()) {
            return false;
        }
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
        Map<Material, List<Location>> migrated = new EnumMap<>(Material.class);
        for (String key : legacy.getKeys(false)) {
            Material material = Material.getMaterial(key);
            String serializedLocation = legacy.getString(key);
            if (material == null || serializedLocation == null) {
                plugin.getLogger().warning("Skipping invalid legacy chest entry: " + key);
                continue;
            }
            LocationCodec.deserialize(serializedLocation)
                    .ifPresent(location -> migrated.put(material, List.of(location)));
        }
        boolean saved = save(migrated);
        if (saved) {
            plugin.getLogger().info("Migrated " + LEGACY_FILE_NAME + " to " + FILE_NAME + '.');
        }
        return saved;
    }

    private boolean ensureDataFolder() {
        File folder = plugin.getDataFolder();
        if (folder.isDirectory() || folder.mkdirs()) {
            return true;
        }
        plugin.getLogger().severe("Could not create plugin data directory: " + folder);
        return false;
    }
}
