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
import lt.mredgariux.saugykla.util.Text;
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
    public static final String STORAGE_GUI_TITLE = "Saugykla";
    public static final String RESOURCES_GUI_TITLE = "Resources";

    private final JavaPlugin plugin;
    private final StorageService storageService;
    private final ResourceService resourceService;
    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public SaugyklaCommand(JavaPlugin plugin, StorageService storageService,
                           ResourceService resourceService) {
        this.plugin = plugin;
        this.storageService = storageService;
        this.resourceService = resourceService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color(plugin.getConfig().getString(
                    "translations.console", "&cYou cannot do that!")));
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
            default -> player.sendMessage(Text.color(
                    "&b- &c/s " + Arrays.toString(args) + " &cis unknown arguments for the command."));
        }
        return true;
    }

    private void openStorage(Player player) {
        int distance = storageService.nearestRegionDistance(player.getLocation());
        if (distance == Integer.MAX_VALUE) {
            player.sendMessage(Text.color("&b- &cThere are no chunks, create a chunk using (&a/s chunks&c)."));
            return;
        }
        if (distance >= 50) {
            player.sendMessage(Text.color("&b- &cYou are too far from storage location."));
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
            player.sendMessage(Text.color("&cNo permission to reset storage."));
            return;
        }
        player.sendMessage(storageService.reset()
                ? Text.color("&aStorage data reset successfully.")
                : Text.color("&cStorage data could not be reset; check the server log."));
    }

    private void highlight(Player player, String[] args) {
        Material material;
        if (args.length == 2) {
            material = Material.matchMaterial(args[1]);
            if (material == null) {
                player.sendMessage(Text.color("&cUnknown item."));
                return;
            }
        } else if (args.length == 1) {
            material = player.getInventory().getItemInMainHand().getType();
            if (material.isAir()) {
                player.sendMessage(Text.color(
                        "&cHold an item or use /s hl <material>, for example /s hl raw_iron."));
                return;
            }
        } else {
            player.sendMessage(Text.color("&cUsage: /s hl <material>"));
            return;
        }

        Location destination = storageService.locationFor(material);
        if (destination == null) {
            player.sendMessage(Text.color("&b- &cYou're not storing this item anywhere"));
            return;
        }
        if (Geometry.horizontalDistance(player.getLocation(), destination) >= 128) {
            player.sendMessage(Text.color("&cYou are too far to perform this action."));
            return;
        }
        storageService.toggleHighlight(material, player);
    }

    private void selectRegion(Player player, String[] args) {
        if (!player.hasPermission("saugykla.chunks")) {
            player.sendMessage(Text.color("&cNo permission to create storage regions."));
            return;
        }
        if (args.length != 2) {
            player.sendMessage(Text.color("&cUsage: /s chunks <start|end>"));
            return;
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType().isAir()) {
            player.sendMessage(Text.color("&cLook at a block to select a region point."));
            return;
        }

        RegionSelection selection = selections.computeIfAbsent(
                player.getUniqueId(), ignored -> new RegionSelection());
        if (args[1].equalsIgnoreCase("start")) {
            selection.setStart(target.getLocation());
            player.sendMessage(Text.color("&aSuccessfully selected the start point."));
            return;
        }
        if (!args[1].equalsIgnoreCase("end")) {
            player.sendMessage(Text.color("&cUsage: /s chunks <start|end>"));
            return;
        }

        Location start = selection.start();
        Location end = target.getLocation();
        if (start == null) {
            player.sendMessage(Text.color("&cSelect the start point first."));
            return;
        }
        if (!Objects.equals(start.getWorld(), end.getWorld())) {
            selections.remove(player.getUniqueId());
            player.sendMessage(Text.color("&cStart and end points must be in the same world."));
            return;
        }
        int size = Geometry.area(start, end);
        int maximumSize = plugin.getConfig().getInt("chunks.max_size", 512);
        if (size > maximumSize || size < 3) {
            selections.remove(player.getUniqueId());
            player.sendMessage(Text.color(size > maximumSize
                    ? "&cThe storage region is too large (maximum " + maximumSize + " blocks)."
                    : "&cThe storage region is too small (minimum 3 blocks)."));
            return;
        }
        selections.remove(player.getUniqueId());
        player.sendMessage(storageService.createRegion(start, end)
                ? Text.color("&aSuccessfully created the storage region.")
                : Text.color("&cThe region overlaps another region or could not be saved."));
    }

    private void search(Player player) {
        Material material = player.getInventory().getItemInMainHand().getType();
        if (material.isAir()) {
            player.sendMessage(Text.color("&b- &cHold an item in your hand to search."));
            return;
        }
        player.sendMessage(storageService.containsMaterial(material)
                ? Text.color("&b- &aThis item exists in the barrels somewhere.")
                : Text.color("&b- &cThis item isn't anywhere in the barrels."));
    }

    private void debug(Player player) {
        if (!player.getName().equalsIgnoreCase("edga0807")) {
            player.sendMessage(Text.color("&4- You are not permitted to do so."));
            return;
        }
        Map<UUID, StorageRegion> regions = storageService.regionSnapshot();
        Map<Material, List<Location>> barrels = storageService.barrelSnapshot();
        List<HighlightSession> highlights = storageService.highlightSnapshot();
        player.sendMessage(Text.color("&6 --- [ Regions ] --- "));
        regions.values().forEach(region -> player.sendMessage(Text.color(
                "&b- &c" + region.id() + " &8- &c" + region.start() + " &8- &c" + region.end()
                        + " &8- &c" + region.size())));
        player.sendMessage(Text.color("&6 --- [ Barrels ] --- "));
        barrels.forEach((material, locations) -> player.sendMessage(
                Text.color("&b- &c" + material + " &8- &c" + locations)));
        player.sendMessage(Text.color("&6 --- [ Highlights ] --- "));
        highlights.forEach(session -> player.sendMessage(Text.color(
                "&b- &c" + session.player().getName() + " &8- &c" + session.material())));
    }
}
