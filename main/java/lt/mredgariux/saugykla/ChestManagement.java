package lt.mredgariux.saugykla;

import lt.mredgariux.saugykla.datasets.Chunk;
import lt.mredgariux.saugykla.utils.calculations;
import lt.mredgariux.saugykla.utils.chat;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class ChestManagement {
    private static final String DATA_FILE_NAME = "chests.yml";
    private static final String DATA_FILE_new_NAME = "chests_v2.yml";
    private static final String DATA_CHUNK_FILE = "chunks.yml";
    private final JavaPlugin plugin;
    private final Map<Material, List<Location>> existingChests = new HashMap<>();

    private final Map<UUID, Chunk> newChunks = new HashMap<>();
    private final List<ItemStack> signs = new ArrayList<>();

    private void loadSigns() {
        signs.clear();
        for (ItemStack itemas : ((main) plugin).recipeManagement.materials) {
            if (itemas.getType().toString().endsWith("_SIGN")) {
                signs.add(itemas);
            }
        }
    }

    public void dropItems(Player player, ItemStack item) {
        World world = player.getWorld();
        Location location = player.getLocation();
        world.dropItemNaturally(location, item);
    }

    public ChestManagement(JavaPlugin plugin) {
        this.plugin = plugin;
        loadChunks();
        loadChestData();
    }

    public boolean is_chest_exists(Location locationas) {
        return existingChests.values().stream().flatMap(List::stream).anyMatch(loc -> loc.equals(locationas));
    }

    public boolean create_chunk(Location start, Location end) {
        // Check if the new chunk collides with existing chunks
        for (Map.Entry<UUID, Chunk> entry : newChunks.entrySet()) {
            Chunk c = entry.getValue();
            Location existingStart = c.getStart();
            Location existingEnd = c.getEnd();

            if (calculations.isColliding(start, end, existingStart, existingEnd)) {
                Bukkit.getLogger().warning("New chunk is colliding with " + c.getId());
                return false; // Collision detected
            }
        }

        UUID unid = UUID.randomUUID();
        Chunk nc = new Chunk(unid, start, end);
        newChunks.put(unid, nc);

        saveChunks();

        Bukkit.getLogger().info("New chunk with ID " + unid + " created");
        return true;
    }

    public Material getMaterial(Location location) {
        for (Map.Entry<Material, List<Location>> entry : existingChests.entrySet()) {
            List<Location> locations = entry.getValue();
            for (Location loc : locations) {
                if (loc.equals(location)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public boolean materialAre(Material material) {
        return existingChests.containsKey(material);
    }

    public void delete_chest(Material material, Location location) {
        List<Location> locations = existingChests.get(material);
        if (locations != null) {
            locations.remove(location);
            if (locations.isEmpty()) {
                existingChests.remove(material);
            }
            saveChestData();
        }
        saveChestData();
    }

    public void highlight_chest(Material material, Player player) {
        if (!existingChests.containsKey(material)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &cNesaugote šio daikto saugyklose"));
            return;
        }
        List<Location> locations = existingChests.get(material);
        Location last_location = locations.getLast();
        Location signLocation = last_location.clone().subtract(1,0,0);
        if (!signLocation.getBlock().getType().toString().endsWith("_SIGN")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &cRadau vieta, tačiau negaliu paryškinti ženklo, kadangi tokio neradau :("));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &a" + last_location.getX() + " " + last_location.getY() + " " + last_location.getZ()));
            return;
        }

        Sign signState = (Sign) signLocation.getBlock().getState();
        signState.getSide(Side.FRONT).setGlowingText(true);
        signState.getSide(Side.FRONT).setColor(DyeColor.BLUE);
        signState.update();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &aStorage item " + material + " highlight has been added"));

        // Nuėmimas xD
        new BukkitRunnable() {
            @Override
            public void run() {
                Sign signState = (Sign) signLocation.getBlock().getState();
                signState.getSide(Side.FRONT).setGlowingText(false);
                signState.getSide(Side.FRONT).setColor(DyeColor.BLACK);
                signState.update();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &aStorage item " + material + " highlight has been hidden"));
            }
        }.runTaskLater(plugin, 30*20L);
    }

    public void placeItemsInChests(ItemStack[] items, Player player) {
        for (ItemStack item : items) {
            if (item == null) continue;
            Material itemType = item.getType();



            // Check if a chest with the item already exists
            if (existingChests.containsKey(itemType)) {
                Bukkit.getLogger().info("[Storage] Adding " + itemType + " to the barrels");
                addItemToChest(existingChests.get(itemType).getFirst(), item, player);
            } else {
                if (newChunks.isEmpty()) {
                    Bukkit.getLogger().severe("No available chunks found.");
                    dropItems(player, item);
                    player.sendMessage(chat.color("&b - &cNo chunks has been found &b(&a/s chunks&b)"));
                    continue;
                }
                loadSigns();
                if (((main) plugin).recipeManagement.takeItem(player, Material.BARREL)) {
                    Bukkit.getLogger().severe("No usable barrels found in resources.");
                    dropItems(player, item);
                    player.sendMessage(chat.color("&b - &cAdd barrels &b(&a/s r&b)"));
                    continue;
                }

                if (signs.isEmpty()) {
                    Bukkit.getLogger().severe("No usable signs found in resources.");
                    dropItems(player, item);
                    player.sendMessage(chat.color("&b - &cAdd signs &b(&a/s r&b)"));
                    continue;
                }

                // Create a new chest and place the item
                Location chestLocation = findNextAvailableLocation();
                if (chestLocation != null) {
                    addItemToNewChest(chestLocation, item, player);
                    existingChests.computeIfAbsent(item.getType(), k -> new ArrayList<>()).add(chestLocation);
                } else {
                    Bukkit.getLogger().severe("No available location for placing a new barrels.");
                    dropItems(player, item);
                    player.sendMessage(chat.color("&b - &cNone of the chunks has enough space to place new barrels &b(&a/s chunks&b)"));
                }
            }

            saveChestData();
        }
    }

    public boolean deleteStoreFile() {
        File file = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (file.exists()) {
            if (file.delete()) {
                for (List<Location> location : existingChests.values()) {
                    for (Location loc : location) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
                existingChests.clear();
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean deleteChunkFile() {
        File file = new File(plugin.getDataFolder(), DATA_CHUNK_FILE);
        if (file.exists()) {
            if (file.delete()) {
                newChunks.clear();
                return true;
            }
            return false;
        }
        return false;
    }

    private void addItemToChest(Location chestLocation, ItemStack item, Player player) {
        Block block = chestLocation.getBlock();
        if (block.getType() == Material.BARREL) {
            Barrel chest = (Barrel) block.getState();
            Inventory inventory = chest.getInventory();

            Map<Integer, ItemStack> leftover = inventory.addItem(item);
            if (!leftover.isEmpty()) {
                loadSigns();
                if (((main) plugin).recipeManagement.takeItem(player, Material.BARREL)) {
                    Bukkit.getLogger().severe("No available barrels.");
                    dropItems(player, item);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c " + item.getType() + " buvo grąžintas, nes nepakanka barreliu jiems sudėti &a(/s r)"));
                    return;
                }

                if (signs.isEmpty()) {
                    Bukkit.getLogger().severe("No available signs.");
                    dropItems(player, item);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + item.getType() + " buvo grąžintas, nes nepakanka ženklų. &a(/s r)"));
                    return;
                }
                Location aboveLocation = chestLocation.clone().add(0, 1, 0);
                if (aboveLocation.getBlock().getType() != Material.AIR) {
                    player.sendMessage(chat.color("&cThe block is disallowing to place barrel"));
                    dropItems(player, item);
                    return;
                }
                addItemToNewChest(aboveLocation, leftover.values().iterator().next(), player);
                existingChests.computeIfAbsent(item.getType(), k -> new ArrayList<>()).add(aboveLocation);
            }
        } else {
            Bukkit.getLogger().warning("Location " + chestLocation + " does not contain a chest.");
            dropItems(player, item);
            player.sendMessage("Item " + item.getType() + " added back to the inventory because it does not have barrel to be put in.");
        }
    }


    private void addItemToNewChest(Location chestLocation, ItemStack item, Player player) {
        Block block = chestLocation.getBlock();
        block.setType(Material.BARREL);

        Directional directional = (Directional) block.getBlockData();
        directional.setFacing(BlockFace.UP);
        block.setBlockData(directional);
        BlockState state = block.getState();
        if (state instanceof Barrel) {
            Barrel chest = (Barrel) block.getState();
            Inventory inventory = chest.getInventory();

            Map<Integer, ItemStack> leftover = inventory.addItem(item);
            if (!leftover.isEmpty()) {
                Bukkit.getLogger().warning("Failed to add item to barrel at " + chestLocation + ". Barrel is full (somehow).");
                dropItems(player, item);
            }
            SignPlace(chestLocation, item, player);
        } else {
            Bukkit.getLogger().severe("Failed to create a Barrel at " + chestLocation);
            dropItems(player, item);
        }
    }

    private void SignPlace(Location loc, ItemStack item, Player playeriui) {
        Location signLocation = loc.clone().subtract(1, 0, 0);
        Block signB = signLocation.getBlock();

        if (signB.getType().toString().endsWith("_SIGN") || !signB.getType().equals(Material.AIR)) {
            Bukkit.getLogger().warning("Sign at location " + signLocation + " could not be placed because there's already a block.");
            return;
        }

        loadSigns();

        if (signs.isEmpty()) {
            Bukkit.getLogger().info("Nebėr ženklų blet");
            playeriui.sendMessage("Nebeuztenka ženklu karoče xD");
            return;
        }
        Iterator<ItemStack> iterator = signs.iterator();
        while (iterator.hasNext()) {
            ItemStack itemas = iterator.next();
            Material signWallas = calculations.SignToWallSign(itemas.getType());
            if (signWallas == null) {
                Bukkit.getLogger().severe("Failed to find corresponding wall sign material for " + itemas.getType());
                continue; // Skip this item and proceed to the next
            }

            // Create the wall sign and set its facing direction
            signB.setType(signWallas);
            BlockFace veidas = loc.getBlock().getFace(signB);
            if (veidas == null) {
                Bukkit.getLogger().warning("Couldn't find a face of the sign");
                return;
            }
            WallSign walsign = (WallSign) signB.getBlockData();
            walsign.setFacing(veidas);
            signB.setBlockData(walsign);

            // Ugh :)
            ((main) plugin).recipeManagement.takeItem(playeriui, itemas.getType());

            Sign sign = (Sign) signB.getState();
            String[] itemName = splitStringToFit(item.getType().name().replace('_', ' '));
            if (itemName.length > 3) {
                Bukkit.getLogger().warning("All text cannot fit to sign.");
                return;
            }
            for (int i = 0; i < itemName.length; i++) {
                sign.getSide(Side.FRONT).setLine(i, itemName[i]);
            }
            sign.update();

            break;
        }
    }

    private String[] splitStringToFit(String text) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");

        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > 15) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder();
            }
            currentLine.append(word).append(" ");
        }
        // Add the last line
        lines.add(currentLine.toString().trim());

        // Limit to maxLines
        return lines.subList(0, Math.min(lines.size(), 4)).toArray(String[]::new);
    }

    public Location findNextAvailableLocation() {
        for (Map.Entry<UUID, Chunk> entry : newChunks.entrySet()) {
            Chunk c = entry.getValue();
            Location start = c.getStart();
            Location end = c.getEnd();
            Location availableLocation = findAvailableLocationInChunk(start, end);
            if (availableLocation != null) {
                return availableLocation;
            }
        }
        return null;
    }

    private Location findAvailableLocationInChunk(Location start, Location end) {
        // Iterate over the coordinates with steps to create the pattern
        int startX = start.getBlockX();
        int startZ = start.getBlockZ();
        int endX = end.getBlockX();
        int endZ = end.getBlockZ();

        for (int x = startX; x <= endX; x += 2) { // X with 1 block space
            for (int z = startZ; z <= endZ; z++) { // No spacing on Z axis
                // Ensure we don't go out of bounds and check for 2x2 availability
                if (is2x2Available(start.getWorld().getName(), x, z)) {
                    return new Location(start.getWorld(), x, 10, z);
                }
            }
        }
        return null;
    }

    private boolean is2x2Available(String worldName, int baseX, int baseZ) {
        for (int z = baseZ; z < baseZ + 1; z++) {
            Location loc = new Location(Bukkit.getWorld(worldName), baseX, 10, z);
            if (loc.getBlock().getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }


    // Save data - Migration from old version to new one
    public boolean migrateChestData() {
        File dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        File dataFileNew = new File(plugin.getDataFolder(), DATA_FILE_new_NAME);
        if (!dataFile.exists()) {
            Bukkit.getLogger().info("[Migration] No file was found to migrate from, not loading any data.");
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        YamlConfiguration newConfig = new YamlConfiguration();

        for (String key : config.getKeys(false)) {
            Material material = Material.getMaterial(key);
            String serializedLocation = config.getString(key);

            if (material != null && serializedLocation != null) {
                List<String> locationsList = new ArrayList<>();
                locationsList.add(serializedLocation); // Adding the single location to the list

                // Add the new list of locations to the new config
                newConfig.set(material.name(), locationsList);
            }
        }

        // Save the new configuration
        try {
            checkDataFolder(); // Ensure the data folder exists
            newConfig.save(dataFileNew);
            Bukkit.getLogger().info("[Migration] Successfully migrated all chest data from old format to new one ;)");
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to save migrated chest data to file: " + e.getMessage());
            return false;
        }
    }


    public void saveChestData() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            // Save each entry in the existingChests map
            for (Map.Entry<Material, List<Location>> entry : existingChests.entrySet()) {
                String materialKey = entry.getKey().name();
                List<String> serializedLocations = new ArrayList<>();
                for (Location location : entry.getValue()) {
                    serializedLocations.add(serializeLocation(location));
                }
                config.set(materialKey, serializedLocations);
            }

            // Ensure the data folder exists
            checkDataFolder();

            // Save the config to the file in the plugin's data folder
            File dataFile = new File(plugin.getDataFolder(), DATA_FILE_new_NAME);
            config.save(dataFile);
            Bukkit.getLogger().info("Saved chest data");
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to save chest data to file: " + e.getMessage());
        }
    }

    private void checkDataFolder(){
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdirs()) {
                Bukkit.getLogger().severe("Nepavyko sukurt kaiko nx");
                plugin.getPluginLoader().disablePlugin(plugin);
            }
        }
    }

    public void saveChunks() {
        File dataFile = new File(plugin.getDataFolder(), DATA_CHUNK_FILE);
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Chunk> entry : newChunks.entrySet()) {
            UUID chunkId = entry.getKey();
            Chunk chunk = entry.getValue();

            Location start = chunk.getStart();
            Location end = chunk.getEnd();

            String key = chunkId.toString();
            config.set(key + ".start.world", start.getWorld().getName());
            config.set(key + ".start.x", start.getX());
            config.set(key + ".start.y", start.getY());
            config.set(key + ".start.z", start.getZ());
            config.set(key + ".end.world", end.getWorld().getName());
            config.set(key + ".end.x", end.getX());
            config.set(key + ".end.y", end.getY());
            config.set(key + ".end.z", end.getZ());
            config.set(key + ".size", chunk.getChunkSize());
        }

        try {
            config.save(dataFile);
            Bukkit.getLogger().info("[Chunks] Saved");
        } catch (IOException e) {
            Bukkit.getLogger().warning(e.getMessage());
        }
    }

    public void loadChunks() {
        newChunks.clear();
        File dataFile = new File(plugin.getDataFolder(), DATA_CHUNK_FILE);
        if (!dataFile.exists()) {
            Bukkit.getLogger().info("No chunks.yml file found");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : config.getKeys(false)) {
            UUID chunkId = UUID.fromString(key);

            String worldName = config.getString(key + ".start.world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double startX = config.getDouble(key + ".start.x");
            double startY = config.getDouble(key + ".start.y");
            double startZ = config.getDouble(key + ".start.z");
            double endX = config.getDouble(key + ".end.x");
            double endY = config.getDouble(key + ".end.y");
            double endZ = config.getDouble(key + ".end.z");
            int size = config.getInt(key + ".size");

            Location startLocation = new Location(world, startX, startY, startZ);
            Location endLocation = new Location(world, endX, endY, endZ);

            newChunks.put(chunkId, new Chunk(chunkId, startLocation, endLocation));
            Bukkit.getLogger().info("[Chunks] Chunk " + chunkId + " loaded");
        }
    }

    public void loadChestData() {
        Bukkit.getLogger().info("Loading chest data from file: " + DATA_FILE_new_NAME);
        File dataFile = new File(plugin.getDataFolder(), DATA_FILE_new_NAME);
        if (!dataFile.exists()) {
            Bukkit.getLogger().info("No data file found, migrating then if that one exists xD");
            if (!migrateChestData()) {
                return;
            }

            if (!dataFile.exists()) {
                Bukkit.getLogger().info("No data file found after migration, error occured ;)");
                return;
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            Material material = Material.getMaterial(key);
            List<String> serializedLocations = config.getStringList(key);
            List<Location> locations = new ArrayList<>();

            for (String serializedLocation : serializedLocations) {
                Location location = deserializeLocation(serializedLocation);
                if (location != null) {
                    locations.add(location);
                }
            }

            if (material != null) {
                existingChests.put(material, locations);
                Bukkit.getLogger().info("Loaded chest material " + material + " with locations " + locations);
            }
        }
    }

    private String serializeLocation(Location location) {
        return location.getWorld().getName() + "," +
                location.getX() + "," +
                location.getY() + "," +
                location.getZ();
    }

    private Location deserializeLocation(String serializedLocation) {
        String[] parts = serializedLocation.split(",");
        if (parts.length == 4) {
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(Bukkit.getWorld(worldName), x, y, z);
        }
        return null;
    }
}

