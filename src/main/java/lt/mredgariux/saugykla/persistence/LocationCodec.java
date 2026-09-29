package lt.mredgariux.saugykla.persistence;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

final class LocationCodec {
    private LocationCodec() {
    }

    static String serialize(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Cannot serialize a location without a world");
        }
        return world.getName() + ',' + location.getX() + ',' + location.getY() + ',' + location.getZ();
    }

    static Optional<Location> deserialize(String value) {
        String[] parts = value.split(",");
        if (parts.length != 4) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Location(world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
