package lt.mredgariux.saugykla.util;

import java.util.Objects;
import org.bukkit.Location;

public final class Geometry {
    private Geometry() {
    }

    public static int area(Location first, Location second) {
        int width = Math.abs(second.getBlockX() - first.getBlockX()) + 1;
        int depth = Math.abs(second.getBlockZ() - first.getBlockZ()) + 1;
        return width * depth;
    }

    public static boolean overlaps(Location firstStart, Location firstEnd,
                                   Location secondStart, Location secondEnd) {
        if (!Objects.equals(firstStart.getWorld(), secondStart.getWorld())) {
            return false;
        }
        return rangesOverlap(firstStart.getBlockX(), firstEnd.getBlockX(),
                        secondStart.getBlockX(), secondEnd.getBlockX())
                && rangesOverlap(firstStart.getBlockY(), firstEnd.getBlockY(),
                        secondStart.getBlockY(), secondEnd.getBlockY())
                && rangesOverlap(firstStart.getBlockZ(), firstEnd.getBlockZ(),
                        secondStart.getBlockZ(), secondEnd.getBlockZ());
    }

    public static int horizontalDistance(Location first, Location second) {
        if (first == null || second == null || !Objects.equals(first.getWorld(), second.getWorld())) {
            return Integer.MAX_VALUE;
        }
        double deltaX = first.getX() - second.getX();
        double deltaZ = first.getZ() - second.getZ();
        return (int) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private static boolean rangesOverlap(int firstStart, int firstEnd, int secondStart, int secondEnd) {
        int firstMin = Math.min(firstStart, firstEnd);
        int firstMax = Math.max(firstStart, firstEnd);
        int secondMin = Math.min(secondStart, secondEnd);
        int secondMax = Math.max(secondStart, secondEnd);
        return firstMax >= secondMin && secondMax >= firstMin;
    }
}
