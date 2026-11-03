package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Abyssal Maneuvers II: Ráfaga Potenciada
 * 
 * Cuando la vida del Wither baja del 50%, dispara 2 proyectiles adicionales
 * por cada ráfaga normal. Los proyectiles adicionales se disparan ligeramente
 * desviados del objetivo principal para crear un patrón de cono.
 */
public final class abyssal_maneuvers_2_action implements mutation_action {
    private final Map<WitherBoss, extra_shot_state> states = new WeakHashMap<>();
    
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
        if (!(entity instanceof WitherBoss wither) || entity.level().isClientSide()) {
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
    private void fireExtraProjectiles(WitherBoss wither, LivingEntity target) {
        if (!(wither.level() instanceof ServerLevel world)) {
            return;
        }
        
        Vec3 witherPos = wither.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 baseDirection = targetPos.subtract(witherPos).normalize();
        
        // Calcular vectores perpendiculares para el spread
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = baseDirection.cross(up).normalize();
        if (right.lengthSqr() < 0.01) {
            right = new Vec3(1, 0, 0);
        }
        
        // Convertir ángulo a radianes
        double spreadRad = Math.toRadians(spreadAngle);
        
        // Efectos visuales al disparar
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                witherPos.x, witherPos.y, witherPos.z,
                20, 0.5D, 0.5D, 0.5D, 0.1D);
        
        world.playSound(null, wither.getX(), wither.getY(), wither.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 0.7F);
        
        // Disparar proyectiles con spread uniforme
        for (int i = 0; i < extraProjectiles; i++) {
            // Calcular ángulo para este proyectil
            double angleOffset = spreadRad * (i - (extraProjectiles - 1) / 2.0D);
            
            // Rotar la dirección base
            Vec3 spreadDir = rotateVector(baseDirection, right, angleOffset);
            
            // Crear y configurar el cráneo
            spawnWitherSkull(wither, world, witherPos, spreadDir);
        }
    }
    
    /**
     * Rota un vector alrededor de un eje
     */
    private Vec3 rotateVector(Vec3 vector, Vec3 axis, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        
        return vector.scale(cos)
                .add(axis.cross(vector).scale(sin))
                .add(axis.scale(axis.dot(vector) * (1 - cos)));
    }
    
    /**
     * Crea y dispara un cráneo de Wither
     */
    private void spawnWitherSkull(WitherBoss wither, ServerLevel world, Vec3 pos, Vec3 direction) {
        WitherSkull skull = (WitherSkull) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("wither_skull")).create(world, EntitySpawnReason.MOB_SUMMONED);
        if (skull == null) {
            return;
        }
        skull.setOwner(wither);
        skull.setPos(pos.x, pos.y, pos.z);
        skull.setDeltaMovement(new Vec3(direction.x * 0.9D, direction.y * 0.9D, direction.z * 0.9D));
        skull.setDangerous(chargedSkulls);
        
        world.addFreshEntity(skull);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof WitherBoss wither) {
            states.remove(wither);
        }
    }

    private static final class extra_shot_state {
        private int shotCooldown = 0;
    }
}
