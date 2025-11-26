package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

abstract class bastion_guard_base_action implements MutationAction {
    private static final Identifier STANCE_SPEED_ID = Identifier.of(Legacycreaturescorey.MOD_ID,
            "bastion_guard_speed_lock");
    private static final Identifier STANCE_KNOCKBACK_ID = Identifier.of(Legacycreaturescorey.MOD_ID,
            "bastion_guard_knockback");

    protected PiglinBruteEntity asServerBrute(LivingEntity entity) {
        if (entity instanceof PiglinBruteEntity piglin && !piglin.getEntityWorld().isClient()) {
            return piglin;
        }
        return null;
    }

    protected Handler handler() {
        return Handler.INSTANCE;
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof PiglinBruteEntity piglin) {
            handler().clear(piglin);
        }
    }

    protected static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private final Map<PiglinBruteEntity, Integer> pommelCooldowns = new WeakHashMap<>();
        private final Map<PiglinBruteEntity, StanceState> stanceStates = new WeakHashMap<>();

        private Handler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                tickPommelCooldowns(world);
                tickStanceStates(world);
            });
        }

        private void tickPommelCooldowns(ServerWorld world) {
            Iterator<Map.Entry<PiglinBruteEntity, Integer>> iterator = pommelCooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PiglinBruteEntity, Integer> entry = iterator.next();
                PiglinBruteEntity piglin = entry.getKey();
                if (piglin == null || piglin.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                if (piglin.getEntityWorld() != world) {
                    continue;
                }
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    iterator.remove();
                } else {
                    entry.setValue(remaining);
                }
            }
        }

        private void tickStanceStates(ServerWorld world) {
            Iterator<Map.Entry<PiglinBruteEntity, StanceState>> iterator = stanceStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PiglinBruteEntity, StanceState> entry = iterator.next();
                PiglinBruteEntity piglin = entry.getKey();
                StanceState state = entry.getValue();
                if (piglin == null || piglin.isRemoved()) {
                    clearStanceModifiers(piglin);
                    iterator.remove();
                    continue;
                }
                if (piglin.getEntityWorld() != world) {
                    continue;
                }
                if (state.remainingTicks > 0) {
                    state.remainingTicks--;
                    if (state.remainingTicks <= 0) {
                        clearStanceModifiers(piglin);
                        state.currentCooldown = Math.max(state.currentCooldown, state.cooldownDuration);
                    }
                } else if (state.currentCooldown > 0) {
                    state.currentCooldown--;
                }
            }
        }

        void tickDefensiveStance(PiglinBruteEntity piglin,
                int durationTicks,
                int cooldownTicks,
                float activationThreshold,
                double damageReduction,
                double forwardDotThreshold) {
            StanceState state = stanceStates.computeIfAbsent(piglin, ignored -> new StanceState());
            if (state.remainingTicks > 0) {
                applyStanceEffects(piglin);
                return;
            }
            if (state.currentCooldown > 0) {
                return;
            }
            if (piglin.getHealth() / piglin.getMaxHealth() <= activationThreshold) {
                state.remainingTicks = durationTicks;
                state.cooldownDuration = cooldownTicks;
                state.currentCooldown = 0;
                state.damageReduction = damageReduction;
                state.forwardDotThreshold = forwardDotThreshold;
                applyStanceModifiers(piglin);
                applyStanceEffects(piglin);
            }
        }

        void handleDefensiveDamage(PiglinBruteEntity piglin, DamageSource source, float amount) {
            StanceState state = stanceStates.get(piglin);
            if (state == null || state.remainingTicks <= 0) {
                return;
            }
            if (!(source.getAttacker() instanceof LivingEntity attacker)) {
                return;
            }
            Vec3d forward = piglin.getRotationVector();
            Vec3d direction = new Vec3d(attacker.getX() - piglin.getX(),
                    attacker.getEyeY() - piglin.getEyeY(),
                    attacker.getZ() - piglin.getZ());
            if (direction.lengthSquared() < 1.0E-4D) {
                return;
            }
            direction = direction.normalize();
            if (forward.dotProduct(direction) >= state.forwardDotThreshold) {
                piglin.heal(amount * (float) state.damageReduction);
            }
        }

        boolean tryPommelStrike(PiglinBruteEntity piglin,
                LivingEntity attacker,
                double chance,
                int cooldownTicks,
                double knockbackStrength,
                double verticalBoost) {
            if (attacker == null) {
                return false;
            }
            if (pommelCooldowns.getOrDefault(piglin, 0) > 0) {
                return false;
            }
            if (piglin.getRandom().nextDouble() > chance) {
                return false;
            }
            Vec3d direction = new Vec3d(attacker.getX() - piglin.getX(), 0.0D, attacker.getZ() - piglin.getZ());
            double horizontalLength = direction.length();
            if (horizontalLength < 1.0E-4D) {
                return false;
            }
            direction = direction.normalize();
            attacker.addVelocity(direction.x * knockbackStrength, verticalBoost, direction.z * knockbackStrength);
            attacker.velocityModified = true;
            if (cooldownTicks > 0) {
                pommelCooldowns.put(piglin, cooldownTicks);
            }
            return true;
        }

        void clear(PiglinBruteEntity piglin) {
            pommelCooldowns.remove(piglin);
            StanceState state = stanceStates.remove(piglin);
            if (state != null) {
                clearStanceModifiers(piglin);
            }
        }

        private void applyStanceEffects(PiglinBruteEntity piglin) {
            piglin.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 1, false, false, true));
            piglin.setAttacking(false);
            piglin.getNavigation().stop();
            applyStanceModifiers(piglin);
        }

        private static void applyStanceModifiers(PiglinBruteEntity piglin) {
            EntityAttributeInstance speed = piglin.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(STANCE_SPEED_ID)) {
                speed.addPersistentModifier(new EntityAttributeModifier(STANCE_SPEED_ID,
                        -1.0D,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            EntityAttributeInstance knockback = piglin.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
            if (knockback != null && !knockback.hasModifier(STANCE_KNOCKBACK_ID)) {
                knockback.addPersistentModifier(new EntityAttributeModifier(STANCE_KNOCKBACK_ID,
                        1.0D,
                        EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }

        private static void clearStanceModifiers(PiglinBruteEntity piglin) {
            if (piglin == null) {
                return;
            }
            EntityAttributeInstance speed = piglin.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            if (speed != null && speed.hasModifier(STANCE_SPEED_ID)) {
                speed.removeModifier(STANCE_SPEED_ID);
            }
            EntityAttributeInstance knockback = piglin.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
            if (knockback != null && knockback.hasModifier(STANCE_KNOCKBACK_ID)) {
                knockback.removeModifier(STANCE_KNOCKBACK_ID);
            }
        }

        private static final class StanceState {
            private int remainingTicks;
            private int currentCooldown;
            private int cooldownDuration;
            private double damageReduction;
            private double forwardDotThreshold;
        }
    }
}
