package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class StasisHandler {
    public static final StasisHandler INSTANCE = new StasisHandler();

    private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerWorld, Set<ProjectileEntity>> dampenedProjectiles = new WeakHashMap<>();
    private final Map<ServerPlayerEntity, Map<StasisSource, Long>> attackSpeedTracker = new WeakHashMap<>();
    private boolean initialized;

    private StasisHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        UseItemCallback.EVENT.register(this::handleItemUse);
    }

    public void register(LivingEntity entity, StasisSource source) {
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getTime();
        for (int i = 0; i < list.size(); i++) {
            ActiveAura aura = list.get(i);
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, StasisSource source) {
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private void handleWorldTick(ServerWorld world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            cleanupAttackSpeed(world.getTime());
            return;
        }

        processProjectileDampen(world, list);
        processSprintSuppression(world, list);
        processTeleportAnchor(world, list);
        cleanupAttackSpeed(world.getTime());
    }

    private ActionResult handleItemUse(PlayerEntity player, World world, Hand hand) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        List<ActiveAura> list = active.get(serverWorld);
        if (list == null || list.isEmpty()) {
            return ActionResult.PASS;
        }
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        boolean teleportCancelled = false;
        if (isTeleportItem(player)) {
            for (ActiveAura aura : list) {
                if (aura.sourceDef.getMode() != StasisSource.Mode.TELEPORT_ANCHOR) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.sourceDef.getRadius();
                if (source.squaredDistanceTo(pos) <= radius * radius) {
                    teleportCancelled = true;
                    break;
                }
            }
            if (teleportCancelled) {
                return ActionResult.FAIL;
            }
        }

        boolean handledShield = false;
        if (player.getStackInHand(hand).isOf(Items.SHIELD)) {
            for (ActiveAura aura : list) {
                if (aura.sourceDef.getShieldCooldownTicks() <= 0
                        || aura.sourceDef.getMode() != StasisSource.Mode.SPRINT_SUPPRESSION) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.sourceDef.getRadius();
                if (source.squaredDistanceTo(pos) <= radius * radius) {
                    ItemStack shieldStack = new ItemStack(player.getStackInHand(hand).getItem());
                    player.getItemCooldownManager().set(shieldStack, aura.sourceDef.getShieldCooldownTicks());
                    handledShield = true;
                    break;
                }
            }
        }

        return handledShield ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private boolean isTeleportItem(PlayerEntity player) {
        Entity entity = player.getVehicle();
        // Ignore mounted players to avoid dismount glitches.
        if (entity != null) {
            return false;
        }
        var stack = player.getMainHandStack();
        if (stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.CHORUS_FRUIT)) {
            return true;
        }
        stack = player.getOffHandStack();
        return stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.CHORUS_FRUIT);
    }

    private void processProjectileDampen(ServerWorld world, List<ActiveAura> list) {
        Set<ProjectileEntity> dampened = dampenedProjectiles.computeIfAbsent(world,
                ignored -> Collections.newSetFromMap(new WeakHashMap<>()));
        for (ActiveAura aura : list) {
            if (aura.sourceDef.getMode() != StasisSource.Mode.PROJECTILE_DAMPEN) {
                continue;
            }
            LivingEntity source = aura.source;
            if (!source.isAlive()) {
                continue;
            }
            double radius = aura.sourceDef.getRadius();
            Box search = source.getBoundingBox().expand(radius);
            List<ProjectileEntity> projectiles = world.getEntitiesByClass(ProjectileEntity.class, search,
                    projectile -> shouldSlow(projectile, source, radius));
            if (projectiles.isEmpty()) {
                continue;
            }
            for (ProjectileEntity projectile : projectiles) {
                if (dampened.contains(projectile)) {
                    continue;
                }
                Vec3d velocity = projectile.getVelocity();
                Vec3d slowed = velocity.multiply(aura.sourceDef.getProjectileSlowFactor());
                projectile.setVelocity(slowed);
                projectile.velocityDirty = true;
                projectile.velocityModified = true;
                dampened.add(projectile);
            }
        }
    }

    private boolean shouldSlow(ProjectileEntity projectile, LivingEntity source, double radius) {
        if (!projectile.isAlive()) {
            return false;
        }
        if (projectile instanceof EnderPearlEntity) {
            return false;
        }
        if (!(projectile instanceof PersistentProjectileEntity) && projectile.getType() != EntityType.SNOWBALL) {
            return false;
        }
        Entity owner = projectile.getOwner();
        if (!(owner instanceof PlayerEntity)) {
            return false;
        }
        return source.squaredDistanceTo(projectile) <= radius * radius;
    }

    private void processSprintSuppression(ServerWorld world, List<ActiveAura> list) {
        for (ActiveAura aura : list) {
            if (aura.sourceDef.getMode() != StasisSource.Mode.SPRINT_SUPPRESSION) {
                continue;
            }
            LivingEntity source = aura.source;
            if (!source.isAlive()) {
                continue;
            }
            double radius = aura.sourceDef.getRadius();
            List<PlayerEntity> players = new ArrayList<>(world
                    .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radius * radius));
            long time = world.getTime();
            for (PlayerEntity player : players) {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    applyAttackSpeed(serverPlayer, aura.sourceDef, time);
                }
            }
        }
    }

    private void processTeleportAnchor(ServerWorld world, List<ActiveAura> list) {
        for (ActiveAura aura : list) {
            if (aura.sourceDef.getMode() != StasisSource.Mode.TELEPORT_ANCHOR
                    || aura.sourceDef.getAttackSpeedModifier() == null) {
                continue;
            }
            LivingEntity source = aura.source;
            if (!source.isAlive()) {
                continue;
            }
            double radius = aura.sourceDef.getRadius();
            double radiusSq = radius * radius;
            long time = world.getTime();
            for (ServerPlayerEntity player : world
                    .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq)) {
                applyAttackSpeed(player, aura.sourceDef, time);
            }
        }
    }

    private void applyAttackSpeed(ServerPlayerEntity player, StasisSource source, long time) {
        EntityAttributeModifier modifier = source.getAttackSpeedModifier();
        if (modifier == null) {
            return;
        }
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (instance == null) {
            return;
        }
        Identifier modifierId = source.getAttackSpeedModifierId();
        if (modifierId == null) {
            return;
        }
        if (!instance.hasModifier(modifierId)) {
            instance.addPersistentModifier(modifier);
        }
        attackSpeedTracker.computeIfAbsent(player, ignored -> new HashMap<>()).put(source, time);
    }

    private void cleanupAttackSpeed(long time) {
        Iterator<Map.Entry<ServerPlayerEntity, Map<StasisSource, Long>>> playerIterator = attackSpeedTracker.entrySet()
                .iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<ServerPlayerEntity, Map<StasisSource, Long>> entry = playerIterator.next();
            ServerPlayerEntity player = entry.getKey();
            Map<StasisSource, Long> state = entry.getValue();
            if (player == null || !player.isAlive()) {
                removeAllModifiers(player, new HashSet<>(state.keySet()));
                playerIterator.remove();
                continue;
            }
            Iterator<Map.Entry<StasisSource, Long>> auraIterator = state.entrySet().iterator();
            while (auraIterator.hasNext()) {
                Map.Entry<StasisSource, Long> auraEntry = auraIterator.next();
                if (time - auraEntry.getValue() > 5L) {
                    removeModifier(player, auraEntry.getKey());
                    auraIterator.remove();
                }
            }
            if (state.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private void removeAllModifiers(ServerPlayerEntity player, Set<StasisSource> sources) {
        if (player == null || sources == null) {
            return;
        }
        for (StasisSource source : sources) {
            removeModifier(player, source);
        }
    }

    private void removeModifier(ServerPlayerEntity player, StasisSource source) {
        Identifier modifierId = source.getAttackSpeedModifierId();
        if (modifierId == null) {
            return;
        }
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (instance == null) {
            return;
        }
        if (instance.hasModifier(modifierId)) {
            instance.removeModifier(modifierId);
        }
    }

    private void cleanup(ServerWorld world) {
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
        Set<ProjectileEntity> dampened = dampenedProjectiles.get(world);
        if (dampened != null) {
            dampened.removeIf(projectile -> projectile == null || !projectile.isAlive());
            if (dampened.isEmpty()) {
                dampenedProjectiles.remove(world);
            }
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final StasisSource sourceDef;
        private long lastSeenTick;

        private ActiveAura(LivingEntity source, StasisSource sourceDef, long tick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = tick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }
}
