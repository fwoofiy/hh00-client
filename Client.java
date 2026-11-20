package dev.hh00;

import dev.hh00.command.CommandManager;
import dev.hh00.command.commands.BindCommand;
import dev.hh00.command.commands.HelpCommand;
import dev.hh00.command.commands.ToggleCommand;
import dev.hh00.config.ConfigManager;
import dev.hh00.core.KeyBind;
import dev.hh00.core.Module;
import dev.hh00.core.ModuleManager;
import dev.hh00.event.EventBus;
import dev.hh00.event.events.KeyEvent;
import dev.hh00.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central client singleton. Holds every manager and the global event bus.
 */
public final class Client {

    public static final String NAME = "hh00";
    public static final String VERSION = "1.0.0";
    public static final String PREFIX = "\u00a7b[" + NAME + "] \u00a7r";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static Client instance;

    private final EventBus eventBus = new EventBus();
    private final ModuleManager moduleManager = new ModuleManager();
    private final CommandManager commandManager = new CommandManager();
    private final ConfigManager configManager = new ConfigManager();

    /** True while config is being applied, so toggling modules doesn't rewrite the file. */
    private boolean loading;

    private Client() {
    }

    public static Client get() {
        if (instance == null) {
            instance = new Client();
            instance.setup();
        }
        return instance;
    }

    private void setup() {
        commandManager.register(new ToggleCommand());
        commandManager.register(new BindCommand());
        commandManager.register(new HelpCommand());

        eventBus.subscribe(KeyEvent.class, this::onKey);
    }

    /** Called once from the mod initializer. */
    public void init() {
        configManager.load();
        LOGGER.info("{} v{} loaded with {} modules", NAME, VERSION, moduleManager.getModules().size());
        ChatUtil.send("\u00a7aLoaded " + NAME + " v" + VERSION + " \u00a77(" + moduleManager.getModules().size() + " modules)");
    }

    /** Called on client shutdown. */
    public void shutdown() {
        configManager.save();
    }

    private void onKey(KeyEvent event) {
        if (!event.isPressed()) return;
        if (MinecraftClient.getInstance().currentScreen != null) return;

        for (Module module : moduleManager.getModules()) {
            KeyBind bind = module.getKeyBind();
            if (bind.isBound() && bind.getKey() == event.getKey()) {
                module.toggle();
            }
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}
