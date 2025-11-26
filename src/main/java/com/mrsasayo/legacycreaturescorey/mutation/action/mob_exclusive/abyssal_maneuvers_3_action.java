package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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
public final class abyssal_maneuvers_3_action implements MutationAction {
    private final Map<WitherEntity, vortex_state> states = new WeakHashMap<>();
    
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
        if (!(entity instanceof WitherEntity wither) || entity.getEntityWorld().isClient()) {
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
        if (entity instanceof WitherEntity wither) {
            vortex_state state = states.remove(wither);
            if (state != null) {
                state.destroyVortexCenter(wither);
            }
        }
    }
    
    /**
     * Crea el vórtice con un ArmorStand invisible como centro
     */
    private void createVortex(WitherEntity wither, vortex_state state) {
        if (!(wither.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        // Posición del vórtice (cerca del Wither, ligeramente adelante)
        Vec3d witherPos = new Vec3d(wither.getX(), wither.getY(), wither.getZ());
        Vec3d forward = wither.getRotationVector().normalize().multiply(3.0D);
        Vec3d vortexPos = witherPos.add(forward);
        
        // Crear ArmorStand invisible como centro de gravedad
        ArmorStandEntity centerStand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        centerStand.setPos(vortexPos.x, vortexPos.y + 1.0D, vortexPos.z);
        centerStand.setInvisible(true);
        centerStand.setNoGravity(true);
        centerStand.setInvulnerable(true);
        centerStand.setSilent(true);
        
        world.spawnEntity(centerStand);
        
        state.vortexCenterUUID = centerStand.getUuid();
        state.vortexTimer = 0;
        state.vortexCenter = vortexPos.add(0, 1.0D, 0);
        
        // Sonido de inicio del vórtice
        world.playSound(null, vortexPos.x, vortexPos.y, vortexPos.z,
                SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1.5F, 0.5F);
    }
    
    /**
     * Procesa el vórtice cada tick
     */
    private void tickVortex(WitherEntity wither, vortex_state state) {
        if (!(wither.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        state.vortexTimer++;
        
        // Verificar si el vórtice debe terminar
        if (state.vortexTimer >= vortexDurationTicks) {
            state.destroyVortexCenter(wither);
            return;
        }
        
        Vec3d center = state.vortexCenter;
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
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, 0.5F, 0.3F);
        }
    }
    
    /**
     * Genera partículas moradas/negras convergiendo al centro
     */
    private void spawnVortexParticles(ServerWorld world, Vec3d center, int timer) {
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
            Vec3d toCenter = center.subtract(startX, startY, startZ).normalize().multiply(0.3D);
            
            // Partículas moradas (portal/enderman)
            world.spawnParticles(ParticleTypes.PORTAL,
                    startX, startY, startZ,
                    1,
                    toCenter.x, toCenter.y, toCenter.z,
                    0.5D);
            
            // Partículas negras (humo del alma)
            world.spawnParticles(ParticleTypes.SOUL,
                    startX, startY + 0.5D, startZ,
                    1,
                    toCenter.x, toCenter.y, toCenter.z,
                    0.3D);
        }
        
        // Partículas en el centro (columna de energía)
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                center.x, center.y, center.z,
                5,
                0.2D, 0.5D, 0.2D,
                0.01D);
        
        // Partículas de wither en espiral
        for (int i = 0; i < 3; i++) {
            double spiralAngle = angle * 2 + (i * Math.PI * 2 / 3);
            double spiralRadius = 0.5D + (timer % 20) * 0.05D;
            
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    center.x + Math.cos(spiralAngle) * spiralRadius,
                    center.y + (timer % 20) * 0.1D - 1.0D,
                    center.z + Math.sin(spiralAngle) * spiralRadius,
                    1, 0, 0.1D, 0, 0.02D);
        }
    }
    
    /**
     * Aplica la fuerza de atracción a jugadores cercanos (simula corriente de agua débil)
     */
    private void applyVortexPull(ServerWorld world, Vec3d center) {
        Box box = new Box(
                center.x - pullRadius, center.y - pullRadius, center.z - pullRadius,
                center.x + pullRadius, center.y + pullRadius, center.z + pullRadius
        );
        
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, box,
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative());
        
        double radiusSq = pullRadius * pullRadius;
        
        for (PlayerEntity player : players) {
            double distSq = player.squaredDistanceTo(center.x, center.y, center.z);
            if (distSq > radiusSq || distSq < 1.0D) {
                continue;
            }
            
            Vec3d playerPos = new Vec3d(player.getX(), player.getY() + player.getHeight() / 2, player.getZ());
            Vec3d toCenter = center.subtract(playerPos);
            double distance = Math.sqrt(distSq);
            
            // Fuerza inversamente proporcional a la distancia (más fuerte cerca del centro)
            double forceFactor = pullStrength * (1.0D - (distance / pullRadius));
            forceFactor = Math.min(forceFactor, maxPullSpeed);
            
            Vec3d pullVelocity = toCenter.normalize().multiply(forceFactor);
            
            // Aplicar velocidad suave (como corriente de agua)
            Vec3d currentVel = player.getVelocity();
            Vec3d newVel = currentVel.add(pullVelocity);
            
            // Limitar velocidad máxima
            if (newVel.length() > maxPullSpeed * 2) {
                newVel = newVel.normalize().multiply(maxPullSpeed * 2);
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
        private Vec3d vortexCenter = null;
        
        private boolean isVortexActive() {
            return vortexCenterUUID != null;
        }
        
        private void destroyVortexCenter(WitherEntity wither) {
            if (vortexCenterUUID != null && wither.getEntityWorld() instanceof ServerWorld world) {
                // Buscar y eliminar el ArmorStand
                world.getEntitiesByClass(ArmorStandEntity.class,
                        wither.getBoundingBox().expand(20.0D),
                        stand -> stand.getUuid().equals(vortexCenterUUID))
                        .forEach(stand -> {
                            // Partículas de dispersión al terminar
                            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
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
