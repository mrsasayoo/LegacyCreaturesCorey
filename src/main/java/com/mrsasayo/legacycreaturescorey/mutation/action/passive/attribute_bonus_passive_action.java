package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

/**
 * Contiene la lógica común para mutaciones pasivas que aplican bonificaciones a atributos base.
 */
abstract class attribute_bonus_passive_action implements mutation_action {
    private final attribute_mutation_action delegate;

    protected attribute_bonus_passive_action(mutation_action_config config,
            ResourceLocation defaultAttribute,
            attribute_mutation_action.Mode defaultMode,
            double defaultAmount) {
        this(config, defaultAttribute, defaultMode, defaultAmount, null);
    }

    protected attribute_bonus_passive_action(mutation_action_config config,
            ResourceLocation defaultAttribute,
            attribute_mutation_action.Mode defaultMode,
            double defaultAmount,
            ResourceLocation defaultModifierId) {
        ResourceLocation attributeId = Objects.requireNonNullElse(config.getIdentifier("attribute", defaultAttribute),
                defaultAttribute);
        attribute_mutation_action.Mode mode = parseMode(config.getString("mode", defaultMode.name()), defaultMode);
        double amount = config.getDouble("amount", defaultAmount);
        ResourceLocation modifierId = config.getIdentifier("modifier_id", defaultModifierId);
        this.delegate = new attribute_mutation_action(attributeId, mode, amount, modifierId);
    }

    private attribute_mutation_action.Mode parseMode(String raw, attribute_mutation_action.Mode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return attribute_mutation_action.Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    @Override
    public void onApply(LivingEntity entity) {
        delegate.onApply(entity);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        delegate.onRemove(entity);
    }
}
