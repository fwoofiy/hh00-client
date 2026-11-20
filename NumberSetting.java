package dev.hh00.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        this(name, "", defaultValue, min, max, step);
    }

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = clamp(defaultValue);
    }

    @Override
    public void setValue(Double value) {
        super.setValue(clamp(value));
    }

    private double clamp(double input) {
        double clamped = Math.max(min, Math.min(max, input));
        if (step > 0) {
            clamped = Math.round(clamped / step) * step;
            clamped = Math.max(min, Math.min(max, clamped));
        }
        return clamped;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float floatValue() {
        return value.floatValue();
    }

    public int intValue() {
        return value.intValue();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            setValue(json.getAsDouble());
        }
    }
}
