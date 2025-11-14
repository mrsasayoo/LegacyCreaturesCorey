package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import com.mrsasayo.legacycreaturescorey.status.NotifyingStatusEffect;
import com.mrsasayo.legacycreaturescorey.status.StatusEffectTicker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
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
        sendSync(effect, true);
    }

    @Inject(method = "onStatusEffectUpgraded", at = @At("TAIL"))
    private void legacy$syncUpgraded(StatusEffectInstance effect, boolean reapplyEffect, Entity source, CallbackInfo ci) {
        sendSync(effect, true);
    }

    @Inject(method = "onStatusEffectRemoved", at = @At("TAIL"))
    private void legacy$syncRemoved(StatusEffectInstance effect, CallbackInfo ci) {
        sendSync(effect, false);
    }

    private void sendSync(StatusEffectInstance effect, boolean start) {
        LivingEntity self = (LivingEntity) (Object) this;
        StatusEffectTicker.handleStatusEffectUpdate(self, effect, start);

        RegistryEntry<StatusEffect> entry = effect.getEffectType();
        StatusEffect raw = entry.value();
        if (!(raw instanceof NotifyingStatusEffect notifying)) {
            return;
        }
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!start && hasStatusEffect(entry)) {
            return;
        }
        int duration = start ? effect.getDuration() : 0;
        float intensity = start ? notifying.computeIntensity(effect.getAmplifier()) : 0.0F;
        ModNetworking.sendClientEffect(player, new ClientEffectPayload(notifying.getEffectType(), start, duration, intensity));
    }
}
