package lt.mredgariux.saugykla;

import java.util.Objects;
import lt.mredgariux.saugykla.command.SaugyklaCommand;
import lt.mredgariux.saugykla.command.SaugyklaTabCompleter;
import lt.mredgariux.saugykla.listener.StorageListener;
import lt.mredgariux.saugykla.persistence.RegionRepository;
import lt.mredgariux.saugykla.persistence.ResourceRepository;
import lt.mredgariux.saugykla.persistence.StorageRepository;
import lt.mredgariux.saugykla.service.ResourceService;
import lt.mredgariux.saugykla.service.StorageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaugyklaPlugin extends JavaPlugin {
    private StorageService storageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ResourceService resourceService = new ResourceService(this, new ResourceRepository(this));
        storageService = new StorageService(
                this,
                resourceService,
                new StorageRepository(this),
                new RegionRepository(this));

        SaugyklaCommand commandHandler = new SaugyklaCommand(this, storageService, resourceService);
        PluginCommand command = Objects.requireNonNull(
                getCommand("saugykla"), "Command 'saugykla' is missing from plugin.yml");
        command.setExecutor(commandHandler);
        command.setTabCompleter(new SaugyklaTabCompleter());
        getServer().getPluginManager().registerEvents(
                new StorageListener(storageService, resourceService), this);

        getLogger().info("Saugykla enabled.");
    }

    @Override
    public void onDisable() {
        if (storageService != null) {
            storageService.stopHighlights();
        }
        getLogger().info("Saugykla disabled.");
    }
}
