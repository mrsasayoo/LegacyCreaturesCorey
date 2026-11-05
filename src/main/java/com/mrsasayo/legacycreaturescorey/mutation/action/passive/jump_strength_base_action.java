package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.action.passive.passive_auto_jump_helper;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * «Saltarina» — dos piezas:
 * 1. El atributo jump_strength en MULTIPLY (el impulso del salto SÍ es más fuerte).
 * 2. Auto-salto inteligente: el pathfinding vanilla no planifica saltos de más de 1
 *    bloque ni "sabe" del salto potenciado; cuando el mob choca de frente contra un
 *    obstáculo en el suelo, saltamos con SU potencia (x1.5/x2.0) para que el muro
 *    escalable según su nivel se supere de verdad.
 */
abstract class jump_strength_base_action implements mutation_action {
    private final attribute_mutation_action delegate;
    private final int assistCooldownTicks;

    protected jump_strength_base_action(mutation_action_config config,
            double defaultMultiplierAmount,
            Identifier modifierId,
            int assistCooldownTicks) {
        double amount = config.getDouble("amount", defaultMultiplierAmount);
        this.delegate = new attribute_mutation_action(passive_attribute_ids.JUMP_STRENGTH,
                attribute_mutation_action.Mode.MULTIPLY, amount, modifierId);
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
        // Auto-salto con la potencia del atributo jump_strength (x1.5/x2.0).
        passive_auto_jump_helper.assistWithJumpPower(entity, assistCooldownTicks);
    }
}
