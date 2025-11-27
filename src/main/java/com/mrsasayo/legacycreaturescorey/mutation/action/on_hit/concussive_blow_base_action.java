package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import com.mrsasayo.legacycreaturescorey.core.network.ClientFeature;
import com.mrsasayo.legacycreaturescorey.core.network.ModNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

import java.util.List;

abstract class concussive_blow_base_action extends proc_on_hit_action {
    private final Mode mode;
    private final int rotationStepTicks;
    private final int shakeDurationTicks;
    private final float shakeIntensity;
    private final List<status_effect_config_parser.status_effect_config_entry> playerEffects;
    private final List<status_effect_config_parser.status_effect_config_entry> attackerEffects;

    protected concussive_blow_base_action(mutation_action_config config,
            double defaultChance,
            Mode defaultMode) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.mode = parseMode(config, defaultMode);
        this.rotationStepTicks = Math.max(1, config.getInt("rotation_step_ticks", 4));
        this.shakeDurationTicks = Math.max(1, config.getInt("shake_duration_ticks", 10));
        this.shakeIntensity = MathHelper.clamp((float) config.getDouble("shake_intensity", 2.6D), 0.2F, 6.0F);
        this.playerEffects = status_effect_config_parser.parseList(config, "player_status_effects", defaultPlayerEffects(this.mode));
        this.attackerEffects = status_effect_config_parser.parseList(config, "attacker_status_effects", defaultAttackerEffects(this.mode));
    }

    private static Mode parseMode(mutation_action_config config, Mode fallback) {
        String raw = config.getString("mode", fallback.name());
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Mode.fromString(raw);
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
        boolean delivered = false;
        if (ModNetworking.supportsFeature(player, ClientFeature.CLIENT_EFFECTS)) {
            ModNetworking.sendClientEffect(player,
                    new ClientEffectPayload(ClientEffectType.CAMERA_SHAKE, true, shakeDurationTicks, shakeIntensity));
            delivered = true;
        }
        if (!delivered) {
            scheduleServerShake(player);
        }

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

    private void scheduleServerShake(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
            private int ticksRemaining = Math.min(shakeDurationTicks, 40);

            @Override
            public boolean tick(ServerWorld currentWorld) {
                if (!player.isAlive() || player.getEntityWorld() != currentWorld) {
                    return true;
                }
                float yawOffset = (player.getRandom().nextFloat() - 0.5F) * shakeIntensity;
                float pitchOffset = (player.getRandom().nextFloat() - 0.5F) * shakeIntensity * 0.6F;
                float newYaw = MathHelper.wrapDegrees(player.getYaw() + yawOffset);
                float newPitch = MathHelper.clamp(player.getPitch() + pitchOffset, -90.0F, 90.0F);
                player.setYaw(newYaw);
                player.setHeadYaw(newYaw);
                player.setBodyYaw(newYaw);
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), newYaw, newPitch);
                return --ticksRemaining <= 0;
            }
        });
    }

    private void applyLocalizedDarkness(ServerPlayerEntity player) {
        applyConfiguredEffects(player, playerEffects);
    }

    private void applyDisorientation(LivingEntity attacker, ServerPlayerEntity player) {
        float baseYaw = MathHelper.wrapDegrees(player.getYaw());
        float pitch = player.getPitch();
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        if (world == null) {
            return;
        }

        queueRotationStep(world, player, baseYaw + 60.0F, pitch, 0);
        queueRotationStep(world, player, baseYaw + 120.0F, pitch, rotationStepTicks);
        queueRotationStep(world, player, baseYaw + 180.0F, pitch, rotationStepTicks * 2);

        applyConfiguredEffects(player, playerEffects);
        applyConfiguredEffects(attacker, attackerEffects);
    }

    protected void afterProc(LivingEntity attacker, LivingEntity victim) {
        // Gancho opcional para subclases.
    }

    private void applyConfiguredEffects(LivingEntity target,
            List<status_effect_config_parser.status_effect_config_entry> configs) {
        if (target == null || configs == null || configs.isEmpty()) {
            return;
        }
        for (status_effect_config_parser.status_effect_config_entry entry : configs) {
            StatusEffectInstance instance = status_effect_config_parser.buildInstance(entry);
            if (instance != null) {
                target.addStatusEffect(instance);
            }
        }
    }

    private static List<status_effect_config_parser.status_effect_config_entry> defaultPlayerEffects(Mode mode) {
        return switch (mode) {
            case TUNNEL -> List.of(entry(StatusEffects.DARKNESS, 60, 0));
            case DISORIENT -> List.of(entry(StatusEffects.NAUSEA, 80, 0));
            default -> List.of();
        };
    }

    private static List<status_effect_config_parser.status_effect_config_entry> defaultAttackerEffects(Mode mode) {
        return List.of();
    }

    private static status_effect_config_parser.status_effect_config_entry entry(RegistryEntry<StatusEffect> effect,
            int duration,
            int amplifier) {
        return new status_effect_config_parser.status_effect_config_entry(effect, duration, amplifier, true, true, true);
    }

    private void queueRotationStep(ServerWorld world, ServerPlayerEntity player, float targetYaw, float targetPitch, int delayTicks) {
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
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

    enum Mode {
        SHAKE,
        TUNNEL,
        DISORIENT;

        static Mode fromString(String raw) {
            String normalized = raw.trim().toUpperCase();
            return switch (normalized) {
                case "SHAKE", "I" -> SHAKE;
                case "TUNNEL", "II" -> TUNNEL;
                case "DISORIENT", "III" -> DISORIENT;
                default -> throw new IllegalArgumentException("Modo de golpe conmocionador desconocido: " + raw);
            };
        }
    }
}
