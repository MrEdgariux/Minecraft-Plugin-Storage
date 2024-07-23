package lt.mredgariux.saugykla.datasets;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Runnables {
    private final Player player;
    private final Material material;
    private final Location where_material;
    private final BukkitRunnable runnable;

    public Runnables(Player player, Material material, Location whereMaterial, BukkitRunnable runnable) {
        this.player = player;
        this.material = material;
        this.where_material = whereMaterial;
        this.runnable = runnable;
    }

    public Player getPlayer() {
        return player;
    }

    public Material getMaterial() {
        return material;
    }

    public Location getWhereMaterial() {
        return where_material;
    }

    public BukkitRunnable getRunnable() {
        return runnable;
    }
}
