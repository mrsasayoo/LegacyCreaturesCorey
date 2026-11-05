package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Auto-salto compartido (Zancada / Saltarina): el pathfinding vanilla no planifica
 * saltos potenciados y a veces se atasca ante obstáculos escalables; cuando el mob
 * choca de frente en el suelo, saltamos con SU potencia para superar el muro.
 */
public final class passive_auto_jump_helper {
    private passive_auto_jump_helper() {
    }

    private static final Map<LivingEntity, Long> LAST_ASSIST = new WeakHashMap<>();

    public static void assist(LivingEntity entity, int cooldownTicks) {
        if (!action_context_isServer(entity)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel world)) {
            return;
        }
        long time = world.getGameTime();
        Long last = LAST_ASSIST.get(entity);
        if (last != null && time - last < cooldownTicks) {
            return;
        }
        if (entity.onGround() && entity.horizontalCollision) {
            Vec3 delta = entity.getDeltaMovement();
            entity.setDeltaMovement(delta.x, Math.max(0.42D, entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT)), delta.z);
            LAST_ASSIST.put(entity, time);
        }
    }

    /** Variante Saltarina: el salto usa la potencia del atributo jump_strength (x1.5/x2.0). */
    public static void assistWithJumpPower(LivingEntity entity, int cooldownTicks) {
        if (entity.level() == null || entity.level().isClientSide()
                || !(entity.level() instanceof ServerLevel world)) {
            return;
        }
        long time = world.getGameTime();
        Long last = LAST_ASSIST.get(entity);
        if (last != null && time - last < cooldownTicks) {
            return;
        }
        if (entity.onGround() && entity.horizontalCollision) {
            Vec3 delta = entity.getDeltaMovement();
            double power = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
            entity.setDeltaMovement(delta.x, Math.max(0.42D, power), delta.z);
            LAST_ASSIST.put(entity, time);
        }
    }

    private static boolean action_context_isServer(LivingEntity entity) {
        return entity.level() != null && !entity.level().isClientSide();
    }
}
