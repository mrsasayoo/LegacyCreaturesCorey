package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Locale;

/**
 * Manipulates incoming projectiles around the aura bearer.
 */
public final class ProjectileShroudAuraAction implements MutationAction {
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
        if (!ActionContext.isServer(entity)) {
            return;
        }
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
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

            switch (mode) {
                case DESTROY -> projectile.discard();
                case DEFLECT -> {
                    projectile.setOwner(entity);
                    projectile.setVelocity(projectile.getVelocity().multiply(-Math.max(0.2D, reflectDamageFactor)));
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
