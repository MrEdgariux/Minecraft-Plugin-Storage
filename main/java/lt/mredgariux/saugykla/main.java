package lt.mredgariux.saugykla;

import lt.mredgariux.saugykla.commands.saugyklaCommand;
import lt.mredgariux.saugykla.commands.saugyklaTabComplete;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;

public final class main extends JavaPlugin {

    public ChestManagement chestManagement;
    public RecipeManagement recipeManagement;

    @Override
    public void onEnable() {

        chestManagement = new ChestManagement(this);
        recipeManagement = new RecipeManagement(this);

        // Register command "saugykla"
        Objects.requireNonNull(this.getCommand("saugykla")).setExecutor(new saugyklaCommand());
        Objects.requireNonNull(this.getCommand("saugykla")).setTabCompleter(new saugyklaTabComplete());

        // Register events
        this.getServer().getPluginManager().registerEvents(new saugyklaCommand(), this);

        this.getLogger().info("Paleidau plugina pydere");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        this.getLogger().info("Palauk, parduodu turguj bandeles");
    }
}
