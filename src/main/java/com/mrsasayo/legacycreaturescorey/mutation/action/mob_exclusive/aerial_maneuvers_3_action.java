package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
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
 * Aerial Maneuvers III: Fuego Rápido
 * 
 * El Ghast dispara 3 bolas de fuego en rápida sucesión (ráfaga).
 * Cada disparo tiene una pequeña variación de dirección para crear
 * un patrón impredecible.
 */
public final class aerial_maneuvers_3_action implements MutationAction {
    private final Map<GhastEntity, burst_state> states = new WeakHashMap<>();
    private final int cooldownTicks;
    private final int shotsPerBurst;
    private final int ticksBetweenShots;
    private final double varianceDegrees;
    private final int fireballPower;

    public aerial_maneuvers_3_action(mutation_action_config config) {
        this.cooldownTicks = Math.max(20, config.getInt("cooldown_ticks", 200));
        this.shotsPerBurst = Math.max(1, config.getInt("shots_per_burst", 3));
        this.ticksBetweenShots = Math.max(1, config.getInt("ticks_between_shots", 5));
        this.varianceDegrees = Math.max(0.0D, config.getDouble("variance_degrees", 5.0D));
        this.fireballPower = Math.max(1, config.getInt("fireball_power", 1));
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof GhastEntity ghast) || entity.getEntityWorld().isClient()) {
            return;
        }
        
        burst_state state = states.computeIfAbsent(ghast, ignored -> new burst_state());
        
        // Si está en modo de disparo rápido
        if (state.firing) {
            state.shotTimer++;
            if (state.shotTimer >= ticksBetweenShots) {
                state.shotTimer = 0;
                LivingEntity target = ghast.getTarget();
                if (target != null && target.isAlive()) {
                    fireSingleShot(ghast, target, state.shotsFired);
                }
                state.shotsFired++;
                if (state.shotsFired >= shotsPerBurst) {
                    state.reset();
                }
            }
            return;
        }

        // Incrementar cooldown
        state.cooldownTimer++;
        
        LivingEntity target = ghast.getTarget();
        if (target != null && target.isAlive() && state.cooldownTimer >= cooldownTicks) {
            // Iniciar ráfaga
            state.beginBurst();
            
            // Sonido de inicio de ráfaga
            if (ghast.getEntityWorld() instanceof ServerWorld world) {
                world.playSound(null, ghast.getX(), ghast.getY(), ghast.getZ(),
                        SoundEvents.ENTITY_GHAST_WARN, SoundCategory.HOSTILE, 1.5F, 1.2F);
                
                // Partículas de carga
                world.spawnParticles(ParticleTypes.FLAME,
                        ghast.getX(), ghast.getEyeY(), ghast.getZ(),
                        15, 0.8D, 0.8D, 0.8D, 0.05D);
            }
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof GhastEntity ghast) {
            states.remove(ghast);
        }
    }

    private void fireSingleShot(GhastEntity ghast, LivingEntity target, int shotIndex) {
        if (!(ghast.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        Vec3d ghastPos = ghast.getEyePos();
        Vec3d targetPos = target.getEyePos();
        
        // Dirección base hacia el objetivo
        Vec3d baseDirection = targetPos.subtract(ghastPos).normalize();
        
        // Aplicar variación aleatoria
        double varianceRad = Math.toRadians(varianceDegrees);
        double yaw = Math.atan2(baseDirection.z, baseDirection.x);
        double pitch = Math.asin(baseDirection.y);
        
        // Variación aleatoria en yaw y pitch
        yaw += (ghast.getRandom().nextDouble() - 0.5D) * 2.0D * varianceRad;
        pitch += (ghast.getRandom().nextDouble() - 0.5D) * 2.0D * varianceRad;
        
        // Limitar pitch para evitar disparos extremos
        pitch = Math.max(-Math.PI / 3, Math.min(Math.PI / 3, pitch));

        double x = Math.cos(yaw) * Math.cos(pitch);
        double y = Math.sin(pitch);
        double z = Math.sin(yaw) * Math.cos(pitch);
        Vec3d direction = new Vec3d(x, y, z).normalize();

        Vec3d start = ghastPos.add(direction.multiply(2.0D));
        
        // Crear y disparar la bola de fuego
        FireballEntity fireball = new FireballEntity(world, ghast, direction.multiply(0.3D), fireballPower);
        fireball.setPos(start.x, start.y, start.z);
        world.spawnEntity(fireball);
        
        // Sonido de disparo
        world.playSound(null, ghast.getX(), ghast.getY(), ghast.getZ(),
                SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.0F, 1.0F + shotIndex * 0.1F);
        
        // Partículas de disparo
        world.spawnParticles(ParticleTypes.SMOKE,
                start.x, start.y, start.z,
                8, 0.2D, 0.2D, 0.2D, 0.03D);
        world.spawnParticles(ParticleTypes.FLAME,
                start.x, start.y, start.z,
                5, 0.1D, 0.1D, 0.1D, 0.02D);
    }

    private final class burst_state {
        private int cooldownTimer = 0;
        private int shotsFired = 0;
        private int shotTimer = 0;
        private boolean firing = false;

        private void beginBurst() {
            firing = true;
            shotsFired = 0;
            shotTimer = ticksBetweenShots; // Disparar inmediatamente
            cooldownTimer = 0;
        }

        private void reset() {
            firing = false;
            shotsFired = 0;
            shotTimer = 0;
            cooldownTimer = 0;
        }
    }
}
