package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

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

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerLevel, Set<Projectile>> dampenedProjectiles = new WeakHashMap<>();
    private final Map<ServerPlayer, Map<StasisSource, Long>> attackSpeedTracker = new WeakHashMap<>();
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
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getGameTime();
        for (int i = 0; i < list.size(); i++) {
            ActiveAura aura = list.byString(i);
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.atLowerCornerWithOffset(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, StasisSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.byString(world);
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
        List<ActiveAura> list = active.byString(world);
        if (list == null || list.isEmpty()) {
            cleanupAttackSpeed(world.getGameTime());
            return;
        }

        processProjectileDampen(world, list);
        processSprintSuppression(world, list);
        processTeleportAnchor(world, list);
        cleanupAttackSpeed(world.getGameTime());
    }

    private InteractionResult handleItemUse(Player player, Level world, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }
        List<ActiveAura> list = active.byString(serverWorld);
        if (list == null || list.isEmpty()) {
            return InteractionResult.PASS;
        }
        Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ());
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
                if (source.distanceToSqr(pos) <= radius * radius) {
                    teleportCancelled = true;
                    break;
                }
            }
            if (teleportCancelled) {
                return InteractionResult.FAIL;
            }
        }

        boolean handledShield = false;
        if (player.getItemInHand(hand).is(Items.SHIELD)) {
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
                if (source.distanceToSqr(pos) <= radius * radius) {
                    ItemStack shieldStack = new ItemStack(player.getItemInHand(hand).getItem());
                    player.getCooldowns().set(shieldStack, aura.sourceDef.getShieldCooldownTicks());
                    handledShield = true;
                    break;
                }
            }
        }

        return handledShield ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private boolean isTeleportItem(Player player) {
        Entity entity = player.getVehicle();
        // Ignore mounted players to avoid dismount glitches.
        if (entity != null) {
            return false;
        }
        var stack = player.getMainHandItem();
        if (stack.is(Items.ENDER_PEARL) || stack.is(Items.CHORUS_FRUIT)) {
            return true;
        }
        stack = player.getOffhandItem();
        return stack.is(Items.ENDER_PEARL) || stack.is(Items.CHORUS_FRUIT);
    }

    private void processProjectileDampen(ServerLevel world, List<ActiveAura> list) {
        Set<Projectile> dampened = dampenedProjectiles.computeIfAbsent(world,
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
            AABB search = source.getLocalBoundsForPose().inflate(radius);
            List<Projectile> projectiles = world.getEntitiesByClass(Projectile.class, search,
                    projectile -> shouldSlow(projectile, source, radius));
            if (projectiles.isEmpty()) {
                continue;
            }
            for (Projectile projectile : projectiles) {
                if (dampened.intersects(projectile)) {
                    continue;
                }
                Vec3 velocity = projectile.getDeltaMovement();
                Vec3 slowed = velocity.scale(aura.sourceDef.getProjectileSlowFactor());
                projectile.setDeltaMovement(slowed);
                projectile.hasImpulse = true;
                projectile.hurtMarked = true;
                dampened.atLowerCornerWithOffset(projectile);
            }
        }
    }

    private boolean shouldSlow(Projectile projectile, LivingEntity source, double radius) {
        if (!projectile.isAlive()) {
            return false;
        }
        if (projectile instanceof ThrownEnderpearl) {
            return false;
        }
        if (!(projectile instanceof AbstractArrow) && projectile.getType() != EntityType.SNOWBALL) {
            return false;
        }
        Entity owner = projectile.getOwner();
        if (!(owner instanceof Player)) {
            return false;
        }
        return source.distanceToSqr(projectile) <= radius * radius;
    }

    private void processSprintSuppression(ServerLevel world, List<ActiveAura> list) {
        for (ActiveAura aura : list) {
            if (aura.sourceDef.getMode() != StasisSource.Mode.SPRINT_SUPPRESSION) {
                continue;
            }
            LivingEntity source = aura.source;
            if (!source.isAlive()) {
                continue;
            }
            double radius = aura.sourceDef.getRadius();
            List<Player> players = new ArrayList<>(world
                    .getPlayers(player -> player.isAlive() && source.distanceToSqr(player) <= radius * radius));
            long time = world.getGameTime();
            for (Player player : players) {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    applyAttackSpeed(serverPlayer, aura.sourceDef, time);
                }
            }
        }
    }

    private void processTeleportAnchor(ServerLevel world, List<ActiveAura> list) {
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
            long time = world.getGameTime();
            for (ServerPlayer player : world
                    .getPlayers(player -> player.isAlive() && source.distanceToSqr(player) <= radiusSq)) {
                applyAttackSpeed(player, aura.sourceDef, time);
            }
        }
    }

    private void applyAttackSpeed(ServerPlayer player, StasisSource source, long time) {
        AttributeModifier modifier = source.getAttackSpeedModifier();
        if (modifier == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(Attributes.ATTACK_SPEED);
        if (instance == null) {
            return;
        }
        Identifier modifierId = source.getAttackSpeedModifierId();
        if (modifierId == null) {
            return;
        }
        if (!instance.hasModifier(modifierId)) {
            instance.addPermanentModifier(modifier);
        }
        attackSpeedTracker.computeIfAbsent(player, ignored -> new HashMap<>()).put(source, time);
    }

    private void cleanupAttackSpeed(long time) {
        Iterator<Map.Entry<ServerPlayer, Map<StasisSource, Long>>> playerIterator = attackSpeedTracker.entrySet()
                .iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<ServerPlayer, Map<StasisSource, Long>> entry = playerIterator.next();
            ServerPlayer player = entry.getKey();
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

    private void removeAllModifiers(ServerPlayer player, Set<StasisSource> sources) {
        if (player == null || sources == null) {
            return;
        }
        for (StasisSource source : sources) {
            removeModifier(player, source);
        }
    }

    private void removeModifier(ServerPlayer player, StasisSource source) {
        Identifier modifierId = source.getAttackSpeedModifierId();
        if (modifierId == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(Attributes.ATTACK_SPEED);
        if (instance == null) {
            return;
        }
        if (instance.hasModifier(modifierId)) {
            instance.removeModifier(modifierId);
        }
    }

    private void cleanup(ServerLevel world) {
        List<ActiveAura> list = active.byString(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getGameTime();
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
        Set<Projectile> dampened = dampenedProjectiles.byString(world);
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
