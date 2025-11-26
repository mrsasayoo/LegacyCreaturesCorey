package com.mrsasayo.legacycreaturescorey.mutation.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

/**
 * Wrapper ligero sobre la sección {@code actions} de cada JSON de mutación para facilitar la lectura
 * de parámetros tipados.
 */
public final class mutation_action_config {
    private final JsonObject data;

    public mutation_action_config(JsonObject data) {
        this.data = data == null ? new JsonObject() : data;
    }

    public double getDouble(String key, double defaultValue) {
        JsonElement element = data.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsDouble() : defaultValue;
    }

    public float getFloat(String key, float defaultValue) {
        JsonElement element = data.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsFloat() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        JsonElement element = data.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        JsonElement element = data.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsLong() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement element = data.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        JsonElement element = data.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : defaultValue;
    }

    public Identifier getIdentifier(String key, Identifier defaultValue) {
        JsonElement element = data.get(key);
        if (element != null && element.isJsonPrimitive()) {
            String raw = element.getAsString();
            if (!raw.isEmpty()) {
                return Identifier.tryParse(raw);
            }
        }
        return defaultValue;
    }

    public mutation_action_config getObject(String key) {
        JsonElement element = data.get(key);
        if (element != null && element.isJsonObject()) {
            return new mutation_action_config(element.getAsJsonObject());
        }
        return new mutation_action_config(new JsonObject());
    }

    public JsonObject raw() {
        return data;
    }
}
