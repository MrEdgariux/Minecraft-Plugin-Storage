package lt.mredgariux.saugykla.model;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public record HighlightSession(Player player, Material material, Location destination, BukkitTask task) {
    public HighlightSession {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(material, "material");
        destination = Objects.requireNonNull(destination, "destination").clone();
        Objects.requireNonNull(task, "task");
    }
}
