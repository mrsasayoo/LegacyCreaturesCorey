package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Applies a status effect periodically to entities within an aura radius.
 */
public final class StatusEffectAuraAction implements mutation_action {
    private final RegistryEntry<StatusEffect> effect;
    private final int duration;
    private final int amplifier;
    private final double radius;
    private final int interval;
    private final Target target;
    private final boolean excludeSelf;
    private final boolean requireUndead;
    private final boolean requireNonUndead;

    public StatusEffectAuraAction(RegistryEntry<StatusEffect> effect,
                                  int duration,
                                  int amplifier,
                                  double radius,
                                  int interval,
                                  Target target,
                                  boolean excludeSelf,
                                  boolean requireUndead,
                                  boolean requireNonUndead) {
        this.effect = effect;
        this.duration = Math.max(1, duration);
        this.amplifier = Math.max(0, amplifier);
        this.radius = Math.max(0.5D, radius);
        this.interval = Math.max(1, interval);
        this.target = target;
        this.excludeSelf = excludeSelf;
        this.requireUndead = requireUndead;
        this.requireNonUndead = requireNonUndead;
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (effect == null || !action_context.isServer(entity)) {
            return;
        }
        if (requireUndead && !entity.getType().isIn(EntityTypeTags.UNDEAD)) {
            return;
        }
        if (requireNonUndead && entity.getType().isIn(EntityTypeTags.UNDEAD)) {
            return;
        }
        if (entity.age % interval != 0) {
            return;
        }

        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        switch (target) {
            case SELF -> applyEffect(entity);
            case PLAYERS -> {
                for (PlayerEntity player : world.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
                    if (skipCandidate(entity, player)) {
                        continue;
                    }
                    applyEffect(player);
                }
            }
            case ALLY_MOBS -> {
                if (!(entity instanceof MobEntity mob)) {
                    return;
                }
                List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, mob.getBoundingBox().expand(radius), other -> other.isAlive());
                for (MobEntity other : mobs) {
                    if (skipCandidate(entity, other)) {
                        continue;
                    }
                    applyEffect(other);
                }
            }
        }
    }

    private boolean skipCandidate(LivingEntity source, LivingEntity candidate) {
        if (excludeSelf && candidate == source) {
            return true;
        }
        return source.squaredDistanceTo(candidate) > radius * radius;
    }

    private void applyEffect(LivingEntity targetEntity) {
        targetEntity.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, true, true, true));
    }

    public enum Target {
        SELF,
        PLAYERS,
        ALLY_MOBS;

        public static Target fromString(String raw) {
            if (raw == null) {
                return PLAYERS;
            }
            return switch (raw.trim().toUpperCase()) {
                case "SELF" -> SELF;
                case "ALLY_MOBS", "ALLIES", "MOBS" -> ALLY_MOBS;
                default -> PLAYERS;
            };
        }
    }
}
