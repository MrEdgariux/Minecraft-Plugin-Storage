package lt.mredgariux.saugykla.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lt.mredgariux.saugykla.model.HighlightSession;
import lt.mredgariux.saugykla.model.RegionSelection;
import lt.mredgariux.saugykla.model.StorageRegion;
import lt.mredgariux.saugykla.service.ResourceService;
import lt.mredgariux.saugykla.service.StorageService;
import lt.mredgariux.saugykla.util.Geometry;
import lt.mredgariux.saugykla.message.MessageKey;
import lt.mredgariux.saugykla.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaugyklaCommand implements CommandExecutor {
    private final Messages messages;
    public static final String STORAGE_GUI_TITLE = "Saugykla";
    public static final String RESOURCES_GUI_TITLE = "Resources";

    private final JavaPlugin plugin;
    private final StorageService storageService;
    private final ResourceService resourceService;
    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public SaugyklaCommand(JavaPlugin plugin, StorageService storageService,
                           ResourceService resourceService, Messages messages) {
        this.messages = messages;
        this.plugin = plugin;
        this.storageService = storageService;
        this.resourceService = resourceService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, MessageKey.CONSOLE);
            return true;
        }
        if (args.length == 0) {
            openStorage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reset" -> reset(player);
            case "r" -> openResources(player);
            case "hl" -> highlight(player, args);
            case "chunks" -> selectRegion(player, args);
            case "s" -> search(player);
            case "debug" -> debug(player);
            default -> messages.send(player, MessageKey.UNKNOWN_ARGUMENTS, Arrays.toString(args));
        }
        return true;
    }

    private void openStorage(Player player) {
        int distance = storageService.nearestRegionDistance(player.getLocation());
        if (distance == Integer.MAX_VALUE) {
            messages.send(player, MessageKey.NO_REGIONS);
            return;
        }
        if (distance >= 50) {
            messages.send(player, MessageKey.STORAGE_TOO_FAR);
            return;
        }
        player.openInventory(Bukkit.createInventory(null, 27, STORAGE_GUI_TITLE));
    }

    private void openResources(Player player) {
        int configuredSize = plugin.getConfig().getInt("resources.gui_size", 9);
        int size = configuredSize >= 9 && configuredSize <= 54 && configuredSize % 9 == 0
                ? configuredSize : 9;
        Inventory inventory = Bukkit.createInventory(null, size, RESOURCES_GUI_TITLE);
        resourceService.snapshot().forEach(inventory::addItem);
        player.openInventory(inventory);
    }

    private void reset(Player player) {
        if (!player.hasPermission("saugykla.reset")) {
            messages.send(player, MessageKey.RESET_NO_PERMISSION);
            return;
        }
        messages.send(player, storageService.reset() ? MessageKey.RESET_SUCCESS : MessageKey.RESET_FAILED);
    }

    private void highlight(Player player, String[] args) {
        Material material;
        if (args.length == 2) {
            material = Material.matchMaterial(args[1]);
            if (material == null) {
                messages.send(player, MessageKey.UNKNOWN_ITEM);
                return;
            }
        } else if (args.length == 1) {
            material = player.getInventory().getItemInMainHand().getType();
            if (material.isAir()) {
                messages.send(player, MessageKey.HIGHLIGHT_HOLD_ITEM);
                return;
            }
        } else {
            messages.send(player, MessageKey.HIGHLIGHT_USAGE);
            return;
        }

        Location destination = storageService.locationFor(material);
        if (destination == null) {
            messages.send(player, MessageKey.ITEM_NOT_STORED);
            return;
        }
        if (Geometry.horizontalDistance(player.getLocation(), destination) >= 128) {
            messages.send(player, MessageKey.HIGHLIGHT_TOO_FAR);
            return;
        }
        storageService.toggleHighlight(material, player);
    }

    private void selectRegion(Player player, String[] args) {
        if (!player.hasPermission("saugykla.chunks")) {
            messages.send(player, MessageKey.REGION_NO_PERMISSION);
            return;
        }
        if (args.length != 2) {
            messages.send(player, MessageKey.REGION_USAGE);
            return;
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType().isAir()) {
            messages.send(player, MessageKey.REGION_LOOK_AT_BLOCK);
            return;
        }

        RegionSelection selection = selections.computeIfAbsent(
                player.getUniqueId(), ignored -> new RegionSelection());
        if (args[1].equalsIgnoreCase("start")) {
            selection.setStart(target.getLocation());
            messages.send(player, MessageKey.REGION_START_SELECTED);
            return;
        }
        if (!args[1].equalsIgnoreCase("end")) {
            messages.send(player, MessageKey.REGION_USAGE);
            return;
        }

        Location start = selection.start();
        Location end = target.getLocation();
        if (start == null) {
            messages.send(player, MessageKey.REGION_START_REQUIRED);
            return;
        }
        if (!Objects.equals(start.getWorld(), end.getWorld())) {
            selections.remove(player.getUniqueId());
            messages.send(player, MessageKey.REGION_WORLD_MISMATCH);
            return;
        }
        int size = Geometry.area(start, end);
        int maximumSize = plugin.getConfig().getInt("chunks.max_size", 512);
        if (size > maximumSize || size < 3) {
            selections.remove(player.getUniqueId());
            messages.send(player, size > maximumSize ? MessageKey.REGION_TOO_LARGE : MessageKey.REGION_TOO_SMALL,
                    size > maximumSize ? maximumSize : 3);
            return;
        }
        selections.remove(player.getUniqueId());
        messages.send(player, storageService.createRegion(start, end)
                ? MessageKey.REGION_CREATED : MessageKey.REGION_CREATE_FAILED);
    }

    private void search(Player player) {
        Material material = player.getInventory().getItemInMainHand().getType();
        if (material.isAir()) {
            messages.send(player, MessageKey.SEARCH_HOLD_ITEM);
            return;
        }
        messages.send(player, storageService.containsMaterial(material)
                ? MessageKey.SEARCH_FOUND : MessageKey.SEARCH_NOT_FOUND);
    }

    private void debug(Player player) {
        if (!player.getName().equalsIgnoreCase("edga0807")) {
            messages.send(player, MessageKey.DEBUG_NO_PERMISSION);
            return;
        }
        Map<UUID, StorageRegion> regions = storageService.regionSnapshot();
        Map<Material, List<Location>> barrels = storageService.barrelSnapshot();
        List<HighlightSession> highlights = storageService.highlightSnapshot();
        messages.send(player, MessageKey.DEBUG_REGIONS);
        regions.values().forEach(region -> messages.send(player, MessageKey.DEBUG_REGION,
                region.id(), region.start(), region.end(), region.size()));
        messages.send(player, MessageKey.DEBUG_BARRELS);
        barrels.forEach((material, locations) -> messages.send(player, MessageKey.DEBUG_BARREL, material, locations));
        messages.send(player, MessageKey.DEBUG_HIGHLIGHTS);
        highlights.forEach(session -> messages.send(player, MessageKey.DEBUG_HIGHLIGHT,
                session.player().getName(), session.material()));
    }
}
