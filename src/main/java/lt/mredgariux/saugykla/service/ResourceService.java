package lt.mredgariux.saugykla.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lt.mredgariux.saugykla.persistence.ResourceRepository;
import lt.mredgariux.saugykla.util.SignMaterials;
import lt.mredgariux.saugykla.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourceService {
    private final JavaPlugin plugin;
    private final ResourceRepository repository;
    private final List<ItemStack> resources;

    public ResourceService(JavaPlugin plugin, ResourceRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        resources = repository.load();
    }

    public List<ItemStack> snapshot() {
        return resources.stream().map(ItemStack::clone).toList();
    }

    public void replaceFromInventory(ItemStack[] contents, Player player) {
        resources.clear();
        boolean convertChests = plugin.getConfig().getBoolean("resources.convert_chests_to_barrels", true);
        for (ItemStack original : contents) {
            if (original == null || original.getType().isAir()) {
                continue;
            }
            ItemStack item = original.clone();
            if (item.getType() == Material.CHEST && convertChests) {
                item.setType(Material.BARREL);
            }
            if (item.getType() != Material.BARREL && !SignMaterials.isStandingSign(item.getType())) {
                player.sendMessage(Text.color("&b- &c" + item.getType() + " isn't an allowed resource."));
                player.getInventory().addItem(item).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                continue;
            }
            resources.add(item);
        }
        repository.save(resources);
        player.sendMessage(Text.color("&b- &aResources saved!"));
    }

    public boolean takeOne(Material material) {
        Iterator<ItemStack> iterator = resources.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (stack.getType() != material) {
                continue;
            }
            if (stack.getAmount() == 1) {
                iterator.remove();
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
            repository.save(resources);
            return true;
        }
        return false;
    }

    public Material takeAnySign() {
        for (ItemStack stack : new ArrayList<>(resources)) {
            if (SignMaterials.isStandingSign(stack.getType()) && takeOne(stack.getType())) {
                return stack.getType();
            }
        }
        return null;
    }

    public void refund(Material material) {
        for (ItemStack stack : resources) {
            if (stack.getType() == material && stack.getAmount() < stack.getMaxStackSize()) {
                stack.setAmount(stack.getAmount() + 1);
                repository.save(resources);
                return;
            }
        }
        resources.add(new ItemStack(material));
        repository.save(resources);
    }
}
