package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;

public class virulent_growth_aura_2_action implements mutation_action, VirulentSource {
    private final double radius;
    private final int intervalTicks;
    private final int stationaryThresholdTicks;
    private final List<status_effect_config_parser.status_effect_config_entry> stationaryEffects;

    public virulent_growth_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 6.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 20));
        this.stationaryThresholdTicks = Math.max(0, config.getInt("stationary_threshold_ticks", 40));
        this.stationaryEffects = resolveStationaryEffects(config);
        VirulentHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        VirulentHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        VirulentHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.STATIONARY_POISON;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public int getAttempts() {
        return 0;
    }

    @Override
    public double getSpreadChance() {
        return 0;
    }

    @Override
    public int getStationaryThresholdTicks() {
        return stationaryThresholdTicks;
    }

    @Override
    public List<status_effect_config_parser.status_effect_config_entry> getStationaryEffects() {
        return stationaryEffects;
    }

    @Override
    public int getFangCount() {
        return 0;
    }

    @Override
    public int getFangWarmupTicks() {
        return 0;
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveStationaryEffects(
            mutation_action_config config) {
        int defaultPoisonDuration = Math.max(0, config.getInt("poison_duration_ticks", 80));
        int defaultPoisonAmplifier = Math.max(0, config.getInt("poison_amplifier", 0));
        List<status_effect_config_parser.status_effect_config_entry> fallback = new ArrayList<>();
        if (defaultPoisonDuration > 0) {
            fallback.add(status_effect_config_parser.createEntry(StatusEffects.POISON,
                    defaultPoisonDuration,
                    defaultPoisonAmplifier,
                    true,
                    true,
                    true));
        }
        fallback.add(status_effect_config_parser.createEntry(StatusEffects.SLOWNESS,
                60,
                0,
                true,
                true,
                true));
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }
}
