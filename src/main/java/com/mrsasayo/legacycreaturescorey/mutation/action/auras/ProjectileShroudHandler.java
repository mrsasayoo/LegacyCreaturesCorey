package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mixin.accessor.persistent_projectile_entity_accessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ProjectileShroudHandler {
    public static final ProjectileShroudHandler INSTANCE = new ProjectileShroudHandler();

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerLevel, Set<AbstractArrow>> processedProjectiles = new WeakHashMap<>();
    private boolean initialized;

    private ProjectileShroudHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
    }

    public void register(LivingEntity entity, ProjectileShroudSource source) {
        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, ProjectileShroudSource source) {
        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private void handleWorldTick(ServerLevel world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }

        Set<AbstractArrow> processed = processedProjectiles.computeIfAbsent(world,
                ignored -> Collections.newSetFromMap(new WeakHashMap<>()));

        for (ActiveAura aura : list) {
            if (!aura.source.isAlive()) {
                continue;
            }
            handleProjectiles(world, aura, processed);
        }

        processed.removeIf(projectile -> projectile == null || !projectile.isAlive());
    }

    private void handleProjectiles(ServerLevel world, ActiveAura aura, Set<AbstractArrow> processed) {
        LivingEntity entity = aura.source;
        double radius = aura.sourceDef.getRadius();
        double chance = aura.sourceDef.getChance();

        List<AbstractArrow> projectiles = world.getEntitiesByClass(
                AbstractArrow.class,
                entity.getLocalBoundsForPose().expand(radius),
                projectile -> projectile.isAlive() && !projectile.isOnGround());

        for (AbstractArrow projectile : projectiles) {
            if (entity.squaredDistanceTo(projectile) > radius * radius) {
                continue;
            }
            if (chance < 1.0D && entity.getRandom().nextDouble() >= chance) {
                continue;
            }
            if (processed.contains(projectile)) {
                continue;
            }

            switch (aura.sourceDef.getMode()) {
                case DESTROY -> discardProjectile(world, projectile, aura.sourceDef);
                case DEFLECT -> {
                    double reflectFactor = Math.max(0.0D, aura.sourceDef.getReflectDamageFactor());
                    projectile.setTarget(entity);
                    double baseDamage = getProjectileDamage(projectile);
                    projectile.setBaseDamage(Math.max(0.0D, baseDamage * reflectFactor));
                    projectile.setVelocity(projectile.getVelocity().multiply(-Math.max(0.2D, reflectFactor)));
                    projectile.velocityDirty = true;
                    projectile.velocityModified = true;
                    processed.add(projectile);
                }
                case SHOVE_SHOOTER -> {
                    if (projectile.getOwner() instanceof LivingEntity shooter) {
                        double dx = shooter.getX() - entity.getX();
                        double dz = shooter.getZ() - entity.getZ();
                        double length = Math.max(0.0001D, Math.hypot(dx, dz));
                        double pushStrength = aura.sourceDef.getPushStrength();
                        shooter.addVelocity(dx / length * pushStrength, 0.25D, dz / length * pushStrength);
                        shooter.velocityModified = true;
                    }
                    discardProjectile(world, projectile, aura.sourceDef);
                }
            }
        }
    }

    private void discardProjectile(ServerLevel world, AbstractArrow projectile, ProjectileShroudSource source) {
        if (source.shouldDropDestroyedProjectiles()) {
            spawnRecoveredItem(world, projectile);
        }
        projectile.discard();
    }

    private void spawnRecoveredItem(ServerLevel world, AbstractArrow projectile) {
        ItemStack stack = getProjectileStack(projectile);
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(world, projectile.getX(), projectile.getY(), projectile.getZ(), stack);
        drop.setPickUpDelay(5);
        world.spawnEntity(drop);
    }

    private ItemStack getProjectileStack(AbstractArrow projectile) {
        if (projectile instanceof persistent_projectile_entity_accessor accessor) {
            ItemStack stack = accessor.legacycreaturescorey$invokeAsItemStack();
            if (!stack.isEmpty()) {
                return stack.copyFrom();
            }
        }
        return ItemStack.EMPTY;
    }

    private double getProjectileDamage(AbstractArrow projectile) {
        if (projectile instanceof persistent_projectile_entity_accessor accessor) {
            return accessor.legacycreaturescorey$getBaseDamage();
        }
        return 0.0D;
    }

    private void cleanup(ServerLevel world) {
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getTime();
        Iterator<ActiveAura> iterator = list.iterator();
        while (iterator.hasNext()) {
            ActiveAura aura = iterator.next();
            if (!aura.source.isAlive() || time - aura.lastSeenTick > 20L) {
                iterator.remove();
            }
        }
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final ProjectileShroudSource sourceDef;
        private long lastSeenTick;

        private ActiveAura(LivingEntity source, ProjectileShroudSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = currentTick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }
}
