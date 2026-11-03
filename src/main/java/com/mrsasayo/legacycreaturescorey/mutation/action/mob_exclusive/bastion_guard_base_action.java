package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

abstract class bastion_guard_base_action implements mutation_action {
    private static final Identifier STANCE_SPEED_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID,
            "bastion_guard_speed_lock");
    private static final Identifier STANCE_KNOCKBACK_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID,
            "bastion_guard_knockback");

    protected PiglinBrute asServerBrute(LivingEntity entity) {
        if (entity instanceof PiglinBrute piglin && !piglin.level().isClientSide()) {
            return piglin;
        }
        return null;
    }

    protected Handler handler() {
        return Handler.INSTANCE;
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof PiglinBrute piglin) {
            handler().clear(piglin);
        }
    }

    protected static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private final Map<PiglinBrute, Integer> pommelCooldowns = new WeakHashMap<>();
        private final Map<PiglinBrute, StanceState> stanceStates = new WeakHashMap<>();

        private Handler() {
            ServerTickEvents.END_LEVEL_TICK.register(world -> {
                tickPommelCooldowns(world);
                tickStanceStates(world);
            });
        }

        private void tickPommelCooldowns(ServerLevel world) {
            Iterator<Map.Entry<PiglinBrute, Integer>> iterator = pommelCooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PiglinBrute, Integer> entry = iterator.next();
                PiglinBrute piglin = entry.getKey();
                if (piglin == null || piglin.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                if (piglin.level() != world) {
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

        private void tickStanceStates(ServerLevel world) {
            Iterator<Map.Entry<PiglinBrute, StanceState>> iterator = stanceStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PiglinBrute, StanceState> entry = iterator.next();
                PiglinBrute piglin = entry.getKey();
                StanceState state = entry.getValue();
                if (piglin == null || piglin.isRemoved()) {
                    clearStanceModifiers(piglin);
                    iterator.remove();
                    continue;
                }
                if (piglin.level() != world) {
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

        void tickDefensiveStance(PiglinBrute piglin,
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

        void handleDefensiveDamage(PiglinBrute piglin, DamageSource source, float amount) {
            StanceState state = stanceStates.get(piglin);
            if (state == null || state.remainingTicks <= 0) {
                return;
            }
            if (!(source.getEntity() instanceof LivingEntity attacker)) {
                return;
            }
            Vec3 forward = piglin.getViewVector(1.0F);
            Vec3 direction = new Vec3(attacker.getX() - piglin.getX(),
                    attacker.getEyeY() - piglin.getEyeY(),
                    attacker.getZ() - piglin.getZ());
            if (direction.lengthSqr() < 1.0E-4D) {
                return;
            }
            direction = direction.normalize();
            if (forward.dot(direction) >= state.forwardDotThreshold) {
                piglin.heal(amount * (float) state.damageReduction);
            }
        }

        boolean tryPommelStrike(PiglinBrute piglin,
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
            Vec3 direction = new Vec3(attacker.getX() - piglin.getX(), 0.0D, attacker.getZ() - piglin.getZ());
            double horizontalLength = direction.length();
            if (horizontalLength < 1.0E-4D) {
                return false;
            }
            direction = direction.normalize();
            attacker.push(direction.x * knockbackStrength, verticalBoost, direction.z * knockbackStrength);
            attacker.syncVelocity = true;
            if (cooldownTicks > 0) {
                pommelCooldowns.put(piglin, cooldownTicks);
            }
            return true;
        }

        void clear(PiglinBrute piglin) {
            pommelCooldowns.remove(piglin);
            StanceState state = stanceStates.remove(piglin);
            if (state != null) {
                clearStanceModifiers(piglin);
            }
        }

        private void applyStanceEffects(PiglinBrute piglin) {
            piglin.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 10, 1, false, false, true));
            piglin.setTarget(null);
            piglin.getNavigation().stop();
            applyStanceModifiers(piglin);
        }

        private static void applyStanceModifiers(PiglinBrute piglin) {
            AttributeInstance speed = piglin.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(STANCE_SPEED_ID)) {
                speed.addPermanentModifier(new AttributeModifier(STANCE_SPEED_ID,
                        -1.0D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            AttributeInstance knockback = piglin.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null && !knockback.hasModifier(STANCE_KNOCKBACK_ID)) {
                knockback.addPermanentModifier(new AttributeModifier(STANCE_KNOCKBACK_ID,
                        1.0D,
                        AttributeModifier.Operation.ADD_VALUE));
            }
        }

        private static void clearStanceModifiers(PiglinBrute piglin) {
            if (piglin == null) {
                return;
            }
            AttributeInstance speed = piglin.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.hasModifier(STANCE_SPEED_ID)) {
                speed.removeModifier(STANCE_SPEED_ID);
            }
            AttributeInstance knockback = piglin.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
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
