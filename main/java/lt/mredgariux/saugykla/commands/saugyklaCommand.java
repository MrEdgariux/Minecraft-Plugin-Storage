package lt.mredgariux.saugykla.commands;

import lt.mredgariux.saugykla.main;
import lt.mredgariux.saugykla.utils.calculations;
import lt.mredgariux.saugykla.utils.chat;
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

import java.util.*;

public class saugyklaCommand implements CommandExecutor, Listener {
    Plugin plugin = main.getPlugin(main.class);

    private Location startPos;
    private Location endPos;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) {
            Bukkit.getLogger().warning("Negalite sio veiksmo atlikti jus");
            return false;
        }


        if (strings.length >= 1) {
            if (strings[0].equalsIgnoreCase("reset")) {
                if (p.hasPermission("saugykla.reset")) {
                    if (((main) plugin).chestManagement.deleteStoreFile() && ((main) plugin).chestManagement.deleteChunkFile()) {
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
                    if (p.getInventory().getItemInMainHand().getType() != Material.AIR) {
                        ((main) plugin).chestManagement.highlight_chest(p.getInventory().getItemInMainHand().getType(), p);
                        return true;
                    } else {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cHave item in main hand or use command like this (/s hl <material>) example (/s hl raw_iron)"));
                        return false;
                    }
                }

                try {
                    Material materialas = Material.getMaterial(strings[1].toUpperCase());
                    if (materialas == null) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUnknown item"));
                        return false;
                    }

                    ((main) plugin).chestManagement.highlight_chest(materialas, p);
                } catch (Exception e) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
                    return false;
                }
            } else  if (strings[0].equalsIgnoreCase("chunks")) {
                if (strings.length != 2) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSorry, not enough arguments. (/s chunks <start|end>) example (/s chunks start)"));
                    return false;
                }

                if (!p.hasPermission("saugykla.chunks")) {
                    p.sendMessage(chat.color("&cNo permissions :("));
                    return false;
                }

                if (strings[1].equalsIgnoreCase("start")) {
                    Block blokas = p.getTargetBlockExact(5);
                    if (blokas == null || blokas.getType() == Material.AIR) {
                        p.sendMessage(chat.color("&cYou need to look at the block you wanna create start point from"));
                        return false;
                    }

                    startPos = blokas.getLocation();
                    p.sendMessage(chat.color("&aSuccessfully created start point"));
                    return true;
                } else if (strings[1].equalsIgnoreCase("end")) {
                    Block blokas = p.getTargetBlockExact(5);
                    if (blokas == null || blokas.getType() == Material.AIR) {
                        p.sendMessage(chat.color("&cYou need to look at the block you wanna create start point from"));
                        return false;
                    }
                    endPos = blokas.getLocation();
                    p.sendMessage(chat.color("&aSuccessfully created end point"));
                    if (startPos != null && endPos != null) {
                        if (!Objects.equals(startPos.getWorld(), endPos.getWorld())) {
                            p.sendMessage(chat.color("&cStart and end points must be in the same world"));
                            startPos = null;
                            endPos = null;
                            return false;
                        }
                        if (calculations.calculateChunkSize(startPos, endPos) > 512) {
                            p.sendMessage(chat.color("&cYour created chunk is too large to be saved. Consider being inside the same Y axis"));
                            startPos = null;
                            endPos = null;
                            return false;
                        } else if (calculations.calculateChunkSize(startPos, endPos) < 3) {
                            p.sendMessage(chat.color("&cYour created chunk is too small to be saved."));
                            startPos = null;
                            endPos = null;
                            return false;
                        }

                        if (!((main) plugin).chestManagement.create_chunk(startPos, endPos)) {
                            p.sendMessage(chat.color("&cYour created chunk is colliding with other chunks, which would affect barrel placing script. Please re-create the chunk in other location"));
                            startPos = null;
                            endPos = null;
                            return false;
                        }
                        p.sendMessage(chat.color("&aSuccessfully created chunk."));
                        startPos = null; endPos = null;
                        return true;
                    }
                    return false;
                }
            } else if (strings[0].equalsIgnoreCase("s")) {
                if (p.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    if (((main) plugin).chestManagement.materialAre(p.getInventory().getItemInMainHand().getType())) {
                        p.sendMessage(chat.color("&b- &aThis item exists in the barrels somewhere."));
                    } else {
                        p.sendMessage(chat.color("&b- &cThis item isn't anywhere in the barrels."));
                    }
                } else {
                    p.sendMessage(chat.color("&b- &cHold item in the hands to search."));
                }
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b- &c/s " + strings[1] + " &cis unknown argument for the command."));
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
                // Scan inventory
                ItemStack[] itemai = inventory.getStorageContents();
                Material expected_item_materials = ((main) plugin).chestManagement.getMaterial(location);
                int scanned_rows = 0;
                for (ItemStack itemas : itemai) {
                    if (itemas != null && itemas.getType() != expected_item_materials) {
                        ((main) plugin).chestManagement.dropItems((Player) e.getPlayer(), itemas);
                        inventory.removeItem(itemas);
                        e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b- &cThis item does not belong here"));
                    } else {
                        scanned_rows++;
                    }
                }
            }
        }
    }


    @EventHandler
    public void onBlockDestroyed(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (block.getType() == Material.BARREL) {
            if (((main) plugin).chestManagement.is_chest_exists(block.getLocation())) {
                Material matas = ((main) plugin).chestManagement.getMaterial(block.getLocation());
                ((main) plugin).chestManagement.delete_chest(matas, block.getLocation());

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b- &cSuccessfully destroyed storage"));
            }
        }
    }

}