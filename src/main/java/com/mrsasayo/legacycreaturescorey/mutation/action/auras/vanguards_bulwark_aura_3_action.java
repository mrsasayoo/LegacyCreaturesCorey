package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public class vanguards_bulwark_aura_3_action implements mutation_action, VanguardsBulwarkSource {
    private static final List<status_effect_config_parser.status_effect_config_entry> DEFAULT_EFFECTS = List.of(
            new status_effect_config_parser.status_effect_config_entry(
                    StatusEffects.REGENERATION,
                    40,
                    1,
                    true,
                    true,
                    true));

    private final double radius;
    private final int potionInterval;
    private final float healAmount;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public vanguards_bulwark_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 8.0D);
        this.potionInterval = Math.max(1, config.getInt("potion_interval", 140));
        this.healAmount = (float) config.getDouble("heal_amount", 10.0D);
        this.effects = status_effect_config_parser.parseList(config, "effects", DEFAULT_EFFECTS);
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
        return Mode.CONDITIONAL;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getIntervalTicks() {
        return 0;
    }

    @Override
    public List<status_effect_config_parser.status_effect_config_entry> getStatusEffects() {
        return effects;
    }

    @Override
    public int getPotionInterval() {
        return potionInterval;
    }

    @Override
    public float getHealAmount() {
        return healAmount;
    }
}
