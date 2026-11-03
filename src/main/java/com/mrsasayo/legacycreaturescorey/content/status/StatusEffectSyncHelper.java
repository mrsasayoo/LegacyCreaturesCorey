package com.mrsasayo.legacycreaturescorey.content.status;

import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.core.network.ModNetworking;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mirrors status effect lifecycle events from the server to interested clients.
 */
public final class StatusEffectSyncHelper {
    private StatusEffectSyncHelper() {}

    public static void handle(LivingEntity entity, MobEffectInstance effect, boolean start) {
        StatusEffectTicker.handleStatusEffectUpdate(entity, effect, start);

        Holder<MobEffect> entry = effect.getEffectType();
        MobEffect raw = entry.value();
        if (!(raw instanceof NotifyingStatusEffect notifying)) {
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
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
