package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;

import java.util.List;

public class virulent_growth_aura_3_action implements mutation_action, VirulentSource {
    private final double radius;
    private final int intervalTicks;
    private final int fangCount;
    private final int fangWarmupTicks;

    public virulent_growth_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 8.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 120));
        this.fangCount = Math.max(0, config.getInt("fang_count", 3));
        this.fangWarmupTicks = Math.max(0, config.getInt("fang_warmup_ticks", 10));
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
        return Mode.ROOT_SPIKES;
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
        return 0;
    }

    @Override
    public List<status_effect_config_parser.status_effect_config_entry> getStationaryEffects() {
        return List.of();
    }

    @Override
    public int getFangCount() {
        return fangCount;
    }

    @Override
    public int getFangWarmupTicks() {
        return fangWarmupTicks;
    }
}
