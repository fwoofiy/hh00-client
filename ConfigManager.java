package dev.hh00.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hh00.Client;
import dev.hh00.core.Module;
import dev.hh00.core.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON persistence for module states, keybinds, settings and the command prefix.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;

    public ConfigManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir()
                .resolve("clientbase")
                .resolve("config.json");
    }

    public void save() {
        if (Client.get().isLoading()) return;

        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();

        for (Module module : Client.get().getModuleManager().getModules()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            moduleJson.addProperty("key", module.getKeyBind().getKey());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                settings.add(setting.getName(), setting.toJson());
            }
            moduleJson.add("settings", settings);

            modules.add(module.getName(), moduleJson);
        }

        root.add("modules", modules);
        root.addProperty("prefix", Client.get().getCommandManager().getPrefix());

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(root));
        } catch (IOException e) {
            Client.LOGGER.error("Failed to save config to {}", configPath, e);
        }
    }

    public void load() {
        if (!Files.exists(configPath)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
            Client.get().setLoading(true);

            if (root.has("prefix")) {
                String prefix = root.get("prefix").getAsString();
                if (!prefix.isBlank()) {
                    Client.get().getCommandManager().setPrefix(prefix);
                }
            }

            JsonObject modules = root.getAsJsonObject("modules");
            if (modules != null) {
                for (Module module : Client.get().getModuleManager().getModules()) {
                    JsonElement moduleElement = modules.get(module.getName());
                    if (moduleElement == null || !moduleElement.isJsonObject()) continue;

                    JsonObject moduleJson = moduleElement.getAsJsonObject();

                    if (moduleJson.has("key")) {
                        module.getKeyBind().setKey(moduleJson.get("key").getAsInt());
                    }

                    JsonObject settings = moduleJson.getAsJsonObject("settings");
                    if (settings != null) {
                        for (Setting<?> setting : module.getSettings()) {
                            JsonElement value = settings.get(setting.getName());
                            if (value != null) {
                                setting.fromJson(value);
                            }
                        }
                    }

                    if (moduleJson.has("enabled")) {
                        module.setEnabled(moduleJson.get("enabled").getAsBoolean());
                    }
                }
            }
        } catch (Exception e) {
            Client.LOGGER.error("Failed to load config from {}", configPath, e);
        } finally {
            Client.get().setLoading(false);
        }
    }

    public Path getConfigPath() {
        return configPath;
    }
}
