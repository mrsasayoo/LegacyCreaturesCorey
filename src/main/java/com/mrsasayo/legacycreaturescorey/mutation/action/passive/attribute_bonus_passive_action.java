package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Objects;

/**
 * Contiene la lógica común para mutaciones pasivas que aplican bonificaciones a atributos base.
 */
abstract class attribute_bonus_passive_action implements MutationAction {
    private final AttributeMutationAction delegate;

    protected attribute_bonus_passive_action(mutation_action_config config,
            Identifier defaultAttribute,
            AttributeMutationAction.Mode defaultMode,
            double defaultAmount) {
        this(config, defaultAttribute, defaultMode, defaultAmount, null);
    }

    protected attribute_bonus_passive_action(mutation_action_config config,
            Identifier defaultAttribute,
            AttributeMutationAction.Mode defaultMode,
            double defaultAmount,
            Identifier defaultModifierId) {
        Identifier attributeId = Objects.requireNonNullElse(config.getIdentifier("attribute", defaultAttribute),
                defaultAttribute);
        AttributeMutationAction.Mode mode = parseMode(config.getString("mode", defaultMode.name()), defaultMode);
        double amount = config.getDouble("amount", defaultAmount);
        Identifier modifierId = config.getIdentifier("modifier_id", defaultModifierId);
        this.delegate = new AttributeMutationAction(attributeId, mode, amount, modifierId);
    }

    private AttributeMutationAction.Mode parseMode(String raw, AttributeMutationAction.Mode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return AttributeMutationAction.Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
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
