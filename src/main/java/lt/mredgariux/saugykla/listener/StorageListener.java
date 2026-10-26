package lt.mredgariux.saugykla.listener;

import lt.mredgariux.saugykla.command.SaugyklaCommand;
import lt.mredgariux.saugykla.service.ResourceService;
import lt.mredgariux.saugykla.service.StorageService;
import lt.mredgariux.saugykla.message.MessageKey;
import lt.mredgariux.saugykla.message.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class StorageListener implements Listener {
    private final Messages messages;
    private final StorageService storageService;
    private final ResourceService resourceService;

    public StorageListener(StorageService storageService, ResourceService resourceService, Messages messages) {
        this.messages = messages;
        this.storageService = storageService;
        this.resourceService = resourceService;
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.equals(SaugyklaCommand.STORAGE_GUI_TITLE)) {
            storageService.placeItems(event.getInventory().getStorageContents(), player);
        } else if (title.equals(SaugyklaCommand.RESOURCES_GUI_TITLE)) {
            resourceService.replaceFromInventory(event.getInventory().getStorageContents(), player);
        }
    }

    @EventHandler
    public void onBarrelClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !(event.getInventory().getHolder() instanceof Barrel barrel)) {
            return;
        }
        Location location = barrel.getLocation();
        if (!storageService.containsLocation(location)) {
            return;
        }

        Material expectedMaterial = storageService.materialAt(location);
        if (expectedMaterial == null) {
            return;
        }
        Inventory inventory = event.getInventory();
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.getType() != expectedMaterial) {
                inventory.removeItem(item);
                player.getInventory().addItem(item).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(
                                player.getLocation(), leftover));
                messages.send(player, MessageKey.BARREL_WRONG_ITEM);
            }
        }
        if (storageService.isBarrelEmpty(location)) {
            messages.send(player, storageService.deleteBarrel(expectedMaterial, location)
                    ? MessageKey.BARREL_EMPTY_REMOVED : MessageKey.BARREL_REMOVE_FAILED);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARREL || !storageService.containsLocation(block.getLocation())) {
            return;
        }
        Material material = storageService.materialAt(block.getLocation());
        if (storageService.deleteBarrel(material, block.getLocation())) {
            event.setDropItems(false);
            messages.send(event.getPlayer(), MessageKey.BARREL_REMOVED);
        }
    }
}
