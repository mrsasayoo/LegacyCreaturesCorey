package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public class vanguards_bulwark_aura_1_action implements MutationAction, VanguardsBulwarkSource {
    private static final List<status_effect_config_parser.status_effect_config_entry> DEFAULT_EFFECTS = List.of(
            new status_effect_config_parser.status_effect_config_entry(
                    StatusEffects.REGENERATION,
                    40,
                    0,
                    true,
                    true,
                    true));

    private final double radius;
    private final int intervalTicks;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public vanguards_bulwark_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 6.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 20));
        this.effects = status_effect_config_parser.parseList(config, "effects", DEFAULT_EFFECTS);
        VanguardsBulwarkHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        VanguardsBulwarkHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        VanguardsBulwarkHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.REGENERATION;
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
    public List<status_effect_config_parser.status_effect_config_entry> getStatusEffects() {
        return effects;
    }

    @Override
    public int getPotionInterval() {
        return 0;
    }

    @Override
    public float getHealAmount() {
        return 0;
    }
}
