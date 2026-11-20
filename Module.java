package dev.hh00.core;

import dev.hh00.Client;
import dev.hh00.core.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every cheat feature.
 *
 * <p>While enabled, the module is registered on the {@link dev.hh00.event.EventBus}, so any
 * method annotated with {@link dev.hh00.event.Subscribe} will receive events. Unregistering on
 * disable keeps event dispatch cheap.</p>
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private final KeyBind keyBind = KeyBind.unbound();
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean state) {
        if (this.enabled == state) return;
        this.enabled = state;

        if (state) {
            Client.get().getEventBus().register(this);
            onEnable();
        } else {
            onDisable();
            Client.get().getEventBus().unregister(this);
        }

        if (!Client.get().isLoading()) {
            Client.get().getConfigManager().save();
        }
    }

    /** Called after the module was enabled and its listeners were registered. */
    protected void onEnable() {
    }

    /** Called before the module is disabled and its listeners are removed. */
    protected void onDisable() {
    }

    protected <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public KeyBind getKeyBind() {
        return keyBind;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return name;
    }
}
