package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;

import java.util.List;

public class virulent_growth_aura_1_action implements mutation_action, VirulentSource {
    private final double radius;
    private final int intervalTicks;
    private final int attempts;
    private final double spreadChance;

    public virulent_growth_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 6.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 40));
        this.attempts = Math.max(0, config.getInt("attempts", 3));
        double chance = config.getDouble("spread_chance", 0.65D);
        this.spreadChance = Math.max(0.0D, Math.min(1.0D, chance));
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
        return Mode.FOLIAGE_SPREAD;
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
        return attempts;
    }

    @Override
    public double getSpreadChance() {
        return spreadChance;
    }

    @Override
    public int getStationaryThresholdTicks() {
        return 0;
    }

    @Override
    public List<status_effect_config_parser.status_effect_config_entry> getStationaryEffects() {
        return List.of();
    }

    @Override
    public int getFangCount() {
        return 0;
    }

    @Override
    public int getFangWarmupTicks() {
        return 0;
    }
}
