package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * «Zancada» — el atributo step_height hace que el mob SUBA los escalones al caminar
 * (vanilla lo usa en maxUpStep), y el auto-salto asiste cuando el pathfinding se atasca.
 */
abstract class step_height_base_action implements mutation_action {
    private final attribute_mutation_action delegate;
    private final int assistCooldownTicks;

    protected step_height_base_action(mutation_action_config config,
            double defaultAmount,
            Identifier modifierId,
            int assistCooldownTicks) {
        double amount = config.getDouble("amount", defaultAmount);
        this.delegate = new attribute_mutation_action(passive_attribute_ids.STEP_HEIGHT,
                attribute_mutation_action.Mode.ADD, amount, modifierId);
        this.assistCooldownTicks = Math.max(2, config.getInt("assist_cooldown_ticks", assistCooldownTicks));
    }

    @Override
    public void onApply(LivingEntity entity) {
        delegate.onApply(entity);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        delegate.onRemove(entity);
    }

    @Override
    public void onTick(LivingEntity entity) {
        passive_auto_jump_helper.assist(entity, assistCooldownTicks);
    }
}
