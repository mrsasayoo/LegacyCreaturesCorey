package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Aerial Maneuvers II: Disparo en Cono
 * 
 * El Ghast dispara 4 bolas de fuego en un patrón de cono cuando tiene
 * un objetivo válido. Cooldown configurable entre disparos.
 */
public final class aerial_maneuvers_2_action implements mutation_action {
    private final Map<Ghast, Integer> timers = new WeakHashMap<>();
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
        if (!(entity instanceof Ghast ghast) || entity.getEntityWorld().isClient()) {
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
        if (entity instanceof Ghast ghast) {
            timers.remove(ghast);
        }
    }

    private void fireConeShot(Ghast ghast, LivingEntity target) {
        if (!(ghast.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        
        Vec3 center = ghast.getEyePos();
        
        // Calcular dirección hacia el objetivo en lugar de usar rotación del mob
        Vec3 targetPos = target.getEyePos();
        Vec3 forward = targetPos.subtract(center).normalize();
        
        // Calcular vectores perpendiculares para el spread
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() < 0.01D) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        up = right.cross(forward).normalize();

        // 4 bolas de fuego en patrón de cono
        Vec3[] directions = new Vec3[] {
                forward.normalize(), // Centro
                forward.atLowerCornerWithOffset(right.scale(coneSpread)).normalize(), // Derecha
                forward.atLowerCornerWithOffset(right.scale(-coneSpread)).normalize(), // Izquierda
                forward.atLowerCornerWithOffset(up.scale(coneSpread)).normalize() // Arriba
        };
        
        // Sonido de disparo
        world.makeSound(null, ghast.getX(), ghast.getY(), ghast.getZ(),
                SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 2.0F, 0.8F);
        
        // Partículas de carga
        world.sendParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z,
                20, 0.5D, 0.5D, 0.5D, 0.05D);
        
        for (Vec3 dir : directions) {
            // Posición inicial de la bola de fuego (delante del Ghast)
            Vec3 startPos = center.atLowerCornerWithOffset(dir.scale(2.0D));
            
            // Crear bola de fuego con velocidad hacia la dirección calculada
            LargeFireball fireball = new LargeFireball(world, ghast, dir.scale(0.3D), fireballPower);
            fireball.setPos(startPos.x, startPos.y, startPos.z);
            
            world.spawnEntity(fireball);
            
            // Partículas de disparo individual
            world.sendParticles(ParticleTypes.SMOKE,
                    startPos.x, startPos.y, startPos.z,
                    5, 0.1D, 0.1D, 0.1D, 0.02D);
        }
    }
}
