package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Beam Refraction II: El Guardián puede disparar su rayo como un proyectil físico cada 3 segundos.
 * Usa ShulkerBulletEntity para tener un proyectil perseguidor similar al rayo original.
 */
public final class beam_refraction_2_action extends beam_refraction_base_action {
    private final double projectileDamageRatio;
    private final float projectileDamage;

    private static final Map<GuardianEntity, ProjectileState> GUARDIAN_STATES = new WeakHashMap<>();

    static {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Iterator<Map.Entry<GuardianEntity, ProjectileState>> iterator = GUARDIAN_STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<GuardianEntity, ProjectileState> entry = iterator.next();
                GuardianEntity guardian = entry.getKey();
                if (guardian == null || guardian.isRemoved() || !guardian.isAlive()) {
                    iterator.remove();
                    continue;
                }
                if (guardian.getEntityWorld() != world) {
                    continue;
                }
                ProjectileState state = entry.getValue();
                state.cooldownRemaining = Math.max(0, state.cooldownRemaining - 1);
            }
        });
    }

    public beam_refraction_2_action(mutation_action_config config) {
        super(config, 0.0D, 60);
        this.projectileDamageRatio = config.getDouble("projectile_damage_ratio", 1.0D);
        this.projectileDamage = config.getFloat("projectile_damage", 6.0f);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof GuardianEntity guardian)) {
            return;
        }
        if (!(guardian.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        ProjectileState state = GUARDIAN_STATES.computeIfAbsent(guardian, g -> new ProjectileState());

        // Solo dispara si hay un objetivo y el cooldown terminó
        LivingEntity target = guardian.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (state.cooldownRemaining > 0) {
            return;
        }

        // Verificar distancia - disparar solo si está en rango de ataque
        double distance = guardian.squaredDistanceTo(target);
        if (distance > 256.0D) { // 16 bloques máximo
            return;
        }

        // Disparar proyectil perseguidor (ShulkerBullet)
        fireProjectile(guardian, target, world);
        state.cooldownRemaining = getCooldownTicks();
    }

    private void fireProjectile(GuardianEntity guardian, LivingEntity target, ServerWorld world) {
        Vec3d eyePos = guardian.getEyePos();

        // Crear ShulkerBullet que persigue al objetivo (similar al rayo del Guardian)
        ShulkerBulletEntity bullet = new ShulkerBulletEntity(
                world,
                guardian,
                target,
                guardian.getMovementDirection().getAxis()
        );
        bullet.setPos(eyePos.x, eyePos.y, eyePos.z);

        // Partículas de disparo
        for (int i = 0; i < 8; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
            world.spawnParticles(
                    ParticleTypes.BUBBLE_POP,
                    eyePos.x + offsetX,
                    eyePos.y + offsetY,
                    eyePos.z + offsetZ,
                    1, 0, 0, 0, 0.05
            );
        }
        world.spawnParticles(
                ParticleTypes.GLOW,
                eyePos.x, eyePos.y, eyePos.z,
                5, 0.2, 0.2, 0.2, 0.1
        );

        // Sonido de disparo
        world.playSound(
                null,
                guardian.getX(), guardian.getY(), guardian.getZ(),
                SoundEvents.ENTITY_GUARDIAN_ATTACK,
                SoundCategory.HOSTILE,
                1.0f, 1.5f
        );

        world.spawnEntity(bullet);
    }

    public double getProjectileDamageRatio() {
        return projectileDamageRatio;
    }

    public float getProjectileDamage() {
        return projectileDamage;
    }

    private static class ProjectileState {
        int cooldownRemaining = 0;
    }
}
