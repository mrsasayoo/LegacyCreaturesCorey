package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class bleeding_base_action extends ProcOnHitAction {
    private final float[] damagePulses;
    private final int intervalTicks;
    private final int effectAmplifier;

    protected bleeding_base_action(mutation_action_config config,
            double defaultChance,
            float[] defaultPulses,
            int defaultIntervalTicks,
            int defaultEffectLevel) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.damagePulses = parsePulses(config, defaultPulses);
        this.intervalTicks = resolveInterval(config, defaultIntervalTicks);
        int configuredLevel = config.getInt("effect_level", defaultEffectLevel);
        this.effectAmplifier = Math.max(1, configuredLevel) - 1;
    }

    static float[] parsePulses(mutation_action_config config, float[] fallback) {
        if (fallback == null || fallback.length == 0) {
            throw new IllegalArgumentException("Se requiere al menos un pulso de daño por defecto");
        }
        JsonArray array = null;
        if (config != null) {
            JsonElement element = config.raw().get("damage_pulses");
            if (element != null && element.isJsonArray()) {
                array = element.getAsJsonArray();
            }
        }
        if (array == null || array.isEmpty()) {
            return Arrays.copyOf(fallback, fallback.length);
        }
        List<Float> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            values.add(Math.max(0.0F, element.getAsFloat()));
        }
        if (values.isEmpty()) {
            return Arrays.copyOf(fallback, fallback.length);
        }
        float[] parsed = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            parsed[i] = values.get(i);
        }
        return parsed;
    }

    static int resolveInterval(mutation_action_config config, int defaultIntervalTicks) {
        int configuredTicks = config.getInt("interval_ticks", -1);
        if (configuredTicks <= 0) {
            int seconds = config.getInt("interval_seconds", Math.max(1, defaultIntervalTicks / 20));
            configuredTicks = seconds * 20;
        }
        return Math.max(1, configuredTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        bleeding_effect_helper.apply(world, attacker, victim, damagePulses, intervalTicks, effectAmplifier);
    }

    protected int getEffectAmplifier() {
        return effectAmplifier;
    }
}
