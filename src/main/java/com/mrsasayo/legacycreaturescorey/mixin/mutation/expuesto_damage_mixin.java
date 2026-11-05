package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * «Expuesto»: amplifica el daño que recibe la entidad marcada (+10% por nivel de
 * amplificador, base 10%) mientras dure el efecto. Se aplica sobre el amount de
 * hurtServer ANTES de las reducciones de armadura (amplifica el daño entrante,
 * igual que lo haría un modifier de daño recibido).
 */
@Mixin(LivingEntity.class)
public abstract class expuesto_damage_mixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float legacycreaturescorey$amplifyExpuesto(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }
        MobEffectInstance instance = self.getEffect(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.EXPUESTO));
        if (instance == null) {
            return amount;
        }
        float bonus = 0.10F + 0.05F * Math.max(0, instance.getAmplifier());
        return amount * (1.0F + bonus);
    }
}
