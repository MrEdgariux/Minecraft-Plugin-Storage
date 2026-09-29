package lt.mredgariux.saugykla.model;

import org.bukkit.Location;

public final class RegionSelection {
    private Location start;

    public void setStart(Location start) {
        this.start = start.clone();
    }

    public Location start() {
        return start == null ? null : start.clone();
    }
}
