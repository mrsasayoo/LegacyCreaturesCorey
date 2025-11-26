package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Acidic Core I: Charco Ácido Básico
 * 
 * Al morir, el slime genera un charco ácido visible con partículas densas.
 * Mejorado: Mayor densidad y visibilidad de partículas.
 */
public final class acidic_core_1_action implements MutationAction {
    private final int particleCount;
    private final double horizontalSpread;
    private final double verticalSpread;
    
    // Configuración de partículas de alta densidad
    private static final int PARTICLE_MULTIPLIER = 4; // Multiplicador de densidad

    public acidic_core_1_action(mutation_action_config config) {
        this.particleCount = Math.max(1, config.getInt("particle_count", 20));
        this.horizontalSpread = Math.max(0.0D, config.getDouble("horizontal_spread", 0.5D));
        this.verticalSpread = Math.max(0.0D, config.getDouble("vertical_spread", 0.1D));
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, LivingEntity killer) {
        if (!(entity instanceof SlimeEntity slime)) {
            return;
        }
        if (!(slime.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        double x = slime.getX();
        double y = slime.getY();
        double z = slime.getZ();
        
        // Oleada 1: Partículas de slime densas
        world.spawnParticles(ParticleTypes.ITEM_SLIME,
                x, y, z,
                particleCount * PARTICLE_MULTIPLIER,
                horizontalSpread * 1.5D,
                verticalSpread,
                horizontalSpread * 1.5D,
                0.08D);
        
        // Oleada 2: Goteo ácido
        world.spawnParticles(ParticleTypes.FALLING_DRIPSTONE_WATER,
                x, y + 0.5D, z,
                particleCount * 2,
                horizontalSpread,
                0.3D,
                horizontalSpread,
                0.02D);
        
        // Oleada 3: Humo de vapor ácido (verde brillante usando HAPPY_VILLAGER)
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y + 0.1D, z,
                particleCount * 3,
                horizontalSpread * 1.2D,
                0.1D,
                horizontalSpread * 1.2D,
                0.01D);
        
        // Partículas de humo verde para efecto de vapor ácido
        world.spawnParticles(ParticleTypes.SNEEZE,
                x, y + 0.2D, z,
                particleCount / 2,
                horizontalSpread,
                0.2D,
                horizontalSpread,
                0.03D);
        
        // Efecto de burbujeo
        world.spawnParticles(ParticleTypes.BUBBLE_POP,
                x, y + 0.05D, z,
                particleCount,
                horizontalSpread * 0.8D,
                0.05D,
                horizontalSpread * 0.8D,
                0.02D);
    }
}
