package dev.hh00.core;

import dev.hh00.module.modules.movement.Sprint;
import dev.hh00.module.modules.render.Hud;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final Map<String, Module> byName = new LinkedHashMap<>();

    public ModuleManager() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(new Sprint());
        register(new Hud());
    }

    public void register(Module module) {
        String key = module.getName().toLowerCase(Locale.ROOT);
        if (byName.containsKey(key)) {
            throw new IllegalStateException("Duplicate module name: " + module.getName());
        }
        byClass.put(module.getClass(), module);
        byName.put(key, module);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) byClass.get(type);
    }

    public Module get(String name) {
        return byName.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Module> getModules() {
        return byName.values();
    }

    public List<Module> getByCategory(Category category) {
        return byName.values().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    public boolean isEnabled(Class<? extends Module> type) {
        Module module = byClass.get(type);
        return module != null && module.isEnabled();
    }

    public List<Module> getEnabled() {
        return byName.values().stream().filter(Module::isEnabled).collect(Collectors.toList());
    }
}
