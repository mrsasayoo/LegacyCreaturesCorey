package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class InterferenceHandler {
    public static final InterferenceHandler INSTANCE = new InterferenceHandler();

    private final Map<ServerWorld, List<ActiveAura>> activeAuras = new WeakHashMap<>();
    private final Map<ServerWorld, List<BlockPos>> pendingBlockBreaks = new WeakHashMap<>();
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

    private ActionResult handleBlockUse(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        List<ActiveAura> auras = getAurasFor(serverWorld, InterferenceSource.Mode.BLOCK_SABOTAGE);
        if (auras.isEmpty()) {
            return ActionResult.PASS;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();
        // The block that would be placed resides on the hit side.
        targetPos = targetPos.offset(side);

        if (shouldSabotagePlacement(serverWorld, player, targetPos, auras)) {
            queueBlockBreak(serverWorld, targetPos);
        }
        return ActionResult.PASS;
    }

    private ActionResult handleItemUse(PlayerEntity player, World world, Hand hand) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return ActionResult.PASS;
        }

        if (!isConsumable(stack)) {
            return ActionResult.PASS;
        }

        List<ActiveAura> auras = getAurasFor(serverWorld, InterferenceSource.Mode.CONSUMPTION_INTERRUPT);
        if (auras.isEmpty()) {
            return ActionResult.PASS;
        }

        if (shouldInterruptConsumption(serverWorld, player, auras)) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.contains(DataComponentTypes.FOOD) || stack.contains(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS)) {
            return true;
        }
        if (stack.contains(DataComponentTypes.POTION_CONTENTS)) {
            return true;
        }
        return stack.isOf(Items.MILK_BUCKET) || stack.isOf(Items.HONEY_BOTTLE);
    }

    private void handleWorldTick(ServerWorld world) {
        cleanup(world);
        processPendingBreaks(world);
        processPearls(world);
    }

    public void register(LivingEntity entity, InterferenceSource source) {
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = activeAuras.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getTime();
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
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = activeAuras.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.entity() == entity && aura.source() == source);
        if (list.isEmpty()) {
            activeAuras.remove(world);
        }
    }

    private List<ActiveAura> getAurasFor(ServerWorld world, InterferenceSource.Mode mode) {
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

    private boolean shouldSabotagePlacement(ServerWorld world, PlayerEntity player, BlockPos pos,
            List<ActiveAura> auras) {
        Vec3d targetCenter = Vec3d.ofCenter(pos);
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

    private void queueBlockBreak(ServerWorld world, BlockPos pos) {
        pendingBlockBreaks.computeIfAbsent(world, ignored -> new ArrayList<>()).add(pos.toImmutable());
    }

    private boolean shouldInterruptConsumption(ServerWorld world, PlayerEntity player, List<ActiveAura> auras) {
        Vec3d position = new Vec3d(player.getX(), player.getY(), player.getZ());
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

    private void processPendingBreaks(ServerWorld world) {
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

    private void processPearls(ServerWorld world) {
        List<ActiveAura> list = getAurasFor(world, InterferenceSource.Mode.PEARL_NEGATION);
        if (list.isEmpty()) {
            return;
        }
        List<EnderPearlEntity> handled = new ArrayList<>();

        for (ActiveAura aura : list) {
            LivingEntity sourceEntity = aura.entity();
            if (!sourceEntity.isAlive()) {
                continue;
            }
            double radius = aura.source().getRadius();
            Box box = sourceEntity.getBoundingBox().expand(radius);
            List<EnderPearlEntity> pearls = world.getEntitiesByClass(EnderPearlEntity.class, box,
                    pearl -> pearl.isAlive() && pearl.getOwner() instanceof PlayerEntity);
            if (pearls.isEmpty()) {
                continue;
            }
            for (EnderPearlEntity pearl : pearls) {
                if (handled.contains(pearl)) {
                    continue;
                }
                Vec3d pos = new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ());
                if (sourceEntity.squaredDistanceTo(pos) > radius * radius) {
                    continue;
                }
                if (aura.source().getChance() < 1.0D
                        && sourceEntity.getRandom().nextDouble() >= aura.source().getChance()) {
                    continue;
                }
                PlayerEntity owner = (PlayerEntity) pearl.getOwner();
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
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.HOSTILE,
                            0.4F,
                            1.1F + world.random.nextFloat() * 0.2F);
                }
                if (owner != null) {
                    owner.damage(world, world.getDamageSources().magic(), aura.source().getPearlDamage());
                }
                handled.add(pearl);
            }
        }
    }

    private void cleanup(ServerWorld world) {
        List<ActiveAura> list = activeAuras.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getTime();
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
