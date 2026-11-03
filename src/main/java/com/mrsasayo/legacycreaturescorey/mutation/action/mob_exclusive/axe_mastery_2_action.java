package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Axe Mastery II: Golpe Circular
 * 
 * Al atacar, el Vindicator ejecuta un golpe circular que daña a todas
 * las entidades en un área expandida alrededor de él.
 */
public final class axe_mastery_2_action extends axe_mastery_base_action {
    private final double slashRange;
    private final double damageMultiplier;
    private final int cooldownTicks;

    public axe_mastery_2_action(mutation_action_config config) {
        this.slashRange = config.getDouble("slash_range", 3.5D);
        this.damageMultiplier = config.getDouble("damage_multiplier", 0.6D); // 60% del daño base para AoE
        this.cooldownTicks = config.getInt("cooldown_ticks", 80); // 4 segundos de cooldown
    }
    
    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        Vindicator vindicator = asServerVindicator(attacker);
        if (vindicator == null) {
            return;
        }
        
        Handler handler = handler();
        if (handler.isOnCooldown(vindicator, "circular_slash")) {
            return;
        }
        
        // Ejecutar el golpe circular
        performCircularSlash(vindicator, target);
        handler.setCooldown(vindicator, "circular_slash", cooldownTicks);
    }

    @Override
    public void onTick(LivingEntity entity) {
        // Este método ya no se usa para el ataque - ahora se activa en onHit
    }

    /**
     * Ejecuta el golpe circular dañando a todas las entidades en rango
     */
    private void performCircularSlash(Vindicator vindicator, LivingEntity primaryTarget) {
        if (!(vindicator.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        
        // Crear caja de colisión expandida para el AoE
        AABB aoeBox = vindicator.getLocalBoundsForPose().inflate(slashRange);
        
        // Obtener todas las entidades vivas en el área (excepto el Vindicator)
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, aoeBox,
                living -> living != vindicator && 
                          living.isAlive() && 
                          !(living instanceof Vindicator) && // No dañar otros Vindicators
                          living != primaryTarget); // El objetivo principal ya recibió daño
        
        // Calcular daño base del Vindicator
        float baseDamage = (float) vindicator.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float aoeDamage = (float) (baseDamage * damageMultiplier);
        
        // Efectos visuales del golpe circular
        spawnSlashParticles(world, vindicator);
        
        // Sonido del golpe
        world.makeSound(null, vindicator.getX(), vindicator.getY(), vindicator.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.8F);
        
        // Aplicar daño a todos los objetivos en el área
        DamageSource damageSource = world.getDamageSources().mobAttack(vindicator);
        
        for (LivingEntity target : targets) {
            // Verificar distancia real (el box puede incluir entidades fuera del rango circular)
            double distSq = vindicator.distanceToSqr(target);
            if (distSq > slashRange * slashRange) {
                continue;
            }
            
            // Aplicar daño
            target.damage(world, damageSource, aoeDamage);
            
            // Pequeño knockback
            Vec3 knockbackDir = new Vec3(
                    target.getX() - vindicator.getX(),
                    0,
                    target.getZ() - vindicator.getZ()
            ).normalize();
            target.addVelocity(knockbackDir.x * 0.3D, 0.1D, knockbackDir.z * 0.3D);
            target.velocityModified = true;
        }
    }
    
    /**
     * Genera partículas visuales para el golpe circular
     */
    private void spawnSlashParticles(ServerLevel world, Vindicator vindicator) {
        double x = vindicator.getX();
        double y = vindicator.getY() + 1.0D;
        double z = vindicator.getZ();
        
        // Partículas en círculo
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0D) * Math.PI * 2;
            double offsetX = Math.cos(angle) * slashRange * 0.8D;
            double offsetZ = Math.sin(angle) * slashRange * 0.8D;
            
            world.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x + offsetX, y, z + offsetZ,
                    1, 0, 0, 0, 0);
        }
        
        // Partículas de daño en el centro
        world.sendParticles(ParticleTypes.CRIT,
                x, y, z,
                10, slashRange * 0.5D, 0.3D, slashRange * 0.5D, 0.1D);
    }
}
