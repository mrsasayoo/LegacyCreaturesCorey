package com.mrsasayo.legacycreaturescorey.mixin.mechanic;

import com.mrsasayo.legacycreaturescorey.content.status.StatusEffectSyncHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3: yarn onStatusEffectApplied/Upgraded/Removed -> mojmap onEffectAdded/onEffectUpdated/onEffectsRemoved
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectMixin {
    @Shadow public abstract boolean hasEffect(Holder<MobEffect> effect);

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void legacy$syncApplied(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        StatusEffectSyncHelper.handle((LivingEntity) (Object) this, effect, true);
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void legacy$syncUpgraded(MobEffectInstance effect, boolean reapplyEffect, Entity source, CallbackInfo ci) {
        StatusEffectSyncHelper.handle((LivingEntity) (Object) this, effect, true);
    }

    @Inject(method = "onEffectsRemoved", at = @At("TAIL"))
    private void legacy$syncRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        // Iterate the batch removal to mirror each change to the client helper.
        for (MobEffectInstance effect : effects) {
            StatusEffectSyncHelper.handle((LivingEntity) (Object) this, effect, false);
        }
    }
}
