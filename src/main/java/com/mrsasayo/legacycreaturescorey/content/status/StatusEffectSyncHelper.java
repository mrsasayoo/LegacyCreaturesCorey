package com.mrsasayo.legacycreaturescorey.content.status;

import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.core.network.ModNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Mirrors status effect lifecycle events from the server to interested clients.
 */
public final class StatusEffectSyncHelper {
    private StatusEffectSyncHelper() {}

    public static void handle(LivingEntity entity, StatusEffectInstance effect, boolean start) {
        StatusEffectTicker.handleStatusEffectUpdate(entity, effect, start);

        RegistryEntry<StatusEffect> entry = effect.getEffectType();
        StatusEffect raw = entry.value();
        if (!(raw instanceof NotifyingStatusEffect notifying)) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!start && entity.hasStatusEffect(entry)) {
            return;
        }

        int duration = start ? effect.getDuration() : 0;
        float intensity = start ? notifying.computeIntensity(effect.getAmplifier()) : 0.0F;
        ModNetworking.sendClientEffect(player, new ClientEffectPayload(notifying.getEffectType(), start, duration, intensity));
    }
}
