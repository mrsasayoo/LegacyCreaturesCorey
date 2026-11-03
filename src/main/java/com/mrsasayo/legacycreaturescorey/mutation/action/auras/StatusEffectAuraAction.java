package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.core.registries.BuiltInRegistries;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Applies a status effect periodically to entities within an aura radius.
 */
public final class StatusEffectAuraAction implements mutation_action {
    private final Holder<MobEffect> effect;
    private final int duration;
    private final int amplifier;
    private final double radius;
    private final int interval;
    private final Target target;
    private final boolean excludeSelf;
    private final boolean requireUndead;
    private final boolean requireNonUndead;

    public StatusEffectAuraAction(Holder<MobEffect> effect,
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
        if (requireUndead && !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(EntityTypeTags.UNDEAD)) {
            return;
        }
        if (requireNonUndead && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(EntityTypeTags.UNDEAD)) {
            return;
        }
        if (entity.tickCount % interval != 0) {
            return;
        }

        ServerLevel world = (ServerLevel) entity.level();
        switch (target) {
            case SELF -> applyEffect(entity);
            case PLAYERS -> {
                for (Player player : world.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
                    if (skipCandidate(entity, player)) {
                        continue;
                    }
                    applyEffect(player);
                }
            }
            case ALLY_MOBS -> {
                if (!(entity instanceof Mob mob)) {
                    return;
                }
                List<Mob> mobs = world.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius), other -> other.isAlive());
                for (Mob other : mobs) {
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
        return source.distanceToSqr(candidate) > radius * radius;
    }

    private void applyEffect(LivingEntity targetEntity) {
        targetEntity.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true, true));
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
