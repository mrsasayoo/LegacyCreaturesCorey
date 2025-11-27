package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Aerial Maneuvers II: Disparo en Cono
 * 
 * El Ghast dispara 4 bolas de fuego en un patrón de cono cuando tiene
 * un objetivo válido. Cooldown configurable entre disparos.
 */
public final class aerial_maneuvers_2_action implements mutation_action {
    private final Map<GhastEntity, Integer> timers = new WeakHashMap<>();
    private final int cooldownTicks;
    private final int fireballPower;
    private final double coneSpread;

    public aerial_maneuvers_2_action(mutation_action_config config) {
        this.cooldownTicks = Math.max(20, config.getInt("cooldown_ticks", 120));
        this.fireballPower = Math.max(1, config.getInt("fireball_power", 1));
        this.coneSpread = config.getDouble("cone_spread", 0.4D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof GhastEntity ghast) || entity.getEntityWorld().isClient()) {
            return;
        }
        
        LivingEntity target = ghast.getTarget();
        if (target == null || !target.isAlive()) {
            timers.put(ghast, 0);
            return;
        }
        
        int timer = timers.getOrDefault(ghast, 0) + 1;
        if (timer >= cooldownTicks) {
            timers.put(ghast, 0);
            fireConeShot(ghast, target);
        } else {
            timers.put(ghast, timer);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof GhastEntity ghast) {
            timers.remove(ghast);
        }
    }

    private void fireConeShot(GhastEntity ghast, LivingEntity target) {
        if (!(ghast.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        Vec3d center = ghast.getEyePos();
        
        // Calcular dirección hacia el objetivo en lugar de usar rotación del mob
        Vec3d targetPos = target.getEyePos();
        Vec3d forward = targetPos.subtract(center).normalize();
        
        // Calcular vectores perpendiculares para el spread
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up);
        if (right.lengthSquared() < 0.01D) {
            right = new Vec3d(1, 0, 0);
        }
        right = right.normalize();
        up = right.crossProduct(forward).normalize();

        // 4 bolas de fuego en patrón de cono
        Vec3d[] directions = new Vec3d[] {
                forward.normalize(), // Centro
                forward.add(right.multiply(coneSpread)).normalize(), // Derecha
                forward.add(right.multiply(-coneSpread)).normalize(), // Izquierda
                forward.add(up.multiply(coneSpread)).normalize() // Arriba
        };
        
        // Sonido de disparo
        world.playSound(null, ghast.getX(), ghast.getY(), ghast.getZ(),
                SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 2.0F, 0.8F);
        
        // Partículas de carga
        world.spawnParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z,
                20, 0.5D, 0.5D, 0.5D, 0.05D);
        
        for (Vec3d dir : directions) {
            // Posición inicial de la bola de fuego (delante del Ghast)
            Vec3d startPos = center.add(dir.multiply(2.0D));
            
            // Crear bola de fuego con velocidad hacia la dirección calculada
            FireballEntity fireball = new FireballEntity(world, ghast, dir.multiply(0.3D), fireballPower);
            fireball.setPos(startPos.x, startPos.y, startPos.z);
            
            world.spawnEntity(fireball);
            
            // Partículas de disparo individual
            world.spawnParticles(ParticleTypes.SMOKE,
                    startPos.x, startPos.y, startPos.z,
                    5, 0.1D, 0.1D, 0.1D, 0.02D);
        }
    }
}
