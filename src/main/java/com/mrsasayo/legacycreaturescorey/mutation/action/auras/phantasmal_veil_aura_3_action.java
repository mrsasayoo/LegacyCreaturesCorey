package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public class phantasmal_veil_aura_3_action implements mutation_action, PhantasmalSource {
    private final double radius;
    private final int intervalTicks;
    private final int shroudVisibleTicks;
    private final int shroudInvisibleTicks;
    private final List<status_effect_config_parser.status_effect_config_entry> shroudEffects;

    public phantasmal_veil_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 32.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 60));
        this.shroudVisibleTicks = Math.max(2, config.getInt("shroud_visible_ticks", 40));
        this.shroudInvisibleTicks = Math.max(2, config.getInt("shroud_invisible_ticks", 20));
        this.shroudEffects = resolveEffects(config, shroudInvisibleTicks);
        PhantasmalHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        PhantasmalHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        PhantasmalHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.ALLY_SHROUD;
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
    public int getParticleCount() {
        return 0;
    }

    @Override
    public int getCloneMinCount() {
        return 0;
    }

    @Override
    public int getCloneMaxCount() {
        return 0;
    }

    @Override
    public int getCloneLifetimeTicks() {
        return 0;
    }

    @Override
    public boolean shouldCloneGlow() {
        return false;
    }

    @Override
    public int getShroudVisibleTicks() {
        return shroudVisibleTicks;
    }

    @Override
    public int getShroudInvisibleTicks() {
        return shroudInvisibleTicks;
    }

    @Override
    public List<status_effect_config_parser.status_effect_config_entry> getShroudEffects() {
        return shroudEffects;
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDurationTicks) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(StatusEffects.INVISIBILITY,
                    fallbackDurationTicks,
                    0,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }
}
