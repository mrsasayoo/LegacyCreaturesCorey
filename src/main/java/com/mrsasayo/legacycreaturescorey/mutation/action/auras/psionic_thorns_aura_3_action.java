package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;

public class psionic_thorns_aura_3_action implements MutationAction, PsionicThornsSource {
    private static final List<status_effect_config_parser.status_effect_config_entry> DEFAULT_SELF_EFFECTS = List.of(
        new status_effect_config_parser.status_effect_config_entry(
            StatusEffects.SPEED,
            20,
            0,
            true,
            true,
            true),
        new status_effect_config_parser.status_effect_config_entry(
            StatusEffects.STRENGTH,
            20,
            0,
            true,
            true,
            true));
    private final double reflectionPercentage;
    private final double maxDistance;
    private final List<ThornsEffect> effects;
    private final List<ThornsEffect> selfEffects;

    public psionic_thorns_aura_3_action(mutation_action_config config) {
        this.reflectionPercentage = clampPercent(config.getDouble("reflection_percentage", 0.33D));
        this.maxDistance = Math.max(0.0D, config.getDouble("radius", 5.0D));
        this.effects = convertEffects(status_effect_config_parser.parseList(config, "effects", List.of()));
        this.selfEffects = convertEffects(status_effect_config_parser.parseList(config, "self_effects", DEFAULT_SELF_EFFECTS));
        PsionicThornsHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onApply(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        PsionicThornsHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        PsionicThornsHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public double getReflectionPercentage() {
        return reflectionPercentage;
    }

    @Override
    public double getMaxDistance() {
        return maxDistance;
    }

    @Override
    public List<ThornsEffect> getEffects() {
        return effects;
    }

    @Override
    public List<ThornsEffect> getSelfEffects() {
        return selfEffects;
    }

    private double clampPercent(double value) {
        return Math.min(1.0D, Math.max(0.0D, value));
    }

    private List<ThornsEffect> convertEffects(List<status_effect_config_parser.status_effect_config_entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<ThornsEffect> converted = new ArrayList<>(entries.size());
        for (status_effect_config_parser.status_effect_config_entry entry : entries) {
            converted.add(new ThornsEffect(entry.effect(), entry.duration(), entry.amplifier()));
        }
        return List.copyOf(converted);
    }
}
