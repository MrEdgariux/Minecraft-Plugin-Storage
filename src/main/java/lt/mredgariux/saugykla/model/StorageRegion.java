package lt.mredgariux.saugykla.model;

import java.util.Objects;
import java.util.UUID;
import lt.mredgariux.saugykla.util.Geometry;
import org.bukkit.Location;

public final class StorageRegion {
    private final UUID id;
    private final Location start;
    private final Location end;

    public StorageRegion(UUID id, Location start, Location end) {
        this.id = Objects.requireNonNull(id, "id");
        this.start = Objects.requireNonNull(start, "start").clone();
        this.end = Objects.requireNonNull(end, "end").clone();
    }

    public UUID id() {
        return id;
    }

    public Location start() {
        return start.clone();
    }

    public Location end() {
        return end.clone();
    }

    public int size() {
        return Geometry.area(start, end);
    }
}
