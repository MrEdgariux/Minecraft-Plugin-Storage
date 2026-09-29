package lt.mredgariux.saugykla.util;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;

public final class SignMaterials {
    private static final Map<Material, Material> WALL_SIGNS = createWallSigns();

    private SignMaterials() {
    }

    public static boolean isStandingSign(Material material) {
        return WALL_SIGNS.containsKey(material);
    }

    public static Material wallSignFor(Material material) {
        return WALL_SIGNS.get(material);
    }

    public static Material standingSignFor(Material material) {
        return WALL_SIGNS.entrySet().stream()
                .filter(entry -> entry.getValue() == material)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private static Map<Material, Material> createWallSigns() {
        Map<Material, Material> signs = new EnumMap<>(Material.class);
        signs.put(Material.OAK_SIGN, Material.OAK_WALL_SIGN);
        signs.put(Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN);
        signs.put(Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN);
        signs.put(Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN);
        signs.put(Material.SPRUCE_SIGN, Material.SPRUCE_WALL_SIGN);
        signs.put(Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN);
        signs.put(Material.BAMBOO_SIGN, Material.BAMBOO_WALL_SIGN);
        signs.put(Material.CHERRY_SIGN, Material.CHERRY_WALL_SIGN);
        signs.put(Material.MANGROVE_SIGN, Material.MANGROVE_WALL_SIGN);
        signs.put(Material.CRIMSON_SIGN, Material.CRIMSON_WALL_SIGN);
        signs.put(Material.WARPED_SIGN, Material.WARPED_WALL_SIGN);
        return Map.copyOf(signs);
    }
}
