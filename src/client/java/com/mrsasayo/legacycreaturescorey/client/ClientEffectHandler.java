package com.mrsasayo.legacycreaturescorey.client;

import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Handles transient feedback-only effects requested by the server.
 */
public final class ClientEffectHandler {
    private static final Map<ClientEffectType, ActiveEffect> ACTIVE = new EnumMap<>(ClientEffectType.class);
    private static final Random RANDOM = new Random();
    private static Float storedHostileVolume;
    // Giro de desorientación (concussive_blow_3): yaw capturado al recibir el payload
    private static Float disorientStartYaw;
    // Sordera (deafening_strike): volumen maestro original del usuario mientras dura el efecto
    private static Float deafeningStoredMasterVolume;

    private ClientEffectHandler() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ClientEffectPayload.ID, (payload, context) ->
            context.client().execute(() -> handlePayload(payload))
        );
        ClientTickEvents.END_CLIENT_TICK.register(ClientEffectHandler::tickClient); 
    }

    private static void handlePayload(ClientEffectPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (payload.start()) {
            if (payload.effect() == ClientEffectType.DISORIENT_SPIN) {
                LocalPlayer player = client.player;
                disorientStartYaw = player != null ? player.getYRot() : 0.0F;
            }
            ACTIVE.put(payload.effect(), new ActiveEffect(payload.durationTicks(), payload.intensity()));
            if (payload.effect().modifiesHostileVolume()) {
                updateHostileVolume();
            }
        } else {
            if (payload.effect() == ClientEffectType.DISORIENT_SPIN) {
                finalizeDisorientSpin(client);
            }
            ACTIVE.remove(payload.effect());
            if (payload.effect().modifiesHostileVolume()) {
                updateHostileVolume();
            }
        }
    }

    private static void tickClient(Minecraft client) {
        updateDeafeningVolumes(client);
        if (ACTIVE.isEmpty()) {
            return;
        }
        ACTIVE.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().tickAndExpired();
            if (expired && entry.getKey() == ClientEffectType.DISORIENT_SPIN) {
                // El último escalón del easing se completa fijando exactamente +180°
                finalizeDisorientSpin(client);
            }
            return expired;
        });
        if (ACTIVE.isEmpty()) {
            restoreHostileVolume();
            return;
        }
        ActiveEffect phantom = ACTIVE.get(ClientEffectType.PHANTOM_SOUNDS);
        if (phantom != null) {
            maybePlayPhantomSound(client, phantom.intensity);
        }
        if (!ACTIVE.containsKey(ClientEffectType.HOSTILE_VOLUME_SCALE) && storedHostileVolume != null) {
            restoreHostileVolume();
        }
    }

    /**
     * Progreso del giro 0..1 (para el easing del mixin de cliente).
     */
    public static float getDisorientSpinProgress() {
        ActiveEffect effect = ACTIVE.get(ClientEffectType.DISORIENT_SPIN);
        if (effect == null || effect.totalDurationTicks <= 0) {
            return 0.0F;
        }
        int elapsed = effect.totalDurationTicks - Math.max(0, effect.ticksRemaining);
        return Mth.clamp((float) elapsed / effect.totalDurationTicks, 0.0F, 1.0F);
    }

    public static float getDisorientStartYaw() {
        return disorientStartYaw != null ? disorientStartYaw : 0.0F;
    }

    private static void finalizeDisorientSpin(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && disorientStartYaw != null) {
            float finalYaw = Mth.wrapDegrees(disorientStartYaw + 180.0F);
            player.setYRot(finalYaw);
            player.setYHeadRot(finalYaw);
        }
        disorientStartYaw = null;
    }

    public static void clearDisorientSpin() {
        disorientStartYaw = null;
        ACTIVE.remove(ClientEffectType.DISORIENT_SPIN);
    }

    /**
     * Sordera por deafening_strike: en vez de tocar fuente por fuente, escala el
     * VOLUMEN MAESTRO (afecta a todas las fuentes de golpe). Oídos Zumbando = 50%,
     * Silencio Absoluto = 0%. Al no haber ninguno de los dos efectos activos, se
     * restaura exactamente el volumen que el usuario tenía guardado.
     */
    private static void updateDeafeningVolumes(Minecraft client) {
        LocalPlayer player = client.player;
        Float scale = null;
        if (player != null) {
            if (player.hasEffect(holderOf(ModStatusEffects.ABSOLUTE_SILENCE))) {
                scale = 0.0F;
            } else if (player.hasEffect(holderOf(ModStatusEffects.RINGING_EARS))) {
                scale = 0.5F;
            }
        }
        if (scale == null) {
            restoreDeafeningMasterVolume(client);
            return;
        }
        if (deafeningStoredMasterVolume == null) {
            deafeningStoredMasterVolume = (float) client.options.getSoundSourceVolume(SoundSource.MASTER);
        }
        client.options.getSoundSourceOptionInstance(SoundSource.MASTER)
            .set((double) (deafeningStoredMasterVolume * scale));
    }

    private static void restoreDeafeningMasterVolume(Minecraft client) {
        if (deafeningStoredMasterVolume == null) {
            return;
        }
        client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set((double) deafeningStoredMasterVolume);
        deafeningStoredMasterVolume = null;
    }

    private static Holder<MobEffect> holderOf(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    private static void maybePlayPhantomSound(Minecraft client, float intensity) {
        LocalPlayer player = client.player;
        if (player == null || player.tickCount % Math.max(10, (int) (20 / Mth.clamp(intensity, 0.2F, 1.0F))) != 0) {
            return;
        }
        double radius = 6.0D;
        double x = player.getX() + (RANDOM.nextDouble() - 0.5D) * radius;
        double y = player.getY() + RANDOM.nextDouble() * 2.0D;
        double z = player.getZ() + (RANDOM.nextDouble() - 0.5D) * radius;
        var sound = RANDOM.nextBoolean() ? net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT : net.minecraft.sounds.SoundEvents.SKELETON_AMBIENT;
        if (client.level != null && client.player != null) {
            client.level.playSound(client.player, x, y, z, sound, SoundSource.HOSTILE, 0.5F, 0.8F + RANDOM.nextFloat() * 0.4F);
        }
    }

    public static boolean isActive(ClientEffectType type) {
        return ACTIVE.containsKey(type);
    }

    public static float getIntensity(ClientEffectType type) {
        ActiveEffect effect = ACTIVE.get(type);
        return effect == null ? 0.0F : effect.intensity;
    }

    private static void updateHostileVolume() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        float scale = 1.0F;
        ActiveEffect silence = ACTIVE.get(ClientEffectType.HOSTILE_VOLUME_SCALE);
        if (silence != null) {
            scale = Mth.clamp(silence.intensity, 0.0F, 1.0F);
        }
        if (Mth.equal(scale, 1.0F)) {
            restoreHostileVolume();
            return;
        }
        if (storedHostileVolume == null) {
            storedHostileVolume = (float) client.options.getSoundSourceVolume(SoundSource.HOSTILE);
        }
        client.options.getSoundSourceOptionInstance(SoundSource.HOSTILE)
            .set((double) Mth.clamp(storedHostileVolume * scale, 0.0F, 1.0F));
    }

    private static void restoreHostileVolume() {
        Minecraft client = Minecraft.getInstance();
        if (storedHostileVolume == null || client.player == null) {
            return;
        }
    client.options.getSoundSourceOptionInstance(SoundSource.HOSTILE).set((double) storedHostileVolume);
        storedHostileVolume = null;
    }

    private static final class ActiveEffect {
        private int ticksRemaining;
        private final int totalDurationTicks;
        private final float intensity;

        private ActiveEffect(int duration, float intensity) {
            this.ticksRemaining = duration;
            this.totalDurationTicks = Math.max(1, duration);
            this.intensity = intensity;
        }

        private boolean tickAndExpired() {
            if (ticksRemaining > 0) {
                ticksRemaining--;
            }
            return ticksRemaining <= 0;
        }
    }
}
