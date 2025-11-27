package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StatusEffectOnDeathAction implements mutation_action {
    private final RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect;
    private final int duration;
    private final int amplifier;
    private final Target target;
    private final double radius;
    private final double chance;
    private final int delayTicks;
    private final float damage;
    private final double pullStrength;

    public StatusEffectOnDeathAction(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
                                     int duration,
                                     int amplifier,
                                     Target target,
                                     double radius,
                                     double chance,
                                     int delayTicks,
                                     float damage,
                                     double pullStrength) {
        this.effect = effect;
        this.duration = Math.max(1, duration);
        this.amplifier = Math.max(0, amplifier);
        this.target = target;
        this.radius = Math.max(0.0D, radius);
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        this.delayTicks = Math.max(0, delayTicks);
        this.damage = Math.max(0.0F, damage);
        this.pullStrength = Math.max(0.0D, pullStrength);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (chance < 1.0D && entity.getRandom().nextDouble() > chance) {
            return;
        }

        Runnable pulse = () -> applyEffects(world, entity, killer);
        if (delayTicks > 0) {
            mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
                private int ticks = delayTicks;

                @Override
                public boolean tick(ServerWorld currentWorld) {
                    if (ticks-- > 0) {
                        return false;
                    }
                    pulse.run();
                    return true;
                }
            });
        } else {
            pulse.run();
        }
    }

    private void applyEffects(ServerWorld world, LivingEntity sourceEntity, @Nullable LivingEntity killer) {
        Vec3d origin = new Vec3d(sourceEntity.getX(), sourceEntity.getY(), sourceEntity.getZ());
        switch (target) {
            case KILLER -> {
                if (killer != null) {
                    applyToEntity(world, origin, killer);
                }
            }
            case PLAYERS_IN_RADIUS -> {
                double radiusSq = radius * radius;
                List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers(player -> !player.isSpectator() && player.squaredDistanceTo(origin) <= radiusSq);
                players.forEach(player -> applyToEntity(world, origin, player));
            }
            case ALL_PLAYERS -> world.getPlayers(player -> !player.isSpectator()).forEach(player -> applyToEntity(world, origin, player));
        }
    }

    private void applyToEntity(ServerWorld world, Vec3d origin, LivingEntity targetEntity) {
        if (pullStrength > 0.0D) {
            Vec3d targetPos = new Vec3d(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());
            Vec3d delta = origin.subtract(targetPos);
            Vec3d normalized = delta.normalize().multiply(pullStrength);
            targetEntity.addVelocity(normalized.x, normalized.y, normalized.z);
            targetEntity.velocityModified = true;
        }
        if (damage > 0.0F) {
            targetEntity.damage(world, world.getDamageSources().magic(), damage);
        }
        if (effect != null) {
            targetEntity.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
        }
    }

    public enum Target {
        KILLER,
        PLAYERS_IN_RADIUS,
        ALL_PLAYERS;

        public static Target fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "KILLER" -> KILLER;
                case "PLAYERS", "PLAYERS_IN_RADIUS", "PLAYERS_RADIUS" -> PLAYERS_IN_RADIUS;
                case "ALL_PLAYERS", "GLOBAL_PLAYERS" -> ALL_PLAYERS;
                default -> throw new IllegalArgumentException("Objetivo on-death desconocido: " + raw);
            };
        }
    }
}
