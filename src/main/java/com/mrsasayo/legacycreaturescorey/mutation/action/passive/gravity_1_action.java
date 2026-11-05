package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * «Peso de Montaña» — gravedad extra SOLO mientras cae: el impulso del salto queda
 * intacto (el mob puede saltar y perseguirte) y una vez en el aire descendiendo
 * acelera hacia abajo, aplanando arcos de knockback.
 * NOTA 26.3: el atributo minecraft:gravity directo rompía el salto (0.08+0.05 = doble
 * gravedad incluso al despegar); PASSIVE ahora tickea y esto vive en onTick.
 */
public final class gravity_1_action implements mutation_action {
    private final double fallAcceleration;

    public gravity_1_action(mutation_action_config config) {
        this.fallAcceleration = Math.max(0.0D, config.getDouble("fall_acceleration", 0.04D));
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        Vec3 delta = entity.getDeltaMovement();
        if (!entity.onGround() && delta.y < 0.0D) {
            entity.setDeltaMovement(delta.x, delta.y - fallAcceleration, delta.z);
        }
    }
}
