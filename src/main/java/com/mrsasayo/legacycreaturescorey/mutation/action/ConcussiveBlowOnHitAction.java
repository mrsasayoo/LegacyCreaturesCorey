package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

/**
 * Applies camera distortion or status-based disorientation effects to the target.
 */
public final class ConcussiveBlowOnHitAction extends ProcOnHitAction {
    private final Mode mode;

    public ConcussiveBlowOnHitAction(double chance, Mode mode) {
        super(chance);
        this.mode = mode;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof ServerPlayerEntity player)) {
            return;
        }

        switch (mode) {
            case SHAKE -> applyCameraShake(player);
            case TUNNEL -> applyLocalizedDarkness(player);
            case DISORIENT -> applyDisorientation(attacker, player);
        }
    }

    private void applyCameraShake(ServerPlayerEntity player) {
        ModNetworking.sendClientEffect(player, new ClientEffectPayload(ClientEffectType.CAMERA_SHAKE, true, 25, 2.8F));

        if (player.getRandom().nextFloat() <= 0.4F) {
            float yawOffset = (player.getRandom().nextFloat() - 0.5F) * 70.0F;
            float pitchOffset = (player.getRandom().nextFloat() - 0.5F) * 24.0F;
            float newYaw = MathHelper.wrapDegrees(player.getYaw() + yawOffset);
            float newPitch = MathHelper.clamp(player.getPitch() + pitchOffset, -90.0F, 90.0F);
            player.setYaw(newYaw);
            player.setHeadYaw(newYaw);
            player.setBodyYaw(newYaw);
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), newYaw, newPitch);
        }
    }

    private void applyLocalizedDarkness(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0));
    }

    private void applyDisorientation(LivingEntity attacker, ServerPlayerEntity player) {
        float baseYaw = MathHelper.wrapDegrees(player.getYaw());
        float pitch = player.getPitch();
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        if (world == null) {
            return;
        }

        final int stepInterval = 4;
        queueRotationStep(world, player, baseYaw + 60.0F, pitch, 0);
        queueRotationStep(world, player, baseYaw + 120.0F, pitch, stepInterval);
        queueRotationStep(world, player, baseYaw + 180.0F, pitch, stepInterval * 2);

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0));
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 1));
    }

    private void queueRotationStep(ServerWorld world, ServerPlayerEntity player, float targetYaw, float targetPitch, int delayTicks) {
    MutationTaskScheduler.schedule(world, new MutationTaskScheduler.TimedTask() {
            private int ticksRemaining = delayTicks;

            @Override
            public boolean tick(ServerWorld currentWorld) {
                if (!player.isAlive() || player.getEntityWorld() != currentWorld) {
                    return true;
                }

                if (ticksRemaining > 0) {
                    ticksRemaining--;
                    return false;
                }

                float wrappedYaw = MathHelper.wrapDegrees(targetYaw);
                float clampedPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);
                player.setYaw(wrappedYaw);
                player.setHeadYaw(wrappedYaw);
                player.setBodyYaw(wrappedYaw);
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), wrappedYaw, clampedPitch);
                return true;
            }
        });
    }

    public enum Mode {
        SHAKE,
        TUNNEL,
        DISORIENT;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "SHAKE", "I" -> SHAKE;
                case "TUNNEL", "II" -> TUNNEL;
                case "DISORIENT", "III" -> DISORIENT;
                default -> throw new IllegalArgumentException("Modo de golpe conmocionador desconocido: " + raw);
            };
        }
    }
}
