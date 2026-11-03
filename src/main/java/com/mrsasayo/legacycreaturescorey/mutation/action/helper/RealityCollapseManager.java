package com.mrsasayo.legacycreaturescorey.mutation.action.helper;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks "reality collapse" fields that disable block placement/breaking and purge potion effects.
 */
public final class RealityCollapseManager {
    private static final Map<ServerLevel, List<Field>> ACTIVE_FIELDS = new WeakHashMap<>();
    private static final AtomicBoolean CALLBACKS_REGISTERED = new AtomicBoolean(false);
    private static final AtomicLong FIELD_COUNTER = new AtomicLong();

    private RealityCollapseManager() {}

    public static void initializeCallbacks() {
        if (CALLBACKS_REGISTERED.compareAndSet(false, true)) {
            AttackBlockCallback.EVENT.register(RealityCollapseManager::handleAttackBlock);
            UseBlockCallback.EVENT.register(RealityCollapseManager::handleBlockUse);
            PlayerBlockBreakEvents.BEFORE.register(RealityCollapseManager::handleBlockBreak);
        }
    }

    public static void spawnField(ServerLevel world, Vec3 center, double radius, int durationTicks) {
        if (radius <= 0.0D || durationTicks <= 0) {
            return;
        }
        Field field = new Field(FIELD_COUNTER.incrementAndGet(), center, radius, durationTicks);
        ACTIVE_FIELDS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(field);
        logDebug("Campo #%d creado en (%.1f, %.1f, %.1f) radio=%.1f duración=%d ticks", field.id,
            center.x, center.y, center.z, radius, durationTicks);
    }

    public static void tick(ServerLevel world) {
        List<Field> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            return;
        }

        Iterator<Field> iterator = fields.iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            if (field.tick(world)) {
                logDebug("Campo #%d finalizó; ya no bloquea acciones.", field.id);
                iterator.remove();
            }
        }

        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
        }
    }

    private static InteractionResult handleAttackBlock(Player player,
                                                  net.minecraft.world.level.Level world,
                                                  InteractionHand hand,
                                                  BlockPos pos,
                                                  Direction direction) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        Field field = findContainingField(serverWorld, new Vec3(player.getX(), player.getY(), player.getZ()), true);
        if (field != null) {
            logDebug("Ataque a bloque cancelado: %s dentro del campo #%d (radio %.1f, restan %d ticks)",
                player.getName().getString(), field.id, field.radius, field.remainingTicks);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleBlockUse(Player player,
                                               net.minecraft.world.level.Level world,
                                               InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        Field field = findContainingField(serverWorld, hitResult.getBlockPos(), true);
        if (field != null) {
            logDebug("Uso de bloque cancelado en (%.1f, %.1f, %.1f) por campo #%d (radio %.1f, restan %d ticks)",
                hitResult.getBlockPos().getX(), hitResult.getBlockPos().getY(), hitResult.getBlockPos().getZ(), field.id, field.radius, field.remainingTicks);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static boolean handleBlockBreak(net.minecraft.world.level.Level world,
                                            Player player,
                                            BlockPos pos,
                                            net.minecraft.world.level.block.state.BlockState state,
                                            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (world.isClientSide()) {
            return false;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return false;
        }
        if (player == null || player.isSpectator()) {
            return false;
        }
        Field field = findContainingField(serverWorld,
            new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D), true);
        if (field != null) {
            logDebug("Rotura de bloque cancelada para %s en (%d, %d, %d) por campo #%d (radio %.1f, restan %d ticks)",
                player.getName().getString(), pos.getX(), pos.getY(), pos.getZ(), field.id, field.radius, field.remainingTicks);
            return true;
        }
        return false;
    }

    private static Field findContainingField(ServerLevel world, Vec3 position, boolean requireActive) {
        List<Field> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        for (Field field : fields) {
            if (requireActive && field.remainingTicks <= 0) {
                continue;
            }
            if (field.contains(position)) {
                return field;
            }
        }
        return null;
    }

    private static void logDebug(String message, Object... args) {
        String formatted = args.length == 0 ? message : String.format(Locale.ROOT, message, args);
        Legacycreaturescorey.LOGGER.info("[Realidad-Colapso] {}", formatted);
    }

    private static final class Field {
        private final long id;
        private final Vec3 center;
        private final double radius;
        private final double radiusSq;
        private int remainingTicks;

        private Field(long id, Vec3 center, double radius, int durationTicks) {
            this.id = id;
            this.center = center;
            this.radius = radius;
            this.radiusSq = radius * radius;
            this.remainingTicks = durationTicks;
        }

        private boolean tick(ServerLevel world) {
            if (--remainingTicks <= 0) {
                return true;
            }
            List<ServerPlayer> players = world.getPlayers(player -> !player.isSpectator() && player.distanceToSqr(center) <= radiusSq);
            for (ServerPlayer player : players) {
                purgeStatusEffects(player);
            }
            return false;
        }

        private void purgeStatusEffects(ServerPlayer player) {
            Set<MobEffectInstance> effects = Set.copyOf(player.getActiveEffects());
            for (MobEffectInstance instance : effects) {
                player.removeEffect(instance.getEffect());
            }
        }

        private boolean contains(Vec3 pos) {
            return pos.distanceToSqr(center) <= radiusSq;
        }
    }
}
