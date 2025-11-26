package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Abyssal Maneuvers II: Ráfaga Potenciada
 * 
 * Cuando la vida del Wither baja del 50%, dispara 2 proyectiles adicionales
 * por cada ráfaga normal. Los proyectiles adicionales se disparan ligeramente
 * desviados del objetivo principal para crear un patrón de cono.
 */
public final class abyssal_maneuvers_2_action implements MutationAction {
    private final Map<WitherEntity, extra_shot_state> states = new WeakHashMap<>();
    
    private final double healthThreshold;
    private final int extraProjectiles;
    private final double spreadAngle;
    private final int cooldownTicks;
    private final boolean chargedSkulls;

    public abyssal_maneuvers_2_action(mutation_action_config config) {
        this.healthThreshold = config.getDouble("health_threshold", 0.5D); // 50% de vida
        this.extraProjectiles = config.getInt("extra_projectiles", 2);
        this.spreadAngle = config.getDouble("spread_angle_degrees", 15.0D);
        this.cooldownTicks = Math.max(20, config.getInt("shot_cooldown_ticks", 40));
        this.chargedSkulls = config.getBoolean("charged_skulls", false);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof WitherEntity wither) || entity.getEntityWorld().isClient()) {
            return;
        }
        
        extra_shot_state state = states.computeIfAbsent(wither, ignored -> new extra_shot_state());
        
        // Decrementar cooldown
        if (state.shotCooldown > 0) {
            state.shotCooldown--;
        }
        
        // Verificar si la vida está por debajo del umbral
        float healthPercent = wither.getHealth() / wither.getMaxHealth();
        if (healthPercent > healthThreshold) {
            return; // No activar si la vida está por encima del 50%
        }
        
        // Buscar objetivo
        LivingEntity target = wither.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        
        // Verificar si está en cooldown
        if (state.shotCooldown > 0) {
            return;
        }
        
        // Disparar proyectiles adicionales
        fireExtraProjectiles(wither, target);
        state.shotCooldown = cooldownTicks;
    }
    
    /**
     * Dispara los proyectiles adicionales hacia el objetivo con spread
     */
    private void fireExtraProjectiles(WitherEntity wither, LivingEntity target) {
        if (!(wither.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        Vec3d witherPos = wither.getEyePos();
        Vec3d targetPos = target.getEyePos();
        Vec3d baseDirection = targetPos.subtract(witherPos).normalize();
        
        // Calcular vectores perpendiculares para el spread
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = baseDirection.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.01) {
            right = new Vec3d(1, 0, 0);
        }
        
        // Convertir ángulo a radianes
        double spreadRad = Math.toRadians(spreadAngle);
        
        // Efectos visuales al disparar
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                witherPos.x, witherPos.y, witherPos.z,
                20, 0.5D, 0.5D, 0.5D, 0.1D);
        
        world.playSound(null, wither.getX(), wither.getY(), wither.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.0F, 0.7F);
        
        // Disparar proyectiles con spread uniforme
        for (int i = 0; i < extraProjectiles; i++) {
            // Calcular ángulo para este proyectil
            double angleOffset = spreadRad * (i - (extraProjectiles - 1) / 2.0D);
            
            // Rotar la dirección base
            Vec3d spreadDir = rotateVector(baseDirection, right, angleOffset);
            
            // Crear y configurar el cráneo
            spawnWitherSkull(wither, world, witherPos, spreadDir);
        }
    }
    
    /**
     * Rota un vector alrededor de un eje
     */
    private Vec3d rotateVector(Vec3d vector, Vec3d axis, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        
        return vector.multiply(cos)
                .add(axis.crossProduct(vector).multiply(sin))
                .add(axis.multiply(axis.dotProduct(vector) * (1 - cos)));
    }
    
    /**
     * Crea y dispara un cráneo de Wither
     */
    private void spawnWitherSkull(WitherEntity wither, ServerWorld world, Vec3d pos, Vec3d direction) {
        WitherSkullEntity skull = EntityType.WITHER_SKULL.create(world, SpawnReason.MOB_SUMMONED);
        if (skull == null) {
            return;
        }
        skull.setOwner(wither);
        skull.setPos(pos.x, pos.y, pos.z);
        skull.setVelocity(direction.x * 0.9D, direction.y * 0.9D, direction.z * 0.9D);
        skull.setCharged(chargedSkulls);
        
        world.spawnEntity(skull);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof WitherEntity wither) {
            states.remove(wither);
        }
    }

    private static final class extra_shot_state {
        private int shotCooldown = 0;
    }
}
