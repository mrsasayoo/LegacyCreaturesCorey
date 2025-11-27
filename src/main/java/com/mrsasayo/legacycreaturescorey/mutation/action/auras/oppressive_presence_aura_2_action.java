package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public class oppressive_presence_aura_2_action implements mutation_action, OppressivePresenceSource {
    private static final List<status_effect_config_parser.status_effect_config_entry> DEFAULT_EFFECTS = List.of(
            new status_effect_config_parser.status_effect_config_entry(
                    StatusEffects.SLOWNESS,
                    40,
                    0,
                    true,
                    true,
                    true),
            new status_effect_config_parser.status_effect_config_entry(
                    StatusEffects.WEAKNESS,
                    40,
                    0,
                    true,
                    true,
                    true));

    private final double radius;
    private final int tickInterval;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public oppressive_presence_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 5.0D);
        this.tickInterval = Math.max(1, config.getInt("tick_interval", 20));
        this.effects = status_effect_config_parser.parseList(config, "effects", DEFAULT_EFFECTS);
        OppressivePresenceHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        OppressivePresenceHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        OppressivePresenceHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getTickInterval() {
        return tickInterval;
    }

    @Override
    public List<status_effect_config_parser.status_effect_config_entry> getEffectConfigs() {
        return effects;
    }
}
