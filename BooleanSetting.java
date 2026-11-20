package dev.hh00.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue) {
        this(name, "", defaultValue);
    }

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() {
        setValue(!value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            setValue(json.getAsBoolean());
        }
    }
}
