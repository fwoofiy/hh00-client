package dev.hh00.core.setting;

import com.google.gson.JsonElement;

import java.util.function.Consumer;

public abstract class Setting<T> {

    private final String name;
    private final String description;
    private final T defaultValue;

    protected T value;
    private Consumer<T> onChange;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setValue(T value) {
        this.value = value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    public void reset() {
        setValue(defaultValue);
    }

    public Setting<T> onChange(Consumer<T> listener) {
        this.onChange = listener;
        return this;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement json);
}
