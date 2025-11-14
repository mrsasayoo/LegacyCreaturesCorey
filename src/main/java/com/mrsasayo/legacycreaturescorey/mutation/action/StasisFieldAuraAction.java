package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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

/**
 * Implements the Stasis Field aura family with projectile dampening, sprint suppression and teleport anchoring.
 */
public final class StasisFieldAuraAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final double projectileSlowFactor;
    private final double attackSpeedMultiplier;
    private final int shieldCooldownTicks;
    private final Identifier attackSpeedModifierId;
    private final EntityAttributeModifier attackSpeedModifier;

    public StasisFieldAuraAction(Mode mode, double radius, double projectileSlowFactor) {
        this(mode, radius, projectileSlowFactor, 1.0D, 0);
    }

    public StasisFieldAuraAction(Mode mode, double radius, double projectileSlowFactor, double attackSpeedMultiplier, int shieldCooldownTicks) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.projectileSlowFactor = projectileSlowFactor;
        this.attackSpeedMultiplier = Math.max(0.0D, attackSpeedMultiplier);
        this.shieldCooldownTicks = Math.max(0, shieldCooldownTicks);
        if (this.attackSpeedMultiplier < 0.999D) {
            double amount = this.attackSpeedMultiplier - 1.0D;
            String key = "stasis_field_" + mode.name().toLowerCase() + "_" + Integer.toHexString(Double.hashCode(radius + projectileSlowFactor + this.attackSpeedMultiplier));
            this.attackSpeedModifierId = Identifier.of("legacycreaturescorey", key);
            this.attackSpeedModifier = new EntityAttributeModifier(attackSpeedModifierId, amount, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        } else {
            this.attackSpeedModifierId = null;
            this.attackSpeedModifier = null;
        }
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity, this);
    }

    Mode getMode() {
        return mode;
    }

    double getRadius() {
        return radius;
    }

    double getProjectileSlowFactor() {
        return projectileSlowFactor;
    }

    double getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }

    EntityAttributeModifier getAttackSpeedModifier() {
        return attackSpeedModifier;
    }

    Identifier getAttackSpeedModifierId() {
        return attackSpeedModifierId;
    }

    int getShieldCooldownTicks() {
        return shieldCooldownTicks;
    }

    public enum Mode {
        PROJECTILE_DAMPEN,
        SPRINT_SUPPRESSION,
        TELEPORT_ANCHOR;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "PROJECTILE", "PROJECTILE_DAMPEN" -> PROJECTILE_DAMPEN;
                case "SPRINT", "SPRINT_SUPPRESSION" -> SPRINT_SUPPRESSION;
                case "ANCHOR", "TELEPORT_ANCHOR", "TELEPORT_LOCK" -> TELEPORT_ANCHOR;
                default -> throw new IllegalArgumentException("Modo de stasis desconocido: " + raw);
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final StasisFieldAuraAction action;
        private long lastSeenTick;

        private ActiveAura(LivingEntity source, StasisFieldAuraAction action, long tick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = tick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<ServerWorld, Set<ProjectileEntity>> dampenedProjectiles = new WeakHashMap<>();
    private final Map<ServerPlayerEntity, Map<StasisFieldAuraAction, Long>> attackSpeedTracker = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
            UseItemCallback.EVENT.register(this::handleItemUse);
        }

        void register(LivingEntity entity, StasisFieldAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (int i = 0; i < list.size(); i++) {
                ActiveAura aura = list.get(i);
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, StasisFieldAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
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
                    if (aura.action.mode != Mode.TELEPORT_ANCHOR) {
                        continue;
                    }
                    LivingEntity source = aura.source;
                    if (!source.isAlive()) {
                        continue;
                    }
                    double radius = aura.action.radius;
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
                    if (aura.action.shieldCooldownTicks <= 0 || aura.action.mode != Mode.SPRINT_SUPPRESSION) {
                        continue;
                    }
                    LivingEntity source = aura.source;
                    if (!source.isAlive()) {
                        continue;
                    }
                    double radius = aura.action.radius;
                    if (source.squaredDistanceTo(pos) <= radius * radius) {
                        ItemStack shieldStack = new ItemStack(player.getStackInHand(hand).getItem());
                        player.getItemCooldownManager().set(shieldStack, aura.action.shieldCooldownTicks);
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
            Set<ProjectileEntity> dampened = dampenedProjectiles.computeIfAbsent(world, ignored -> Collections.newSetFromMap(new WeakHashMap<>()));
            for (ActiveAura aura : list) {
                if (aura.action.mode != Mode.PROJECTILE_DAMPEN) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action.radius;
                Box search = source.getBoundingBox().expand(radius);
                List<ProjectileEntity> projectiles = world.getEntitiesByClass(ProjectileEntity.class, search, projectile -> shouldSlow(projectile, source, radius));
                if (projectiles.isEmpty()) {
                    continue;
                }
                for (ProjectileEntity projectile : projectiles) {
                    if (dampened.contains(projectile)) {
                        continue;
                    }
                    Vec3d velocity = projectile.getVelocity();
                    Vec3d slowed = velocity.multiply(aura.action.projectileSlowFactor);
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
                if (aura.action.mode != Mode.SPRINT_SUPPRESSION) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action.radius;
                List<PlayerEntity> players = new ArrayList<>(world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radius * radius));
                long time = world.getTime();
                for (PlayerEntity player : players) {
                    if (player.isSprinting()) {
                        player.setSprinting(false);
                    }
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        applyAttackSpeed(serverPlayer, aura.action, time);
                    }
                }
            }
        }

        private void processTeleportAnchor(ServerWorld world, List<ActiveAura> list) {
            for (ActiveAura aura : list) {
                if (aura.action.mode != Mode.TELEPORT_ANCHOR || aura.action.attackSpeedModifier == null) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action.radius;
                double radiusSq = radius * radius;
                long time = world.getTime();
                for (ServerPlayerEntity player : world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq)) {
                    applyAttackSpeed(player, aura.action, time);
                }
            }
        }

        private void applyAttackSpeed(ServerPlayerEntity player, StasisFieldAuraAction action, long time) {
            EntityAttributeModifier modifier = action.getAttackSpeedModifier();
            if (modifier == null) {
                return;
            }
            EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
            if (instance == null) {
                return;
            }
            Identifier modifierId = action.getAttackSpeedModifierId();
            if (modifierId == null) {
                return;
            }
            if (!instance.hasModifier(modifierId)) {
                instance.addPersistentModifier(modifier);
            }
            attackSpeedTracker.computeIfAbsent(player, ignored -> new HashMap<>()).put(action, time);
        }

        private void cleanupAttackSpeed(long time) {
            Iterator<Map.Entry<ServerPlayerEntity, Map<StasisFieldAuraAction, Long>>> playerIterator = attackSpeedTracker.entrySet().iterator();
            while (playerIterator.hasNext()) {
                Map.Entry<ServerPlayerEntity, Map<StasisFieldAuraAction, Long>> entry = playerIterator.next();
                ServerPlayerEntity player = entry.getKey();
                Map<StasisFieldAuraAction, Long> state = entry.getValue();
                if (player == null || !player.isAlive()) {
                    removeAllModifiers(player, new HashSet<>(state.keySet()));
                    playerIterator.remove();
                    continue;
                }
                Iterator<Map.Entry<StasisFieldAuraAction, Long>> auraIterator = state.entrySet().iterator();
                while (auraIterator.hasNext()) {
                    Map.Entry<StasisFieldAuraAction, Long> auraEntry = auraIterator.next();
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

        private void removeAllModifiers(ServerPlayerEntity player, Set<StasisFieldAuraAction> actions) {
            if (player == null || actions == null) {
                return;
            }
            for (StasisFieldAuraAction action : actions) {
                removeModifier(player, action);
            }
        }

        private void removeModifier(ServerPlayerEntity player, StasisFieldAuraAction action) {
            Identifier modifierId = action.getAttackSpeedModifierId();
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
    }
}
