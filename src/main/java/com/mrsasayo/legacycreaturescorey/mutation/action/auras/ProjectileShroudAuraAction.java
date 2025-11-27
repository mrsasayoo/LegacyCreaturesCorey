package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Manipulates incoming projectiles around the aura bearer.
 */
public final class ProjectileShroudAuraAction implements mutation_action {
    private static final Map<ServerWorld, Set<PersistentProjectileEntity>> PROCESSED_PROJECTILES = new WeakHashMap<>();

    private final double radius;
    private final double chance;
    private final Mode mode;
    private final double pushStrength;
    private final double reflectDamageFactor;

    public ProjectileShroudAuraAction(double radius,
                                      double chance,
                                      Mode mode,
                                      double pushStrength,
                                      double reflectDamageFactor) {
        this.radius = Math.max(0.5D, radius);
        this.chance = Math.min(1.0D, Math.max(0.0D, chance));
        this.mode = mode;
        this.pushStrength = pushStrength;
        this.reflectDamageFactor = reflectDamageFactor;
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        Set<PersistentProjectileEntity> processed = PROCESSED_PROJECTILES.computeIfAbsent(world, ignored -> Collections.newSetFromMap(new WeakHashMap<>()));
        List<PersistentProjectileEntity> projectiles = world.getEntitiesByClass(
            PersistentProjectileEntity.class,
            entity.getBoundingBox().expand(radius),
            projectile -> projectile.isAlive() && projectile.isOnGround() == false
        );

        for (PersistentProjectileEntity projectile : projectiles) {
            if (entity.squaredDistanceTo(projectile) > radius * radius) {
                continue;
            }
            if (chance < 1.0D && entity.getRandom().nextDouble() >= chance) {
                continue;
            }
            if (processed.contains(projectile)) {
                continue;
            }

            switch (mode) {
                case DESTROY -> projectile.discard();
                case DEFLECT -> {
                    projectile.setOwner(entity);
                    projectile.setVelocity(projectile.getVelocity().multiply(-Math.max(0.2D, reflectDamageFactor)));
                    projectile.velocityDirty = true;
                    projectile.velocityModified = true;
                    processed.add(projectile);
                }
                case SHOVE_SHOOTER -> {
                    if (projectile.getOwner() instanceof LivingEntity shooter) {
                        double dx = shooter.getX() - entity.getX();
                        double dz = shooter.getZ() - entity.getZ();
                        double length = Math.max(0.0001D, Math.hypot(dx, dz));
                        shooter.addVelocity(dx / length * pushStrength, 0.25D, dz / length * pushStrength);
                        shooter.velocityModified = true;
                    }
                    projectile.discard();
                }
            }
        }
        processed.removeIf(projectile -> projectile == null || !projectile.isAlive());
    }

    public enum Mode {
        DESTROY,
        SHOVE_SHOOTER,
        DEFLECT;

        public static Mode fromString(String raw) {
            return Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
    }
}
