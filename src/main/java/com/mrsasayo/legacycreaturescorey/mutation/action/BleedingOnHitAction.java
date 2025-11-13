package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Arrays;

/**
 * Deals a sequence of damage pulses after a successful proc.
 */
public final class BleedingOnHitAction extends ProcOnHitAction {
    private final float[] damagePulses;
    private final int intervalTicks;

    public BleedingOnHitAction(double chance, float[] damagePulses, int intervalTicks) {
        super(chance);
        if (damagePulses.length == 0) {
            throw new IllegalArgumentException("Se requieren pulsos de daño");
        }
        this.damagePulses = Arrays.copyOf(damagePulses, damagePulses.length);
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        OnHitTaskScheduler.schedule(world, new DamageTask(world, attacker, victim, damagePulses, intervalTicks));
    }

    private static final class DamageTask implements OnHitTaskScheduler.TimedTask {
        private final ServerWorld world;
        private final LivingEntity attacker;
        private final LivingEntity victim;
        private final float[] pulses;
        private final int interval;
        private int index;
        private int ticksUntilNext;

        private DamageTask(ServerWorld world,
                            LivingEntity attacker,
                            LivingEntity victim,
                            float[] pulses,
                            int interval) {
            this.world = world;
            this.attacker = attacker;
            this.victim = victim;
            this.pulses = pulses;
            this.interval = interval;
            this.index = 0;
            this.ticksUntilNext = interval;
        }

        @Override
        public boolean tick(ServerWorld ignored) {
            if (victim.isRemoved() || !victim.isAlive()) {
                return true;
            }
            if (--ticksUntilNext > 0) {
                return false;
            }

            ticksUntilNext = interval;
            float amount = pulses[index++];
            victim.damage(world, world.getDamageSources().mobAttack(attacker), amount);
            return index >= pulses.length;
        }
    }
}
