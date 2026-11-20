package dev.hh00.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        this(name, "", defaultValue, modes);
    }

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        if (modes.length == 0) {
            throw new IllegalArgumentException("ModeSetting needs at least one mode: " + name);
        }
        this.modes = List.of(modes);
        this.value = this.modes.contains(defaultValue) ? defaultValue : this.modes.get(0);
    }

    public List<String> getModes() {
        return modes;
    }

    public String getMode() {
        return value;
    }

    public void setMode(String mode) {
        if (modes.contains(mode)) {
            setValue(mode);
        }
    }

    public void cycle() {
        int index = modes.indexOf(value);
        setValue(modes.get((index + 1) % modes.size()));
    }

    public boolean is(String mode) {
        return value.equalsIgnoreCase(mode);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            setMode(json.getAsString());
        }
    }

    @Override
    public String toString() {
        return value + " " + Arrays.toString(modes.toArray());
    }
}
