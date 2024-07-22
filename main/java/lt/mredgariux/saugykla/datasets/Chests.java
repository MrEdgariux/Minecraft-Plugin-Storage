package lt.mredgariux.saugykla.datasets;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

public class Chests {
    private final Material material;
    private List<Location> location;
    public Chests(Material material, List<Location> location) {
        this.material = material;
        this.location = location;
    }
}
