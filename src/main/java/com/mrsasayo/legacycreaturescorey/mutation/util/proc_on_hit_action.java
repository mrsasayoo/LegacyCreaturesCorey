package com.mrsasayo.legacycreaturescorey.mutation.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * Clase base abstracta para acciones de mutación con probabilidad de activación al golpear.
 *
 * «Gramática sensorial» (Lote Identidad On-Hit): cada proc puede declarar en el JSON una
 * firma de sonido y partícula (proc_sound / proc_particles, opcionalmente proc_particle_count
 * y proc_pitch) para que el jugador aprenda a reconocer qué le pegó sin leer iconos.
 * Solo se disparan cuando el proc REALMENTE ocurre (tras el dado de chance).
 */
public abstract class proc_on_hit_action implements mutation_action {
    private final double chance;
    private final Identifier procSound;
    private final float procPitch;
    private final ParticleOptions procParticle;
    private final int procParticleCount;

    protected proc_on_hit_action(mutation_action_config config, double defaultChance) {
        this.chance = Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D);
        this.procSound = config.getIdentifier("proc_sound", null);
        this.procPitch = (float) Math.min(2.0D, Math.max(0.5D, config.getDouble("proc_pitch", 1.0D)));
        this.procParticle = resolveParticle(config.getIdentifier("proc_particles", config.getIdentifier("proc_particle", null)));
        this.procParticleCount = Math.max(1, config.getInt("proc_particle_count", 12));
    }

    /** Constructor de compatibilidad para clases que no usan gramática sensorial. */
    protected proc_on_hit_action(double chance) {
        this.chance = Mth.clamp(chance, 0.0D, 1.0D);
        this.procSound = null;
        this.procPitch = 1.0F;
        this.procParticle = null;
        this.procParticleCount = 0;
    }

    @Override
    public final void onHit(LivingEntity attacker, LivingEntity victim) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(victim, "victim");

        if (!action_context.isServer(attacker)) {
            return;
        }
        if (chance < 1.0D && attacker.getRandom().nextDouble() >= chance) {
            return;
        }
        onProc(attacker, victim);
        playProcSignature(victim);
    }

    private void playProcSignature(LivingEntity victim) {
        if (procSound == null && procParticle == null) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel world)) {
            return;
        }
        double x = victim.getX();
        double y = victim.getY(0.5D);
        double z = victim.getZ();
        if (procSound != null) {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(procSound);
            if (sound != null) {
                world.playSound(null, x, y, z, sound, SoundSource.HOSTILE, 0.8F, procPitch);
            }
        }
        if (procParticle != null) {
            world.sendParticles(procParticle, x, y, z, procParticleCount, 0.3D, 0.3D, 0.3D, 0.02D);
        }
    }

    private static ParticleOptions resolveParticle(Identifier id) {
        if (id == null) {
            return null;
        }
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
        if (type instanceof ParticleOptions options) {
            return options;
        }
        return null;
    }

    protected abstract void onProc(LivingEntity attacker, LivingEntity victim);

    protected double getChance() {
        return chance;
    }
}
