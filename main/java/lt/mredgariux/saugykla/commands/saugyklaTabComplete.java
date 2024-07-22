package lt.mredgariux.saugykla.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class saugyklaTabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 2 && strings[0].equalsIgnoreCase("hl")) {
            List<String> materials = new ArrayList<>();
            String currentInput = strings[1].toLowerCase();
            for (Material material : Material.values()) {
                if (material.name().toLowerCase().contains(currentInput)) {
                    materials.add(material.name().toLowerCase());
                }
            }
            return materials;
        }
        return null;
    }
}
