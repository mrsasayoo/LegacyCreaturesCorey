package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Permite ajustar el temporizador de invulnerabilidad que controla cuándo una entidad puede recibir daño nuevamente.
 */
@Mixin(Entity.class)
public interface entity_damage_cooldown_accessor {
    @Accessor("timeUntilRegen")
    void legacycreaturescorey$setTimeUntilRegen(int value);
}
