package lt.mredgariux.saugykla.utils;

import org.bukkit.ChatColor;

public class chat {
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
