package lt.mredgariux.saugykla.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class SaugyklaTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("r", "hl", "chunks", "s", "reset", "debug");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hl")) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return Arrays.stream(Material.values())
                    .map(material -> material.name().toLowerCase(Locale.ROOT))
                    .filter(name -> name.startsWith(input))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("chunks")) {
            return matching(List.of("start", "end"), args[1]);
        }
        return List.of();
    }

    private static List<String> matching(List<String> values, String input) {
        String lowercaseInput = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(lowercaseInput)).toList();
    }
}
