package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Permite ajustar el temporizador de invulnerabilidad que controla cuando una entidad puede recibir dano nuevamente.
 * 26.3: yarn timeUntilRegen -> mojmap invulnerableTime
 */
@Mixin(Entity.class)
public interface entity_damage_cooldown_accessor {
    @Accessor("invulnerableTime")
    void legacycreaturescorey$setTimeUntilRegen(int value);
}
