package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
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

/**
 * Handles the "interference" family of auras that disturb player actions around the caster.
 */
public final class InterferenceAuraAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final double chance;
    private final float pearlDamage;

    public InterferenceAuraAction(Mode mode, double radius, double chance, float pearlDamage) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.chance = Math.min(1.0D, Math.max(0.0D, chance));
        this.pearlDamage = pearlDamage;
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

    double getChance() {
        return chance;
    }

    float getPearlDamage() {
        return pearlDamage;
    }

    public enum Mode {
        BLOCK_SABOTAGE,
        CONSUMPTION_INTERRUPT,
        PEARL_NEGATION;

        public static Mode fromString(String raw) {
            String normalized = raw.trim().toUpperCase();
            return switch (normalized) {
                case "BLOCK", "BLOCK_PLACEMENT", "BLOCK_SABOTAGE" -> BLOCK_SABOTAGE;
                case "CONSUMPTION", "USE_INTERRUPTION", "CONSUMPTION_INTERRUPT" -> CONSUMPTION_INTERRUPT;
                case "PEARL", "ENDER_PEARL", "PEARL_NEGATION" -> PEARL_NEGATION;
                default -> throw new IllegalArgumentException("Modo de interferencia desconocido: " + raw);
            };
        }
    }

    private record ActiveAura(LivingEntity source,
                              InterferenceAuraAction action,
                              long lastActiveTick) {
        ActiveAura tick(long worldTime) {
            return new ActiveAura(source, action, worldTime);
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerWorld, List<ActiveAura>> activeAuras = new WeakHashMap<>();
        private final Map<ServerWorld, List<BlockPos>> pendingBlockBreaks = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
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
            List<ActiveAura> auras = getAurasFor(serverWorld, Mode.BLOCK_SABOTAGE);
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

            List<ActiveAura> auras = getAurasFor(serverWorld, Mode.CONSUMPTION_INTERRUPT);
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

        void register(LivingEntity entity, InterferenceAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = activeAuras.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (int i = 0; i < list.size(); i++) {
                ActiveAura aura = list.get(i);
                if (aura.source() == entity && aura.action() == action) {
                    list.set(i, aura.tick(time));
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, InterferenceAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = activeAuras.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source() == entity && aura.action() == action);
            if (list.isEmpty()) {
                activeAuras.remove(world);
            }
        }

        private List<ActiveAura> getAurasFor(ServerWorld world, Mode mode) {
            List<ActiveAura> list = activeAuras.get(world);
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            List<ActiveAura> filtered = new ArrayList<>();
            for (ActiveAura aura : list) {
                if (aura.action().getMode() == mode) {
                    filtered.add(aura);
                }
            }
            return filtered;
        }

        private boolean shouldSabotagePlacement(ServerWorld world, PlayerEntity player, BlockPos pos, List<ActiveAura> auras) {
            Vec3d targetCenter = Vec3d.ofCenter(pos);
            for (ActiveAura aura : auras) {
                LivingEntity source = aura.source();
                if (!source.isAlive()) {
                    continue;
                }
                if (source.squaredDistanceTo(targetCenter) > aura.action().getRadius() * aura.action().getRadius()) {
                    continue;
                }
                if (aura.action().getChance() < 1.0D && source.getRandom().nextDouble() >= aura.action().getChance()) {
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
                LivingEntity source = aura.source();
                if (!source.isAlive()) {
                    continue;
                }
                if (source.squaredDistanceTo(position) > aura.action().getRadius() * aura.action().getRadius()) {
                    continue;
                }
                if (aura.action().getChance() < 1.0D && source.getRandom().nextDouble() >= aura.action().getChance()) {
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
            List<ActiveAura> list = getAurasFor(world, Mode.PEARL_NEGATION);
            if (list.isEmpty()) {
                return;
            }
            List<EnderPearlEntity> handled = new ArrayList<>();

            for (ActiveAura aura : list) {
                LivingEntity source = aura.source();
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action().getRadius();
                Box box = source.getBoundingBox().expand(radius);
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
                    if (source.squaredDistanceTo(pos) > radius * radius) {
                        continue;
                    }
                    if (aura.action().getChance() < 1.0D && source.getRandom().nextDouble() >= aura.action().getChance()) {
                        continue;
                    }
                    PlayerEntity owner = (PlayerEntity) pearl.getOwner();
                    pearl.discard();
                    if (owner != null) {
                        owner.damage(world, world.getDamageSources().magic(), aura.action().getPearlDamage());
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
            list.removeIf(aura -> !aura.source().isAlive() || time - aura.lastActiveTick() > 20L);
            if (list.isEmpty()) {
                activeAuras.remove(world);
            }
        }
    }
}
