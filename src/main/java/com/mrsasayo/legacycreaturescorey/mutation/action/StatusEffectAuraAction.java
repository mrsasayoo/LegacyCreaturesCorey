package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Applies a status effect periodically to entities within an aura radius.
 */
public final class StatusEffectAuraAction implements MutationAction {
    private final RegistryEntry<StatusEffect> effect;
    private final int duration;
    private final int amplifier;
    private final double radius;
    private final int interval;
    private final Target target;
    private final boolean excludeSelf;

    public StatusEffectAuraAction(Identifier effectId,
                                  int duration,
                                  int amplifier,
                                  double radius,
                                  int interval,
                                  Target target,
                                  boolean excludeSelf) {
        StatusEffect resolved = Registries.STATUS_EFFECT.get(effectId);
        this.effect = resolved != null
            ? Registries.STATUS_EFFECT.getEntry(Registries.STATUS_EFFECT.getRawId(resolved)).orElse(null)
            : null;
        this.duration = Math.max(1, duration);
        this.amplifier = Math.max(0, amplifier);
        this.radius = Math.max(0.5D, radius);
        this.interval = Math.max(1, interval);
        this.target = target;
        this.excludeSelf = excludeSelf;
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (effect == null || !ActionContext.isServer(entity)) {
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
