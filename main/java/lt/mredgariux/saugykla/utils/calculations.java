package lt.mredgariux.saugykla.utils;

import org.bukkit.Location;
import org.bukkit.Material;

public class calculations {
    public static int calculateChunkSize(Location start, Location end) {
        // Calculate the width, height, and depth
        int width = Math.abs(end.getBlockX() - start.getBlockX()) + 1;
        int depth = Math.abs(end.getBlockZ() - start.getBlockZ()) + 1;

        // Calculate the total number of blocks
        return width  * depth;
    }

    public static boolean isColliding(Location start1, Location end1, Location start2, Location end2) {
        // Check if the two chunks overlap in any dimension
        return !(end1.getBlockX() < start2.getBlockX() || start1.getBlockX() > end2.getBlockX() ||
                end1.getBlockY() < start2.getBlockY() || start1.getBlockY() > end2.getBlockY() ||
                end1.getBlockZ() < start2.getBlockZ() || start1.getBlockZ() > end2.getBlockZ());
    }

    public static int calculateDistanceBetweenLocations(Location one, Location two) {
        // Calculate the differences in the X and Z coordinates
        double deltaX = one.getX() - two.getX();
        double deltaZ = one.getZ() - two.getZ();

        // Use the Pythagorean theorem to calculate the distance in 2D (X and Z)
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Return the distance as an integer (you can choose to return it as a double if needed)
        return (int) distance;
    }

    public static Material SignToWallSign(Material signMaterial) {
        switch (signMaterial) {
            case OAK_SIGN:
                return Material.OAK_WALL_SIGN;
            case BIRCH_SIGN:
                return Material.BIRCH_WALL_SIGN;
            case DARK_OAK_SIGN:
                return Material.DARK_OAK_WALL_SIGN;
            case JUNGLE_SIGN:
                return Material.JUNGLE_WALL_SIGN;
            case SPRUCE_SIGN:
                return Material.SPRUCE_WALL_SIGN;
            case ACACIA_SIGN:
                return Material.ACACIA_WALL_SIGN;
            case BAMBOO_SIGN:
                return Material.BAMBOO_WALL_SIGN;
            case CHERRY_SIGN:
                return Material.CHERRY_WALL_SIGN;
            case MANGROVE_SIGN:
                return Material.MANGROVE_WALL_SIGN;
            case CRIMSON_SIGN:
                return Material.CRIMSON_WALL_SIGN;
            case WARPED_SIGN:
                return Material.WARPED_WALL_SIGN;
            default:
                return null; // Handle other cases if needed
        }
    }
}
