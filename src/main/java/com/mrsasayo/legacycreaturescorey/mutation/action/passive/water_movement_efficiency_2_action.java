package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * «Aleta» — asistencia de nado en onTick. El atributo vanilla
 * water_movement_efficiency solo añade aceleración mínima (por eso no se notaba en el
 * ahogado); aquí compensamos el arrastre del agua (x0.8) multiplicando la velocidad
 * horizontal mientras nada, con tope de velocidad para no romper el balance.
 */
public final class water_movement_efficiency_2_action implements mutation_action {
    private final double dragCompensation;
    private final double maxSpeed;

    public water_movement_efficiency_2_action(mutation_action_config config) {
        this.dragCompensation = Math.max(1.0D, config.getDouble("drag_compensation", 1.45D));
        this.maxSpeed = config.getDouble("max_water_speed", 0.4D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity) || !entity.isInWater()) {
            return;
        }
        Vec3 delta = entity.getDeltaMovement();
        double hSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (hSpeed < 0.02D) {
            return;
        }
        double target = Math.min(hSpeed * dragCompensation, maxSpeed);
        double scale = target / hSpeed;
        entity.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
    }
}
