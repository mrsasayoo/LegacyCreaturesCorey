package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.action.helper.RealityCollapseManager;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Implements the Final Gambit line of on-death behaviors.
 */
public final class FinalGambitOnDeathAction implements mutation_action {
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
        if (!(entity.level() instanceof ServerLevel world)) {
            return;
        }
    Vec3 origin = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        switch (mode) {
            case PROJECTILE_PURGE -> purgeProjectiles(world, origin);
            case WEAPON_COOLDOWN -> applyWeaponCooldown(killer);
            case REALITY_COLLAPSE -> spawnCollapse(world, origin);
        }
    }

    private void purgeProjectiles(ServerLevel world, Vec3 origin) {
        AABB box = AABB.of(origin, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class, box,
            projectile -> projectile.isAlive() && projectile.distanceToSqr(origin) <= radius * radius);
        for (Projectile projectile : projectiles) {
            projectile.discard();
        }
    }

    private void applyWeaponCooldown(@Nullable LivingEntity killer) {
        if (!(killer instanceof Player player)) {
            return;
        }
        if (cooldownTicks <= 0) {
            return;
        }
        var cooldownManager = player.getCooldowns();
        if (cooldownManager == null) {
            return;
        }
        var stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
    cooldownManager.addCooldown(stack, cooldownTicks);
    }

    private void spawnCollapse(ServerLevel world, Vec3 origin) {
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
