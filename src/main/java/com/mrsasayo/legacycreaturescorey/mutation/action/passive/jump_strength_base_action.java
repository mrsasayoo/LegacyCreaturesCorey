package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * «Saltarina» — dos piezas:
 * 1. El atributo jump_strength en MULTIPLY (el impulso del salto SÍ es más fuerte).
 * 2. Auto-salto inteligente: el pathfinding vanilla no planifica saltos de más de 1
 *    bloque ni "sabe" del salto potenciado; cuando el mob choca de frente contra un
 *    obstáculo en el suelo, saltamos con SU potencia (x1.5/x2.0) para que el muro
 *    escalable según su nivel se supere de verdad.
 */
abstract class jump_strength_base_action implements mutation_action {
    private static final Map<LivingEntity, Long> LAST_ASSIST = new WeakHashMap<>();

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
        if (!action_context.isServer(entity) || !(entity.level() instanceof ServerLevel world)) {
            return;
        }
        long time = world.getGameTime();
        Long last = LAST_ASSIST.get(entity);
        if (last != null && time - last < assistCooldownTicks) {
            return;
        }
        if (entity.onGround() && entity.horizontalCollision) {
            Vec3 delta = entity.getDeltaMovement();
            double power = entity.getAttributeValue(Attributes.JUMP_STRENGTH);
            entity.setDeltaMovement(delta.x, Math.max(0.42D, power), delta.z);
            LAST_ASSIST.put(entity, time);
        }
    }
}
