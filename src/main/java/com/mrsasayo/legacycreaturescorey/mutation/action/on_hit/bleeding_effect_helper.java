package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Centraliza la aplicación del sangrado para garantizar que solo exista una instancia activa por entidad.
 */
final class bleeding_effect_helper {
    private static final Map<LivingEntity, bleeding_tick_task> ACTIVE_TASKS = new WeakHashMap<>();

    private bleeding_effect_helper() {}

    static void apply(ServerWorld world,
            LivingEntity attacker,
            LivingEntity victim,
            float[] pulses,
            int intervalTicks,
            int amplifier) {
        if (world == null || victim == null || pulses == null || pulses.length == 0 || intervalTicks <= 0) {
            return;
        }

        int clampedInterval = Math.max(1, intervalTicks);
        int clampedAmplifier = Math.max(0, amplifier);
        int durationTicks = computeDuration(pulses, clampedInterval);
        StatusEffectInstance instance = new StatusEffectInstance(bleedingEntry(),
            durationTicks,
            clampedAmplifier);

        boolean shouldApplyEffect = true;
        synchronized (ACTIVE_TASKS) {
            bleeding_tick_task existing = ACTIVE_TASKS.get(victim);
            if (existing != null) {
                if (clampedAmplifier < existing.getAmplifier()) {
                    shouldApplyEffect = false;
                } else {
                    existing.restart(attacker, pulses, clampedInterval, clampedAmplifier);
                }
            } else {
                bleeding_tick_task task = new bleeding_tick_task(attacker, victim, pulses, clampedInterval,
                        clampedAmplifier);
                ACTIVE_TASKS.put(victim, task);
                mutation_task_scheduler.schedule(world, task);
            }
        }

        if (shouldApplyEffect) {
            victim.addStatusEffect(instance);
        }
    }

    private static int computeDuration(float[] pulses, int intervalTicks) {
        return Math.max(intervalTicks, pulses.length * intervalTicks + 5);
    }

    private static void removeTask(LivingEntity victim, bleeding_tick_task task) {
        synchronized (ACTIVE_TASKS) {
            ACTIVE_TASKS.remove(victim, task);
        }
    }

    private static RegistryEntry<StatusEffect> bleedingEntry() {
        return Objects.requireNonNull(Registries.STATUS_EFFECT.getEntry(ModStatusEffects.BLEEDING),
            "Missing registry entry for bleeding effect");
    }

    private static final class bleeding_tick_task implements mutation_task_scheduler.TimedTask {
        private LivingEntity attacker;
        private final LivingEntity victim;
        private float[] pulses;
        private int interval;
        private int index;
        private int ticksUntilNext;
        private int amplifier;

        private bleeding_tick_task(LivingEntity attacker,
                LivingEntity victim,
                float[] pulses,
                int interval,
                int amplifier) {
            this.attacker = attacker;
            this.victim = victim;
            this.pulses = pulses;
            this.interval = interval;
            this.amplifier = amplifier;
            this.index = 0;
            this.ticksUntilNext = interval;
        }

        int getAmplifier() {
            return amplifier;
        }

        void restart(LivingEntity attacker,
                float[] pulses,
                int interval,
                int amplifier) {
            this.attacker = attacker;
            this.pulses = pulses;
            this.interval = interval;
            this.amplifier = amplifier;
            this.index = 0;
            this.ticksUntilNext = interval;
        }

        @Override
        public boolean tick(ServerWorld currentWorld) {
            if (victim.isRemoved() || !victim.isAlive()) {
                victim.removeStatusEffect(bleedingEntry());
                removeTask(victim, this);
                return true;
            }
            if (!victim.hasStatusEffect(bleedingEntry())) {
                removeTask(victim, this);
                return true;
            }
            if (--ticksUntilNext > 0) {
                return false;
            }

            ticksUntilNext = interval;
            float amount = pulses[index++];
            if (amount > 0.0F) {
                DamageSource source = resolveSource(currentWorld, attacker);
                victim.damage(currentWorld, source, amount);
            }

            if (index >= pulses.length) {
                victim.removeStatusEffect(bleedingEntry());
                removeTask(victim, this);
                return true;
            }
            return false;
        }
    }

    private static DamageSource resolveSource(ServerWorld world, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            return world.getDamageSources().playerAttack(player);
        }
        if (attacker != null) {
            return world.getDamageSources().mobAttack(attacker);
        }
        return world.getDamageSources().magic();
    }
}
