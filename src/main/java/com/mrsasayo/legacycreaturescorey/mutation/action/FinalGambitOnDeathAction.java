package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Implements the Final Gambit line of on-death behaviors.
 */
public final class FinalGambitOnDeathAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final int cooldownTicks;
    private final int collapseDuration;

    public FinalGambitOnDeathAction(Mode mode, double radius, int cooldownTicks, int collapseDuration) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.collapseDuration = Math.max(0, collapseDuration);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
    Vec3d origin = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        switch (mode) {
            case PROJECTILE_PURGE -> purgeProjectiles(world, origin);
            case WEAPON_COOLDOWN -> applyWeaponCooldown(killer);
            case REALITY_COLLAPSE -> spawnCollapse(world, origin);
        }
    }

    private void purgeProjectiles(ServerWorld world, Vec3d origin) {
        Box box = Box.of(origin, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        List<ProjectileEntity> projectiles = world.getEntitiesByClass(ProjectileEntity.class, box,
            projectile -> projectile.isAlive() && projectile.squaredDistanceTo(origin) <= radius * radius);
        for (ProjectileEntity projectile : projectiles) {
            projectile.discard();
        }
    }

    private void applyWeaponCooldown(@Nullable LivingEntity killer) {
        if (!(killer instanceof PlayerEntity player)) {
            return;
        }
        if (cooldownTicks <= 0) {
            return;
        }
        var cooldownManager = player.getItemCooldownManager();
        if (cooldownManager == null) {
            return;
        }
        var stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            return;
        }
    cooldownManager.set(stack, cooldownTicks);
    }

    private void spawnCollapse(ServerWorld world, Vec3d origin) {
        if (collapseDuration <= 0) {
            return;
        }
        RealityCollapseManager.spawnField(world, origin, radius, collapseDuration);
    }

    public enum Mode {
        PROJECTILE_PURGE,
        WEAPON_COOLDOWN,
        REALITY_COLLAPSE;

        public static Mode fromString(String raw) {
            String normalized = raw.trim().toLowerCase();
            return switch (normalized) {
                case "projectile_purge", "destroy_projectiles" -> PROJECTILE_PURGE;
                case "weapon_cooldown", "cooldown" -> WEAPON_COOLDOWN;
                case "reality_collapse", "collapse" -> REALITY_COLLAPSE;
                default -> throw new IllegalArgumentException("Modo de Final Gambit desconocido: " + raw);
            };
        }
    }
}
