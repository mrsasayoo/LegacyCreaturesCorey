package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.content.status.HealingReductionStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adjusts the amount of healing received when custom mortal wound effects are active.
 * 26.3: yarn getStatusEffects/getEffectType -> mojmap getActiveEffects/getEffect
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

        for (MobEffectInstance instance : self.getActiveEffects()) {
            Holder<MobEffect> entry = instance.getEffect();
            MobEffect effect = entry.value();
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
