package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class InterferenceHandler {
    public static final InterferenceHandler INSTANCE = new InterferenceHandler();

    private final Map<ServerLevel, List<ActiveAura>> activeAuras = new WeakHashMap<>();
    private final Map<ServerLevel, List<BlockPos>> pendingBlockBreaks = new WeakHashMap<>();
    private boolean initialized;

    private InterferenceHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseBlockCallback.EVENT.register(this::handleBlockUse);
        UseItemCallback.EVENT.register(this::handleItemUse);
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
    }

    private InteractionResult handleBlockUse(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }
        List<ActiveAura> auras = getAurasFor(serverWorld, InterferenceSource.Mode.BLOCK_SABOTAGE);
        if (auras.isEmpty()) {
            return InteractionResult.PASS;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();
        // The block that would be placed resides on the hit side.
        targetPos = targetPos.offset(side);

        if (shouldSabotagePlacement(serverWorld, player, targetPos, auras)) {
            queueBlockBreak(serverWorld, targetPos);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult handleItemUse(Player player, Level world, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!isConsumable(stack)) {
            return InteractionResult.PASS;
        }

        List<ActiveAura> auras = getAurasFor(serverWorld, InterferenceSource.Mode.CONSUMPTION_INTERRUPT);
        if (auras.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (shouldInterruptConsumption(serverWorld, player, auras)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.contains(DataComponents.FOOD) || stack.contains(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
            return true;
        }
        if (stack.contains(DataComponents.POTION_CONTENTS)) {
            return true;
        }
        return stack.isOf(Items.MILK_BUCKET) || stack.isOf(Items.HONEY_BOTTLE);
    }

    private void handleWorldTick(ServerLevel world) {
        cleanup(world);
        processPendingBreaks(world);
        processPearls(world);
    }

    public void register(LivingEntity entity, InterferenceSource source) {
        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        List<ActiveAura> list = activeAuras.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getGameTime();
        for (int i = 0; i < list.size(); i++) {
            ActiveAura aura = list.get(i);
            if (aura.entity() == entity && aura.source() == source) {
                list.set(i, aura.tick(time));
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, InterferenceSource source) {
        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        List<ActiveAura> list = activeAuras.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.entity() == entity && aura.source() == source);
        if (list.isEmpty()) {
            activeAuras.remove(world);
        }
    }

    private List<ActiveAura> getAurasFor(ServerLevel world, InterferenceSource.Mode mode) {
        List<ActiveAura> list = activeAuras.get(world);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<ActiveAura> filtered = new ArrayList<>();
        for (ActiveAura aura : list) {
            if (aura.source().getMode() == mode) {
                filtered.add(aura);
            }
        }
        return filtered;
    }

    private boolean shouldSabotagePlacement(ServerLevel world, Player player, BlockPos pos,
            List<ActiveAura> auras) {
        Vec3 targetCenter = Vec3.ofCenter(pos);
        for (ActiveAura aura : auras) {
            LivingEntity sourceEntity = aura.entity();
            if (!sourceEntity.isAlive()) {
                continue;
            }
            if (sourceEntity.squaredDistanceTo(targetCenter) > aura.source().getRadius() * aura.source().getRadius()) {
                continue;
            }
            if (aura.source().getChance() < 1.0D
                    && sourceEntity.getRandom().nextDouble() >= aura.source().getChance()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private void queueBlockBreak(ServerLevel world, BlockPos pos) {
        pendingBlockBreaks.computeIfAbsent(world, ignored -> new ArrayList<>()).add(pos.toImmutable());
    }

    private boolean shouldInterruptConsumption(ServerLevel world, Player player, List<ActiveAura> auras) {
        Vec3 position = new Vec3(player.getX(), player.getY(), player.getZ());
        for (ActiveAura aura : auras) {
            LivingEntity sourceEntity = aura.entity();
            if (!sourceEntity.isAlive()) {
                continue;
            }
            if (sourceEntity.squaredDistanceTo(position) > aura.source().getRadius() * aura.source().getRadius()) {
                continue;
            }
            if (aura.source().getChance() < 1.0D
                    && sourceEntity.getRandom().nextDouble() >= aura.source().getChance()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private void processPendingBreaks(ServerLevel world) {
        List<BlockPos> queue = pendingBlockBreaks.get(world);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        Iterator<BlockPos> iterator = queue.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = world.getBlockState(pos);
            if (!state.isAir()) {
                world.breakBlock(pos, true);
            }
            iterator.remove();
        }
        if (queue.isEmpty()) {
            pendingBlockBreaks.remove(world);
        }
    }

    private void processPearls(ServerLevel world) {
        List<ActiveAura> list = getAurasFor(world, InterferenceSource.Mode.PEARL_NEGATION);
        if (list.isEmpty()) {
            return;
        }
        List<ThrownEnderpearl> handled = new ArrayList<>();

        for (ActiveAura aura : list) {
            LivingEntity sourceEntity = aura.entity();
            if (!sourceEntity.isAlive()) {
                continue;
            }
            double radius = aura.source().getRadius();
            AABB box = sourceEntity.getBoundingBox().expand(radius);
            List<ThrownEnderpearl> pearls = world.getEntitiesByClass(ThrownEnderpearl.class, box,
                    pearl -> pearl.isAlive() && pearl.getOwner() instanceof Player);
            if (pearls.isEmpty()) {
                continue;
            }
            for (ThrownEnderpearl pearl : pearls) {
                if (handled.contains(pearl)) {
                    continue;
                }
                Vec3 pos = new Vec3(pearl.getX(), pearl.getY(), pearl.getZ());
                if (sourceEntity.squaredDistanceTo(pos) > radius * radius) {
                    continue;
                }
                if (aura.source().getChance() < 1.0D
                        && sourceEntity.getRandom().nextDouble() >= aura.source().getChance()) {
                    continue;
                }
                Player owner = (Player) pearl.getOwner();
                if (!pearl.isRemoved()) {
                    pearl.remove(Entity.RemovalReason.KILLED);
                    world.spawnParticles(ParticleTypes.SMOKE,
                            pearl.getX(),
                            pearl.getY(),
                            pearl.getZ(),
                            6,
                            0.1D,
                            0.1D,
                            0.1D,
                            0.01D);
                    world.playSound(null,
                            pearl.getX(),
                            pearl.getY(),
                            pearl.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.HOSTILE,
                            0.4F,
                            1.1F + world.random.nextFloat() * 0.2F);
                }
                if (owner != null) {
                    owner.damage(world, world.damageSources().magic(), aura.source().getPearlDamage());
                }
                handled.add(pearl);
            }
        }
    }

    private void cleanup(ServerLevel world) {
        List<ActiveAura> list = activeAuras.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getGameTime();
        list.removeIf(aura -> !aura.entity().isAlive() || time - aura.lastActiveTick() > 20L);
        if (list.isEmpty()) {
            activeAuras.remove(world);
        }
    }

    private record ActiveAura(LivingEntity entity,
            InterferenceSource source,
            long lastActiveTick) {
        ActiveAura tick(long worldTime) {
            return new ActiveAura(entity, source, worldTime);
        }
    }
}
