package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.attack_downtime_manager;
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
    private static final int DEFAULT_SPIN_DURATION_TICKS = 8; // ~0,4s: igual de rápido que antes, pero fluido
    private final Mode mode;
    private final int shakeDurationTicks;
    private final float shakeIntensity;
    private final int spinDurationTicks;
    private final int attackDowntimeTicks;
    private final List<status_effect_config_parser.status_effect_config_entry> playerEffects;
    private final List<status_effect_config_parser.status_effect_config_entry> attackerEffects;

    protected concussive_blow_base_action(mutation_action_config config,
            double defaultChance,
            Mode defaultMode) {
        super(config, defaultChance);
        this.mode = parseMode(config, defaultMode);
        this.shakeDurationTicks = Math.max(1, config.getInt("shake_duration_ticks", 10));
        this.shakeIntensity = Mth.clamp((float) config.getDouble("shake_intensity", 2.6D), 0.2F, 6.0F);
        this.spinDurationTicks = Math.max(2, config.getInt("spin_duration_ticks", DEFAULT_SPIN_DURATION_TICKS));
        this.attackDowntimeTicks = Math.max(0, config.getInt("attack_downtime_ticks", 0));
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
            // Nivel 2 = todo lo del nivel 1 (sacudida) + Visión de Túnel.
            case TUNNEL -> {
                applyCameraShake(player);
                applyConfiguredEffects(player, playerEffects);
            }
            case DISORIENT -> applyDisorientation(attacker, player);
        }
        // Bloqueo de ataque real para el mob (concussive_blow_3).
        attack_downtime_manager.apply(attacker, attackDowntimeTicks);
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

    private void applyDisorientation(LivingEntity attacker, ServerPlayer player) {
        // Giro de 180° fluido: el cliente interpola con easing durante spinDurationTicks.
        boolean delivered = false;
        if (ModNetworking.supportsFeature(player, ClientFeature.CLIENT_EFFECTS)) {
            ModNetworking.sendClientEffect(player,
                    new ClientEffectPayload(ClientEffectType.DISORIENT_SPIN, true, spinDurationTicks, 1.0F));
            delivered = true;
        }
        if (!delivered) {
            // Sin cliente compatible: giro instantáneo de autoridad del servidor.
            float newYaw = Mth.wrapDegrees(player.getYRot() + 180.0F);
            player.setYRot(newYaw);
            player.setYHeadRot(newYaw);
            player.setYBodyRot(newYaw);
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), newYaw, player.getXRot());
        }

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
            case TUNNEL -> List.of(entry(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .wrapAsHolder(com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects.TUNNEL_VISION), 60, 0));
            case DISORIENT -> List.of(entry(MobEffects.NAUSEA, 100, 0), entry(MobEffects.HUNGER, 100, 0));
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
