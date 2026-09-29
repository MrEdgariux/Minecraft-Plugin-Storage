package lt.mredgariux.saugykla.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourceRepository {
    private static final String FILE_NAME = "resursai.yml";
    private static final String MATERIALS_KEY = "materials";

    private final JavaPlugin plugin;
    private final File file;

    public ResourceRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), FILE_NAME);
    }

    public List<ItemStack> load() {
        if (!file.exists()) {
            plugin.getLogger().info("No " + FILE_NAME + " file found; starting with empty resources.");
            return new ArrayList<>();
        }

        List<ItemStack> resources = new ArrayList<>();
        List<?> serializedItems = YamlConfiguration.loadConfiguration(file).getList(MATERIALS_KEY);
        if (serializedItems == null) {
            return resources;
        }

        for (Object value : serializedItems) {
            if (!(value instanceof Map<?, ?> rawMap)) {
                plugin.getLogger().warning("Skipping invalid resource entry in " + FILE_NAME + '.');
                continue;
            }
            try {
                resources.add(ItemStack.deserialize(stringKeyedMap(rawMap)));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "Skipping resource entry that could not be deserialized.", exception);
            }
        }
        plugin.getLogger().info("Loaded " + resources.size() + " resource stacks.");
        return resources;
    }

    public boolean save(List<ItemStack> resources) {
        if (!ensureDataFolder()) {
            return false;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(MATERIALS_KEY, resources.stream().map(ItemStack::serialize).toList());
        try {
            configuration.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + FILE_NAME + '.', exception);
            return false;
        }
    }

    private boolean ensureDataFolder() {
        File folder = plugin.getDataFolder();
        if (folder.isDirectory() || folder.mkdirs()) {
            return true;
        }
        plugin.getLogger().severe("Could not create plugin data directory: " + folder);
        return false;
    }

    private static Map<String, Object> stringKeyedMap(Map<?, ?> rawMap) {
        java.util.HashMap<String, Object> result = new java.util.HashMap<>();
        rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
}
