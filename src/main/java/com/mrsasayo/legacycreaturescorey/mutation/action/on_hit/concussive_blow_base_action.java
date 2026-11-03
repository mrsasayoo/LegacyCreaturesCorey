package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import com.mrsasayo.legacycreaturescorey.core.network.ClientFeature;
import com.mrsasayo.legacycreaturescorey.core.network.ModNetworking;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

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
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.mode = parseMode(config, defaultMode);
        this.rotationStepTicks = Math.max(1, config.getInt("rotation_step_ticks", 4));
        this.shakeDurationTicks = Math.max(1, config.getInt("shake_duration_ticks", 10));
        this.shakeIntensity = Mth.clamp((float) config.getDouble("shake_intensity", 2.6D), 0.2F, 6.0F);
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
        if (!(victim instanceof ServerPlayer player)) {
            return;
        }

        switch (mode) {
            case SHAKE -> applyCameraShake(player);
            case TUNNEL -> applyLocalizedDarkness(player);
            case DISORIENT -> applyDisorientation(attacker, player);
        }
    }

    private void applyCameraShake(ServerPlayer player) {
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
            float newYaw = Mth.wrapDegrees(player.getYRot() + yawOffset);
            float newPitch = Mth.clamp(player.getXRot() + pitchOffset, -90.0F, 90.0F);
            player.setYRot(newYaw);
            player.setYHeadRot(newYaw);
            player.setYBodyRot(newYaw);
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), newYaw, newPitch);
        }
    }

    private void scheduleServerShake(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
            private int ticksRemaining = Math.min(shakeDurationTicks, 40);

            @Override
            public boolean tick(ServerLevel currentWorld) {
                if (!player.isAlive() || player.level() != currentWorld) {
                    return true;
                }
                float yawOffset = (player.getRandom().nextFloat() - 0.5F) * shakeIntensity;
                float pitchOffset = (player.getRandom().nextFloat() - 0.5F) * shakeIntensity * 0.6F;
                float newYaw = Mth.wrapDegrees(player.getYRot() + yawOffset);
                float newPitch = Mth.clamp(player.getXRot() + pitchOffset, -90.0F, 90.0F);
                player.setYRot(newYaw);
                player.setYHeadRot(newYaw);
                player.setYBodyRot(newYaw);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), newYaw, newPitch);
                return --ticksRemaining <= 0;
            }
        });
    }

    private void applyLocalizedDarkness(ServerPlayer player) {
        applyConfiguredEffects(player, playerEffects);
    }

    private void applyDisorientation(LivingEntity attacker, ServerPlayer player) {
        float baseYaw = Mth.wrapDegrees(player.getYRot());
        float pitch = player.getXRot();
        ServerLevel world = (ServerLevel) player.level();
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
            MobEffectInstance instance = status_effect_config_parser.buildInstance(entry);
            if (instance != null) {
                target.addEffect(instance);
            }
        }
    }

    private static List<status_effect_config_parser.status_effect_config_entry> defaultPlayerEffects(Mode mode) {
        return switch (mode) {
            case TUNNEL -> List.of(entry(MobEffects.DARKNESS, 60, 0));
            case DISORIENT -> List.of(entry(MobEffects.NAUSEA, 80, 0));
            default -> List.of();
        };
    }

    private static List<status_effect_config_parser.status_effect_config_entry> defaultAttackerEffects(Mode mode) {
        return List.of();
    }

    private static status_effect_config_parser.status_effect_config_entry entry(Holder<MobEffect> effect,
            int duration,
            int amplifier) {
        return new status_effect_config_parser.status_effect_config_entry(effect, duration, amplifier, true, true, true);
    }

    private void queueRotationStep(ServerLevel world, ServerPlayer player, float targetYaw, float targetPitch, int delayTicks) {
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
            private int ticksRemaining = delayTicks;

            @Override
            public boolean tick(ServerLevel currentWorld) {
                if (!player.isAlive() || player.level() != currentWorld) {
                    return true;
                }

                if (ticksRemaining > 0) {
                    ticksRemaining--;
                    return false;
                }

                float wrappedYaw = Mth.wrapDegrees(targetYaw);
                float clampedPitch = Mth.clamp(targetPitch, -90.0F, 90.0F);
                player.setYRot(wrappedYaw);
                player.setYHeadRot(wrappedYaw);
                player.setYBodyRot(wrappedYaw);
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), wrappedYaw, clampedPitch);
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
