package lt.mredgariux.saugykla.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lt.mredgariux.saugykla.model.HighlightSession;
import lt.mredgariux.saugykla.model.StorageRegion;
import lt.mredgariux.saugykla.persistence.RegionRepository;
import lt.mredgariux.saugykla.persistence.StorageRepository;
import lt.mredgariux.saugykla.util.Geometry;
import lt.mredgariux.saugykla.util.SignMaterials;
import lt.mredgariux.saugykla.util.SignText;
import lt.mredgariux.saugykla.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class StorageService {
    private static final int HIGHLIGHT_DISTANCE = 128;

    private final JavaPlugin plugin;
    private final ResourceService resourceService;
    private final StorageRepository storageRepository;
    private final RegionRepository regionRepository;
    private final Map<Material, List<Location>> barrels;
    private final Map<UUID, StorageRegion> regions;
    private final List<HighlightSession> highlights = new ArrayList<>();

    public StorageService(JavaPlugin plugin, ResourceService resourceService,
                          StorageRepository storageRepository, RegionRepository regionRepository) {
        this.plugin = plugin;
        this.resourceService = resourceService;
        this.storageRepository = storageRepository;
        this.regionRepository = regionRepository;
        barrels = storageRepository.load();
        regions = regionRepository.load();
    }

    public boolean containsLocation(Location location) {
        return barrels.values().stream().flatMap(Collection::stream).anyMatch(location::equals);
    }

    public boolean containsMaterial(Material material) {
        return barrels.containsKey(material) && !barrels.get(material).isEmpty();
    }

    public Material materialAt(Location location) {
        return barrels.entrySet().stream()
                .filter(entry -> entry.getValue().contains(location))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public Location locationFor(Material material) {
        List<Location> locations = barrels.get(material);
        return locations == null || locations.isEmpty() ? null : locations.get(0).clone();
    }

    public int nearestRegionDistance(Location location) {
        return regions.values().stream()
                .mapToInt(region -> Geometry.horizontalDistance(location, region.start()))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    public boolean createRegion(Location start, Location end) {
        boolean overlaps = regions.values().stream()
                .anyMatch(region -> Geometry.overlaps(start, end, region.start(), region.end()));
        if (overlaps) {
            return false;
        }
        UUID id = UUID.randomUUID();
        regions.put(id, new StorageRegion(id, start, end));
        return regionRepository.save(regions);
    }

    public Map<Material, List<Location>> barrelSnapshot() {
        Map<Material, List<Location>> copy = new EnumMap<>(Material.class);
        barrels.forEach((material, locations) -> copy.put(
                material, locations.stream().map(Location::clone).toList()));
        return Collections.unmodifiableMap(copy);
    }

    public Map<UUID, StorageRegion> regionSnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(regions));
    }

    public List<HighlightSession> highlightSnapshot() {
        return List.copyOf(highlights);
    }

    public void toggleHighlight(Material material, Player player) {
        List<Location> locations = barrels.get(material);
        if (locations == null || locations.isEmpty()) {
            player.sendMessage(Text.color("&b- &cYou're not storing this item anywhere"));
            return;
        }

        Iterator<HighlightSession> iterator = highlights.iterator();
        while (iterator.hasNext()) {
            HighlightSession session = iterator.next();
            if (session.player().getUniqueId().equals(player.getUniqueId())
                    && session.material() == material) {
                session.task().cancel();
                iterator.remove();
                player.sendMessage(Text.color("&b- &cPath to the &2" + material + "&c hidden"));
                return;
            }
        }

        Location destination = locations.get(locations.size() - 1).clone();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin, () -> drawParticleTrail(player, destination), 0L, 2L);
        highlights.add(new HighlightSession(player, material, destination, task));
        player.sendMessage(Text.color("&b- &aShowing path to the &2" + material));
    }

    public void stopHighlights() {
        highlights.forEach(session -> session.task().cancel());
        highlights.clear();
    }

    public boolean isBarrelEmpty(Location location) {
        if (!(location.getBlock().getState() instanceof Barrel barrel)) {
            return false;
        }
        return barrel.getInventory().isEmpty();
    }

    public boolean deleteBarrel(Material material, Location location) {
        if (material == null) {
            return false;
        }
        List<Location> locations = barrels.get(material);
        if (locations == null || !locations.remove(location)) {
            return false;
        }
        if (locations.isEmpty()) {
            barrels.remove(material);
        }
        cancelHighlights(material);
        removeBarrelBlocks(location);
        storageRepository.save(barrels);
        return true;
    }

    public void placeItems(ItemStack[] items, Player player) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            ItemStack remaining = item.clone();
            if (containsMaterial(item.getType())) {
                remaining = addToExistingBarrels(barrels.get(item.getType()), remaining);
            }
            if (remaining == null) {
                continue;
            }

            boolean existingMaterial = containsMaterial(item.getType());
            Location location = existingMaterial
                    ? nextStackLocation(barrels.get(item.getType()))
                    : findNextAvailableLocation();
            if (location == null) {
                String message = existingMaterial
                        ? "&b- &cNo more vertical space is available for &2" + item.getType()
                                + "&c in its storage region."
                        : "&b- &cNo horizontal space is available for a new item barrel "
                                + "in any storage region &b(&a/s chunks&b)";
                returnToPlayer(player, remaining, message);
                continue;
            }
            if (!createBarrel(location, remaining, player)) {
                continue;
            }
            barrels.computeIfAbsent(item.getType(), ignored -> new ArrayList<>()).add(location.clone());
            storageRepository.save(barrels);
        }
    }

    public boolean reset() {
        boolean storageDeleted = storageRepository.delete();
        boolean regionsDeleted = regionRepository.delete();
        if (!storageDeleted || !regionsDeleted) {
            return false;
        }
        barrels.values().stream().flatMap(Collection::stream).forEach(this::removeBarrelBlocks);
        barrels.clear();
        regions.clear();
        stopHighlights();
        return true;
    }

    private ItemStack addToExistingBarrels(List<Location> locations, ItemStack item) {
        ItemStack remaining = item.clone();
        for (Location location : locations) {
            if (!(location.getBlock().getState() instanceof Barrel barrel)) {
                plugin.getLogger().warning("Expected a barrel at " + location + " but found "
                        + location.getBlock().getType() + '.');
                continue;
            }
            Map<Integer, ItemStack> leftovers = barrel.getInventory().addItem(remaining);
            if (leftovers.isEmpty()) {
                return null;
            }
            remaining = leftovers.values().iterator().next();
        }
        return remaining;
    }

    private boolean createBarrel(Location location, ItemStack item, Player player) {
        Location signLocation = location.clone().subtract(1, 0, 0);
        if (!location.getBlock().getType().isAir() || !signLocation.getBlock().getType().isAir()) {
            returnToPlayer(player, item, "&cA block is preventing placement of a storage barrel.");
            return false;
        }
        if (!resourceService.takeOne(Material.BARREL)) {
            returnToPlayer(player, item, "&b- &cAdd barrels &b(&a/s r&b)");
            return false;
        }
        Material signMaterial = resourceService.takeAnySign();
        if (signMaterial == null) {
            resourceService.refund(Material.BARREL);
            returnToPlayer(player, item, "&b- &cAdd signs &b(&a/s r&b)");
            return false;
        }

        try {
            Block block = location.getBlock();
            block.setType(Material.BARREL);
            Directional barrelData = (Directional) block.getBlockData();
            barrelData.setFacing(BlockFace.UP);
            block.setBlockData(barrelData);
            Barrel barrel = (Barrel) block.getState();
            Map<Integer, ItemStack> leftovers = barrel.getInventory().addItem(item);
            leftovers.values().forEach(leftover -> returnToInventory(player, leftover));
            placeSign(signLocation.getBlock(), block, signMaterial, item.getType());
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Failed to create storage barrel at " + location + '.', exception);
            location.getBlock().setType(Material.AIR);
            signLocation.getBlock().setType(Material.AIR);
            resourceService.refund(Material.BARREL);
            resourceService.refund(signMaterial);
            returnToPlayer(player, item, "&cThe storage barrel could not be created.");
            return false;
        }
    }

    private void placeSign(Block signBlock, Block barrelBlock, Material standingSign, Material storedMaterial) {
        Material wallSign = Objects.requireNonNull(
                SignMaterials.wallSignFor(standingSign), "Unsupported sign material: " + standingSign);
        signBlock.setType(wallSign);
        WallSign data = (WallSign) signBlock.getBlockData();
        BlockFace face = Objects.requireNonNull(barrelBlock.getFace(signBlock), "Sign is not adjacent to barrel");
        data.setFacing(face);
        signBlock.setBlockData(data);

        Sign sign = (Sign) signBlock.getState();
        List<String> lines = SignText.wrap(storedMaterial.name().replace('_', ' '));
        for (int index = 0; index < lines.size(); index++) {
            sign.getSide(Side.FRONT).setLine(index, lines.get(index));
        }
        sign.update();
    }

    private Location nextStackLocation(List<Location> locations) {
        Location top = locations.get(locations.size() - 1);
        Location above = top.clone().add(0, 1, 0);
        Location signLocation = above.clone().subtract(1, 0, 0);
        boolean insideRegion = regions.values().stream()
                .anyMatch(region -> contains(region, above));
        return insideRegion
                && above.getBlock().getType().isAir()
                && signLocation.getBlock().getType().isAir()
                ? above : null;
    }

    private Location findNextAvailableLocation() {
        for (StorageRegion region : regions.values()) {
            Location available = findAvailableLocation(region);
            if (available != null) {
                return available;
            }
        }
        return null;
    }

    private Location findAvailableLocation(StorageRegion region) {
        Location start = region.start();
        Location end = region.end();
        World world = start.getWorld();
        if (world == null) {
            return null;
        }
        int minX = Math.min(start.getBlockX(), end.getBlockX());
        int maxX = Math.max(start.getBlockX(), end.getBlockX());
        int minY = Math.min(start.getBlockY(), end.getBlockY());
        int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
        int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());
        for (int x = minX; x <= maxX; x += 2) {
            for (int z = minZ; z <= maxZ; z++) {
                Location barrelLocation = new Location(world, x, minY, z);
                Location signLocation = barrelLocation.clone().subtract(1, 0, 0);
                if (barrelLocation.getBlock().getType().isAir()
                        && signLocation.getBlock().getType().isAir()) {
                    return barrelLocation;
                }
            }
        }
        return null;
    }

    private boolean contains(StorageRegion region, Location location) {
        Location start = region.start();
        Location end = region.end();
        return Objects.equals(start.getWorld(), location.getWorld())
                && location.getBlockX() >= Math.min(start.getBlockX(), end.getBlockX())
                && location.getBlockX() <= Math.max(start.getBlockX(), end.getBlockX())
                && location.getBlockY() >= Math.min(start.getBlockY(), end.getBlockY())
                && location.getBlockY() <= Math.max(start.getBlockY(), end.getBlockY())
                && location.getBlockZ() >= Math.min(start.getBlockZ(), end.getBlockZ())
                && location.getBlockZ() <= Math.max(start.getBlockZ(), end.getBlockZ());
    }

    private void drawParticleTrail(Player player, Location destination) {
        if (!player.isOnline() || Geometry.horizontalDistance(player.getLocation(), destination) >= HIGHLIGHT_DISTANCE) {
            cancelHighlight(player.getUniqueId(), destination);
            if (player.isOnline()) {
                player.sendMessage(Text.color("&cYou ran too far, so the path was hidden automatically."));
            }
            return;
        }
        Location start = player.getLocation().add(0, 0.5, 0);
        Location end = destination.clone().add(0.5, 0.5, 0.5);
        World world = start.getWorld();
        if (world == null || !Objects.equals(world, end.getWorld())) {
            cancelHighlight(player.getUniqueId(), destination);
            return;
        }
        int particleCount = 20;
        for (int index = 0; index <= particleCount; index++) {
            double progress = index / (double) particleCount;
            Location point = new Location(world,
                    start.getX() + progress * (end.getX() - start.getX()),
                    start.getY() + progress * (end.getY() - start.getY()),
                    start.getZ() + progress * (end.getZ() - start.getZ()));
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
        }
    }

    private void cancelHighlights(Material material) {
        Iterator<HighlightSession> iterator = highlights.iterator();
        while (iterator.hasNext()) {
            HighlightSession session = iterator.next();
            if (session.material() == material) {
                session.task().cancel();
                session.player().sendMessage(Text.color(
                        "&b- &cThe barrel you were navigating to was destroyed by another player"));
                iterator.remove();
            }
        }
    }

    private void cancelHighlight(UUID playerId, Location destination) {
        Iterator<HighlightSession> iterator = highlights.iterator();
        while (iterator.hasNext()) {
            HighlightSession session = iterator.next();
            if (session.player().getUniqueId().equals(playerId)
                    && session.destination().equals(destination)) {
                session.task().cancel();
                iterator.remove();
                return;
            }
        }
    }

    private void removeBarrelBlocks(Location location) {
        Block signBlock = location.clone().subtract(1, 0, 0).getBlock();
        Material standingSign = SignMaterials.standingSignFor(signBlock.getType());
        if (standingSign != null) {
            signBlock.setType(Material.AIR);
            resourceService.refund(standingSign);
        }

        Block barrelBlock = location.getBlock();
        if (barrelBlock.getState() instanceof Barrel barrel) {
            for (ItemStack item : barrel.getInventory().getStorageContents()) {
                if (item != null) {
                    World world = location.getWorld();
                    if (world != null) {
                        world.dropItem(location, item);
                    }
                }
            }
            barrelBlock.setType(Material.AIR);
            resourceService.refund(Material.BARREL);
        }
    }

    private void returnToPlayer(Player player, ItemStack item, String message) {
        returnToInventory(player, item);
        player.sendMessage(Text.color(message));
    }

    private static void returnToInventory(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
