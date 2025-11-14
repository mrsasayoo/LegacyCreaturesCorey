package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.status.HealingReductionStatusEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adjusts the amount of healing received when custom mortal wound effects are active.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float legacy$reduceHealing(float amount) {
        if (amount <= 0.0F) {
            return amount;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        float multiplier = 1.0F;

        for (StatusEffectInstance instance : self.getStatusEffects()) {
            RegistryEntry<StatusEffect> entry = instance.getEffectType();
            StatusEffect effect = entry.value();
            if (effect instanceof HealingReductionStatusEffect reduction) {
                multiplier = Math.min(multiplier, reduction.getHealingMultiplier(instance.getAmplifier()));
                if (multiplier <= 0.0F) {
                    return 0.0F;
                }
            }
        }
        return amount * multiplier;
    }
}
