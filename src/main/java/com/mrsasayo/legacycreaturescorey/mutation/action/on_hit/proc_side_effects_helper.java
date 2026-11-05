package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Utilidades compartidas por los rediseños «Identidad On-Hit»: visualización de robos,
 * estela de fuego (Llamas Voraces), latido del Toque Mortal y Aire Espeso.
 */
final class proc_side_effects_helper {
    private proc_side_effects_helper() {
    }

    /** Línea de partículas desde el pecho del jugador hasta el pecho del mob (el robo se ve). */
    static void drawTheftLine(ServerLevel world, LivingEntity from, LivingEntity to, ParticleOptions particle) {
        if (from == null || to == null || !from.isAlive() || !to.isAlive()) {
            return;
        }
        Vec3 a = from.position().add(0.0D, from.getBbHeight() * 0.8D, 0.0D);
        Vec3 b = to.position().add(0.0D, to.getBbHeight() * 0.8D, 0.0D);
        int steps = Math.max(6, (int) (a.distanceTo(b) * 3.0D));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            Vec3 p = a.lerp(b, t);
            world.sendParticles(particle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * «Llamas Voraces» (fire): mientras el jugador ardiente se mueve, deja una estela
     * que le inflige 1 de daño por bloque pisado (respetando fireImmune).
     */
    static void scheduleFireTrail(ServerLevel world, LivingEntity target, int seconds) {
        if (target.fireImmune() || seconds <= 0) {
            return;
        }
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
            private int ticksRemaining = Math.max(1, seconds * 20);
            private BlockPos lastPos;

            @Override
            public boolean tick(ServerLevel currentWorld) {
                if (!target.isAlive() || --ticksRemaining <= 0) {
                    return true;
                }
                BlockPos pos = target.blockPosition();
                if (!pos.equals(lastPos)) {
                    lastPos = pos.immutable();
                    currentWorld.sendParticles(ParticleTypes.FLAME,
                            pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D,
                            4, 0.2D, 0.05D, 0.2D, 0.01D);
                    if (!target.fireImmune()) {
                        target.hurtServer(currentWorld, currentWorld.damageSources().inFire(), 1.0F);
                    }
                }
                return false;
            }
        });
    }

    /**
     * «Toque Mortal» (wither): latido grave que acelera según la vida restante del
     * jugador, mientras dure el Wither aplicado por la mutación.
     */
    static void scheduleWitherHeartbeat(ServerLevel world, LivingEntity victim) {
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
            private int ticksUntilBeat = 0;

            @Override
            public boolean tick(ServerLevel currentWorld) {
                if (!victim.isAlive() || !victim.hasEffect(MobEffects.WITHER)) {
                    return true;
                }
                if (--ticksUntilBeat > 0) {
                    return false;
                }
                // Cuanto menos vida, más rápido late el tambor grave.
                float healthRatio = Math.max(0.0F, victim.getHealth() / Math.max(1.0F, victim.getMaxHealth()));
                ticksUntilBeat = Math.max(8, (int) (healthRatio * 40.0F));
                float pitch = 0.55F + healthRatio * 0.25F;
                currentWorld.playSound(null, victim.getX(), victim.getY(0.5D), victim.getZ(),
                        SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 0.9F, pitch);
                return false;
            }
        });
    }

    /**
     * «Aire Espeso» (slow_falling): durante la Caída Lenta aplicada por la mutación se
     * anulan el elytra (stopFallFlying) y el Riptide (no se deja cargar el tridente).
     * Nivel 3 también rompe el sprint (y con él el impulso del salto con sprint).
     */
    static void scheduleAireEspeso(ServerLevel world, LivingEntity victim, boolean cancelSprint) {
        mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
            @Override
            public boolean tick(ServerLevel currentWorld) {
                if (!victim.isAlive() || !victim.hasEffect(MobEffects.SLOW_FALLING)) {
                    return true;
                }
                if (victim.isFallFlying()) {
                    victim.stopFallFlying();
                }
                if (victim.isUsingItem() && victim.getUseItem().is(Items.TRIDENT)) {
                    victim.stopUsingItem();
                }
                if (cancelSprint && victim.isSprinting()) {
                    victim.setSprinting(false);
                }
                return false;
            }
        });
    }
}
