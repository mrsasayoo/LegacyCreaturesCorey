package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Permite ajustar el temporizador de i-frames que controla cuando una entidad puede
 * recibir daño nuevamente.
 * 26.3 VERIFICADO con javap: el gate de hurtServer es LivingEntity.damageCooldownTime
 * (comparado contra lastHurt), NO Entity.invulnerableTime (que es la invulnerabilidad
 * general, p. ej. resistencia). Apuntar al campo equivocado hacía que el daño bonus
 * del critical (amount <= lastHurt) se descartara silenciosamente.
 */
@Mixin(LivingEntity.class)
public interface entity_damage_cooldown_accessor {
    @Accessor("damageCooldownTime")
    void legacycreaturescorey$setTimeUntilRegen(int value);
}
