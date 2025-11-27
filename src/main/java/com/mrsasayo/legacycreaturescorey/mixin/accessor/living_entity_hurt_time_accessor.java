package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Expone el contador de animación de daño para reiniciarlo cuando aplicamos pulsos adicionales.
 */
@Mixin(LivingEntity.class)
public interface living_entity_hurt_time_accessor {
    @Accessor("hurtTime")
    void legacycreaturescorey$setHurtTime(int value);
}
