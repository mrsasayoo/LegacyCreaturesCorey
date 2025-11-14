package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.status.StatusEffectSyncHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectMixin {
    @Shadow public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Inject(method = "onStatusEffectApplied", at = @At("TAIL"))
    private void legacy$syncApplied(StatusEffectInstance effect, Entity source, CallbackInfo ci) {
        StatusEffectSyncHelper.handle((LivingEntity) (Object) this, effect, true);
    }

    @Inject(method = "onStatusEffectUpgraded", at = @At("TAIL"))
    private void legacy$syncUpgraded(StatusEffectInstance effect, boolean reapplyEffect, Entity source, CallbackInfo ci) {
        StatusEffectSyncHelper.handle((LivingEntity) (Object) this, effect, true);
    }

    @Inject(method = "onStatusEffectsRemoved", at = @At("TAIL"))
    private void legacy$syncRemoved(Collection<StatusEffectInstance> effects, CallbackInfo ci) {
        // Iterate the batch removal to mirror each change to the client helper.
        for (StatusEffectInstance effect : effects) {
            StatusEffectSyncHelper.handle((LivingEntity) (Object) this, effect, false);
        }
    }
}
