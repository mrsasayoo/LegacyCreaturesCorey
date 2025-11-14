package com.mrsasayo.legacycreaturescorey.client;

import com.mrsasayo.legacycreaturescorey.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

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

    private ClientEffectHandler() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ClientEffectPayload.ID, (payload, context) ->
            context.client().execute(() -> handlePayload(payload))
        );
        ClientTickEvents.END_CLIENT_TICK.register(ClientEffectHandler::tickClient); 
    }

    private static void handlePayload(ClientEffectPayload payload) {
        if (payload.start()) {
            ACTIVE.put(payload.effect(), new ActiveEffect(payload.durationTicks(), payload.intensity()));
            if (payload.effect().modifiesHostileVolume()) {
                updateHostileVolume();
            }
        } else {
            ACTIVE.remove(payload.effect());
            if (payload.effect().modifiesHostileVolume()) {
                updateHostileVolume();
            }
        }
    }

    private static void tickClient(MinecraftClient client) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().tickAndExpired());
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

    private static void maybePlayPhantomSound(MinecraftClient client, float intensity) {
        ClientPlayerEntity player = client.player;
        if (player == null || player.age % Math.max(10, (int) (20 / MathHelper.clamp(intensity, 0.2F, 1.0F))) != 0) {
            return;
        }
        double radius = 6.0D;
        double x = player.getX() + (RANDOM.nextDouble() - 0.5D) * radius;
        double y = player.getY() + RANDOM.nextDouble() * 2.0D;
        double z = player.getZ() + (RANDOM.nextDouble() - 0.5D) * radius;
        var sound = RANDOM.nextBoolean() ? net.minecraft.sound.SoundEvents.ENTITY_ZOMBIE_AMBIENT : net.minecraft.sound.SoundEvents.ENTITY_SKELETON_AMBIENT;
        if (client.world != null && client.player != null) {
            client.world.playSound(client.player, x, y, z, sound, SoundCategory.HOSTILE, 0.5F, 0.8F + RANDOM.nextFloat() * 0.4F, RANDOM.nextLong());
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        float scale = 1.0F;
        ActiveEffect silence = ACTIVE.get(ClientEffectType.HOSTILE_VOLUME_SCALE);
        if (silence != null) {
            scale = MathHelper.clamp(silence.intensity, 0.0F, 1.0F);
        }
        if (MathHelper.approximatelyEquals(scale, 1.0F)) {
            restoreHostileVolume();
            return;
        }
        if (storedHostileVolume == null) {
            storedHostileVolume = (float) client.options.getSoundVolume(SoundCategory.HOSTILE);
        }
        client.options.getSoundVolumeOption(SoundCategory.HOSTILE)
            .setValue((double) MathHelper.clamp(storedHostileVolume * scale, 0.0F, 1.0F));
    }

    private static void restoreHostileVolume() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (storedHostileVolume == null || client.player == null) {
            return;
        }
    client.options.getSoundVolumeOption(SoundCategory.HOSTILE).setValue((double) storedHostileVolume);
        storedHostileVolume = null;
    }

    private static final class ActiveEffect {
        private int ticksRemaining;
        private final float intensity;

        private ActiveEffect(int duration, float intensity) {
            this.ticksRemaining = duration;
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
