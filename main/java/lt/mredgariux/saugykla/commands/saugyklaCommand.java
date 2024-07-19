package lt.mredgariux.saugykla.commands;

import lt.mredgariux.saugykla.main;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class saugyklaCommand implements CommandExecutor, Listener {
    Plugin plugin = main.getPlugin(main.class);

    private final Location startPos = new Location(Bukkit.getWorld("world"), 2638, 10, 0);
    private final Location endPos = new Location(Bukkit.getWorld("world"), 2644, 10, 0);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) {
            Bukkit.getLogger().warning("Negalite sio veiksmo atlikti jus");
            return false;
        }


        if (strings.length >= 1) {
            if (strings[0].equalsIgnoreCase("reset")) {
                if (p.hasPermission("saugykla.reset")) {
                    if (((main) plugin).chestManagement.deleteStoreFile()) {
                        p.sendMessage("Success");
                        return true;
                    } else {
                        p.sendMessage("Error deleting file nx");
                        return false;
                    }
                } else {
                    p.sendMessage("No perms to do so");
                    return false;
                }
            } else if (strings[0].equalsIgnoreCase("r")) {
                // Adding materials for barrel making. As they are expensive :)
                Inventory inv = Bukkit.createInventory(null, 9, "Resources");
                if (!((main) plugin).recipeManagement.materials.isEmpty()) {
                    for (ItemStack itemas : ((main) plugin).recipeManagement.materials) {
                        inv.addItem(itemas);
                    }
                }
                p.openInventory(inv);
            } else if (strings[0].equalsIgnoreCase("hl")) {
                if (strings.length != 2) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cDėja, nepakanka informacijos (/s hl <material>) pvz (/s hl raw_iron)"));
                    return false;
                }

                try {
                    Material materialas = Material.getMaterial(strings[1].toUpperCase());
                    if (materialas == null) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNežinomas itemas"));
                        return false;
                    }

                    ((main) plugin).chestManagement.highlight_chest(materialas, p);
                } catch (Exception e) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
                    return false;
                }
            }
        } else {
            // Create GUI
            Inventory inv = Bukkit.createInventory(null, 27, "Saugykla");
            p.openInventory(inv);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e) {
        Inventory inventorius  = e.getInventory();

        if (e.getView().getTitle().equals("Saugykla")) {
            ((main) plugin).chestManagement.placeItemsInChests(inventorius.getStorageContents(), (Player) e.getPlayer());
        } else if (e.getView().getTitle().equals("Resources")) {
            ((main) plugin).recipeManagement.addResources(inventorius.getStorageContents(), (Player) e.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (inventory.getHolder() instanceof Barrel barrel) {
            Location location = barrel.getLocation();
            if (((main) plugin).chestManagement.is_chest_exists(location)) {
                Bukkit.getLogger().info(location.toString() + " have barrel in database, by player " + e.getPlayer().getName());

                // Scan inventory
                ItemStack[] itemai = inventory.getStorageContents();
                Material expected_item_materials = ((main) plugin).chestManagement.get_material(location);
                int scanned_rows = 0;
                for (ItemStack itemas : itemai) {
                    if (itemas.getType() != expected_item_materials) {
                        ((main) plugin).chestManagement.dropItems((Player) e.getPlayer(), itemas);
                        inventory.removeItem(itemas);
                        e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &c&lNEMAIŠYK DAIKTŲ TAN, KUR JIE NEPRIKLAUSO!."));
                    } else {
                        scanned_rows++;
                    }
                }
                if (scanned_rows != 0) {
                    return;
                }
                Bukkit.getLogger().info("Sistema nerado daiktų barrelyje xD");
                e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&cSaugykla&4] &aSunaikintas tuščias barrelis."));
                // Tipo naikina xD
            }
        }
    }


    @EventHandler
    public void onBlockDestroyed(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (block.getType() == Material.BARREL) {
            if (((main) plugin).chestManagement.is_chest_exists(block.getLocation())) {
                Material matas = ((main) plugin).chestManagement.get_material(block.getLocation());
                ((main) plugin).chestManagement.delete_chest(matas);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Saugyklos] &aSekmingai sunaikinote saugykla"));
            }
        }
    }

}