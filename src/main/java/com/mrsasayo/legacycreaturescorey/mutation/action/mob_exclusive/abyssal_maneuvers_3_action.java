package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Abyssal Maneuvers III: Vórtice Abisal
 * 
 * Crea un vórtice con un ArmorStand invisible como centro de gravedad.
 * El vórtice atrae a los jugadores cercanos con una fuerza débil (simula corriente de agua).
 * Partículas moradas/negras convergen hacia el centro.
 */
public final class abyssal_maneuvers_3_action implements mutation_action {
    private final Map<WitherBoss, vortex_state> states = new WeakHashMap<>();
    
    private final int cooldownTicks;
    private final int vortexDurationTicks;
    private final double pullRadius;
    private final double pullStrength;
    private final double maxPullSpeed;

    public abyssal_maneuvers_3_action(mutation_action_config config) {
        this.cooldownTicks = Math.max(20, config.getInt("cooldown_ticks", 300));
        this.vortexDurationTicks = Math.max(20, config.getInt("vortex_duration_ticks", 100)); // 5 segundos
        this.pullRadius = Math.max(1.0D, config.getDouble("pull_radius", 10.0D));
        // Fuerza débil para simular corriente de agua
        this.pullStrength = Math.max(0.01D, config.getDouble("pull_strength", 0.08D));
        this.maxPullSpeed = Math.max(0.1D, config.getDouble("max_pull_speed", 0.4D));
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof WitherBoss wither) || entity.getEntityWorld().isClient()) {
            return;
        }
        
        vortex_state state = states.computeIfAbsent(wither, ignored -> new vortex_state());
        
        // Si hay un vórtice activo, procesarlo
        if (state.isVortexActive()) {
            tickVortex(wither, state);
            return;
        }
        
        // Incrementar cooldown
        state.cooldownTimer++;
        if (state.cooldownTimer >= cooldownTicks) {
            state.cooldownTimer = 0;
            createVortex(wither, state);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof WitherBoss wither) {
            vortex_state state = states.remove(wither);
            if (state != null) {
                state.destroyVortexCenter(wither);
            }
        }
    }
    
    /**
     * Crea el vórtice con un ArmorStand invisible como centro
     */
    private void createVortex(WitherBoss wither, vortex_state state) {
        if (!(wither.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        
        // Posición del vórtice (cerca del Wither, ligeramente adelante)
        Vec3 witherPos = new Vec3(wither.getX(), wither.getY(), wither.getZ());
        Vec3 forward = wither.getRotationVector().normalize().scale(3.0D);
        Vec3 vortexPos = witherPos.atLowerCornerWithOffset(forward);
        
        // Crear ArmorStand invisible como centro de gravedad
        ArmorStand centerStand = new ArmorStand(EntityType.ARMOR_STAND, world);
        centerStand.setPos(vortexPos.x, vortexPos.y + 1.0D, vortexPos.z);
        centerStand.setInvisible(true);
        centerStand.setNoGravity(true);
        centerStand.setInvulnerable(true);
        centerStand.setSilent(true);
        
        world.spawnEntity(centerStand);
        
        state.vortexCenterUUID = centerStand.getUuid();
        state.vortexTimer = 0;
        state.vortexCenter = vortexPos.atLowerCornerWithOffset(0, 1.0D, 0);
        
        // Sonido de inicio del vórtice
        world.makeSound(null, vortexPos.x, vortexPos.y, vortexPos.z,
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
    }
    
    /**
     * Procesa el vórtice cada tick
     */
    private void tickVortex(WitherBoss wither, vortex_state state) {
        if (!(wither.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        
        state.vortexTimer++;
        
        // Verificar si el vórtice debe terminar
        if (state.vortexTimer >= vortexDurationTicks) {
            state.destroyVortexCenter(wither);
            return;
        }
        
        Vec3 center = state.vortexCenter;
        if (center == null) {
            state.destroyVortexCenter(wither);
            return;
        }
        
        // Generar partículas convergentes al centro
        spawnVortexParticles(world, center, state.vortexTimer);
        
        // Aplicar atracción a jugadores cercanos
        applyVortexPull(world, center);
        
        // Sonido ambiental del vórtice (cada 20 ticks)
        if (state.vortexTimer % 20 == 0) {
            world.makeSound(null, center.x, center.y, center.z,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 0.5F, 0.3F);
        }
    }
    
    /**
     * Genera partículas moradas/negras convergiendo al centro
     */
    private void spawnVortexParticles(ServerLevel world, Vec3 center, int timer) {
        // Ángulo de rotación basado en el tiempo
        double angle = (timer * 0.2D) % (Math.PI * 2);
        
        for (int i = 0; i < 8; i++) {
            double particleAngle = angle + (i * Math.PI / 4);
            double distance = pullRadius * 0.8D;
            
            // Posición inicial de la partícula (borde del vórtice)
            double startX = center.x + Math.cos(particleAngle) * distance;
            double startZ = center.z + Math.sin(particleAngle) * distance;
            double startY = center.y + (Math.sin(timer * 0.1D + i) * 1.5D);
            
            // Velocidad hacia el centro
            Vec3 toCenter = center.subtract(startX, startY, startZ).normalize().scale(0.3D);
            
            // Partículas moradas (portal/enderman)
            world.sendParticles(ParticleTypes.PORTAL,
                    startX, startY, startZ,
                    1,
                    toCenter.x, toCenter.y, toCenter.z,
                    0.5D);
            
            // Partículas negras (humo del alma)
            world.sendParticles(ParticleTypes.SOUL,
                    startX, startY + 0.5D, startZ,
                    1,
                    toCenter.x, toCenter.y, toCenter.z,
                    0.3D);
        }
        
        // Partículas en el centro (columna de energía)
        world.sendParticles(ParticleTypes.REVERSE_PORTAL,
                center.x, center.y, center.z,
                5,
                0.2D, 0.5D, 0.2D,
                0.01D);
        
        // Partículas de wither en espiral
        for (int i = 0; i < 3; i++) {
            double spiralAngle = angle * 2 + (i * Math.PI * 2 / 3);
            double spiralRadius = 0.5D + (timer % 20) * 0.05D;
            
            world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    center.x + Math.cos(spiralAngle) * spiralRadius,
                    center.y + (timer % 20) * 0.1D - 1.0D,
                    center.z + Math.sin(spiralAngle) * spiralRadius,
                    1, 0, 0.1D, 0, 0.02D);
        }
    }
    
    /**
     * Aplica la fuerza de atracción a jugadores cercanos (simula corriente de agua débil)
     */
    private void applyVortexPull(ServerLevel world, Vec3 center) {
        AABB box = new AABB(
                center.x - pullRadius, center.y - pullRadius, center.z - pullRadius,
                center.x + pullRadius, center.y + pullRadius, center.z + pullRadius
        );
        
        List<Player> players = world.getEntitiesByClass(Player.class, box,
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative());
        
        double radiusSq = pullRadius * pullRadius;
        
        for (Player player : players) {
            double distSq = player.distanceToSqr(center.x, center.y, center.z);
            if (distSq > radiusSq || distSq < 1.0D) {
                continue;
            }
            
            Vec3 playerPos = new Vec3(player.getX(), player.getY() + player.getHeight() / 2, player.getZ());
            Vec3 toCenter = center.subtract(playerPos);
            double distance = Math.sqrt(distSq);
            
            // Fuerza inversamente proporcional a la distancia (más fuerte cerca del centro)
            double forceFactor = pullStrength * (1.0D - (distance / pullRadius));
            forceFactor = Math.min(forceFactor, maxPullSpeed);
            
            Vec3 pullVelocity = toCenter.normalize().scale(forceFactor);
            
            // Aplicar velocidad suave (como corriente de agua)
            Vec3 currentVel = player.getVelocity();
            Vec3 newVel = currentVel.atLowerCornerWithOffset(pullVelocity);
            
            // Limitar velocidad máxima
            if (newVel.length() > maxPullSpeed * 2) {
                newVel = newVel.normalize().scale(maxPullSpeed * 2);
            }
            
            player.setVelocity(newVel);
            player.velocityModified = true;
        }
    }

    /**
     * Estado del vórtice para cada Wither
     */
    private static final class vortex_state {
        private int cooldownTimer = 0;
        private int vortexTimer = 0;
        private UUID vortexCenterUUID = null;
        private Vec3 vortexCenter = null;
        
        private boolean isVortexActive() {
            return vortexCenterUUID != null;
        }
        
        private void destroyVortexCenter(WitherBoss wither) {
            if (vortexCenterUUID != null && wither.getEntityWorld() instanceof ServerLevel world) {
                // Buscar y eliminar el ArmorStand
                world.getEntitiesByClass(ArmorStand.class,
                        wither.getLocalBoundsForPose().inflate(20.0D),
                        stand -> stand.getUuid().equals(vortexCenterUUID))
                        .forEach(stand -> {
                            // Partículas de dispersión al terminar
                            world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    stand.getX(), stand.getY(), stand.getZ(),
                                    30, 1.0D, 1.0D, 1.0D, 0.1D);
                            stand.discard();
                        });
            }
            vortexCenterUUID = null;
            vortexCenter = null;
            vortexTimer = 0;
        }
    }
}
