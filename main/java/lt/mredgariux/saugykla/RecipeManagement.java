package lt.mredgariux.saugykla;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public class RecipeManagement {
    private static final String DATA_FILE_NAME = "resursai.yml";
    private final JavaPlugin plugin;
    public List<ItemStack> materials = new ArrayList<ItemStack>();

    public RecipeManagement(JavaPlugin plugin) {
        this.plugin = plugin;
        loadResources();
    }

    public void addResources(ItemStack[] resursai, Player added_by) {
        materials.clear();
        for (ItemStack itemas : resursai) {
            if (itemas == null) {
                continue;
            }

            if (itemas.getType() != Material.BARREL && itemas.getType() != Material.CHEST &&
                    !itemas.getType().toString().endsWith("_SIGN") && itemas.getType().toString().contains("HANGING")) {
                added_by.sendMessage("Nepriimtas " + itemas.getType());
                added_by.getInventory().addItem(itemas);
                continue;
            }
            if (itemas.getType() == Material.CHEST) {
                itemas.setType(Material.BARREL);
                Bukkit.getLogger().info("[Resources] CHEST paverstas į BARREL");
            }
            materials.add(itemas);
        }

        added_by.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Resursai] &aOperacija sekmingai atlikta"));

        saveResources();
    }

    public boolean takeItem(Material itemas) {
        Iterator<ItemStack> iterator = materials.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item.getType() == itemas) {
                int amount = item.getAmount();
                if (amount > 1) {
                    item.setAmount(amount - 1);
                    saveResources();
                    return false; // Item found and quantity decremented, so return false
                } else {
                    iterator.remove(); // Remove the item from the list
                    saveResources();
                    return false; // Item found and removed, so return false
                }
            }
        }
        return true; // Item not found, return true
    }


    private void saveResources() {
        YamlConfiguration config = new YamlConfiguration();
        try {

            if (!plugin.getDataFolder().exists()) {
                if (!plugin.getDataFolder().mkdirs()){
                    Bukkit.getLogger().severe("Nepavyko sukurt kaiko nx");
                    plugin.getPluginLoader().disablePlugin(plugin);
                    return;
                }
            }
            // Save each ItemStack in the materials list
            List<Map<String, Object>> items = new ArrayList<>();
            for (ItemStack item : materials) {
                items.add(item.serialize());
            }
            config.set("materials", items);

            // Save the config to the file
            File dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
            config.save(dataFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Resources] Failed to save resource data to file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadResources() {
        Bukkit.getLogger().info("[Resources] Loading resource data from file: " + DATA_FILE_NAME);
        File dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (!dataFile.exists()) {
            Bukkit.getLogger().info("No data file found");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<String, Object>> items = (List<Map<String, Object>>) config.getList("materials");
        if (items != null) {
            for (Map<String, Object> itemData : items) {
                ItemStack item = ItemStack.deserialize(itemData);
                materials.add(item);
            }
        }
        Bukkit.getLogger().info("Loaded " + materials.size() + " resources");
    }
}
