package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public class vanguards_bulwark_aura_2_action implements mutation_action, VanguardsBulwarkSource {
    // Nivel 1 heredado: Regeneración I continua a los aliados.
    private static final List<status_effect_config_parser.status_effect_config_entry> DEFAULT_EFFECTS = List.of(
            new status_effect_config_parser.status_effect_config_entry(
                    MobEffects.REGENERATION,
                    40,
                    0,
                    true,
                    true,
                    true));

    // Pulso propio del nivel 2: Resistencia I durante 2s cada 4s.
    private static final List<status_effect_config_parser.status_effect_config_entry> DEFAULT_PULSE_EFFECTS = List.of(
            new status_effect_config_parser.status_effect_config_entry(
                    MobEffects.RESISTANCE,
                    40,
                    0,
                    true,
                    true,
                    true));

    private final double radius;
    private final int intervalTicks;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;
    private final List<status_effect_config_parser.status_effect_config_entry> pulseEffects;
    private final int pulseIntervalTicks;

    public vanguards_bulwark_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 8.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 20));
        this.effects = status_effect_config_parser.parseList(config, "effects", DEFAULT_EFFECTS);
        this.pulseEffects = status_effect_config_parser.parseList(config, "pulse_effects", DEFAULT_PULSE_EFFECTS);
        this.pulseIntervalTicks = Math.max(0, config.getInt("pulse_interval_ticks", 80));
        VanguardsBulwarkHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        VanguardsBulwarkHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        VanguardsBulwarkHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.RESISTANCE;
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
    public List<status_effect_config_parser.status_effect_config_entry> getPulseEffects() {
        return pulseEffects;
    }

    @Override
    public int getPulseIntervalTicks() {
        return pulseIntervalTicks;
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
