package lt.mredgariux.saugykla.persistence;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import lt.mredgariux.saugykla.model.StorageRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionRepository {
    private static final String FILE_NAME = "chunks.yml";

    private final JavaPlugin plugin;
    private final File file;

    public RegionRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), FILE_NAME);
    }

    public Map<UUID, StorageRegion> load() {
        Map<UUID, StorageRegion> regions = new LinkedHashMap<>();
        if (!file.exists()) {
            plugin.getLogger().info("No " + FILE_NAME + " file found; starting with no storage regions.");
            return regions;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (String key : configuration.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String worldName = configuration.getString(key + ".start.world");
                World world = worldName == null ? null : Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Skipping region " + key + " because its world is unavailable.");
                    continue;
                }
                Location start = readLocation(configuration, key + ".start", world);
                Location end = readLocation(configuration, key + ".end", world);
                regions.put(id, new StorageRegion(id, start, end));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().log(Level.WARNING, "Skipping invalid region entry: " + key, exception);
            }
        }
        plugin.getLogger().info("Loaded " + regions.size() + " storage regions.");
        return regions;
    }

    public boolean save(Map<UUID, StorageRegion> regions) {
        if (!ensureDataFolder()) {
            return false;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        regions.forEach((id, region) -> {
            writeLocation(configuration, id + ".start", region.start());
            writeLocation(configuration, id + ".end", region.end());
            configuration.set(id + ".size", region.size());
        });
        try {
            configuration.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + FILE_NAME + '.', exception);
            return false;
        }
    }

    public boolean delete() {
        if (!file.exists() || file.delete()) {
            return true;
        }
        plugin.getLogger().warning("Could not delete " + FILE_NAME + '.');
        return false;
    }

    private static Location readLocation(YamlConfiguration configuration, String path, World world) {
        return new Location(world,
                configuration.getDouble(path + ".x"),
                configuration.getDouble(path + ".y"),
                configuration.getDouble(path + ".z"));
    }

    private static void writeLocation(YamlConfiguration configuration, String path, Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Cannot save a region location without a world");
        }
        configuration.set(path + ".world", world.getName());
        configuration.set(path + ".x", location.getX());
        configuration.set(path + ".y", location.getY());
        configuration.set(path + ".z", location.getZ());
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
