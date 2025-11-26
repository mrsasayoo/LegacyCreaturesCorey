package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

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
    private static final Map<ServerWorld, List<Field>> ACTIVE_FIELDS = new WeakHashMap<>();
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

    public static void spawnField(ServerWorld world, Vec3d center, double radius, int durationTicks) {
        if (radius <= 0.0D || durationTicks <= 0) {
            return;
        }
        Field field = new Field(FIELD_COUNTER.incrementAndGet(), center, radius, durationTicks);
        ACTIVE_FIELDS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(field);
        logDebug("Campo #%d creado en (%.1f, %.1f, %.1f) radio=%.1f duración=%d ticks", field.id,
            center.x, center.y, center.z, radius, durationTicks);
    }

    public static void tick(ServerWorld world) {
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

    private static ActionResult handleAttackBlock(PlayerEntity player,
                                                  net.minecraft.world.World world,
                                                  Hand hand,
                                                  BlockPos pos,
                                                  Direction direction) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (player == null || player.isSpectator()) {
            return ActionResult.PASS;
        }
        Field field = findContainingField(serverWorld, new Vec3d(player.getX(), player.getY(), player.getZ()), true);
        if (field != null) {
            logDebug("Ataque a bloque cancelado: %s dentro del campo #%d (radio %.1f, restan %d ticks)",
                player.getName().getString(), field.id, field.radius, field.remainingTicks);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private static ActionResult handleBlockUse(PlayerEntity player,
                                               net.minecraft.world.World world,
                                               Hand hand,
                                               BlockHitResult hitResult) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (player == null || player.isSpectator()) {
            return ActionResult.PASS;
        }
        Field field = findContainingField(serverWorld, hitResult.getPos(), true);
        if (field != null) {
            logDebug("Uso de bloque cancelado en (%.1f, %.1f, %.1f) por campo #%d (radio %.1f, restan %d ticks)",
                hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z, field.id, field.radius, field.remainingTicks);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private static boolean handleBlockBreak(net.minecraft.world.World world,
                                            PlayerEntity player,
                                            BlockPos pos,
                                            net.minecraft.block.BlockState state,
                                            net.minecraft.block.entity.BlockEntity blockEntity) {
        if (world.isClient()) {
            return false;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        if (player == null || player.isSpectator()) {
            return false;
        }
        Field field = findContainingField(serverWorld,
            new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D), true);
        if (field != null) {
            logDebug("Rotura de bloque cancelada para %s en (%d, %d, %d) por campo #%d (radio %.1f, restan %d ticks)",
                player.getName().getString(), pos.getX(), pos.getY(), pos.getZ(), field.id, field.radius, field.remainingTicks);
            return true;
        }
        return false;
    }

    private static Field findContainingField(ServerWorld world, Vec3d position, boolean requireActive) {
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
        private final Vec3d center;
        private final double radius;
        private final double radiusSq;
        private int remainingTicks;

        private Field(long id, Vec3d center, double radius, int durationTicks) {
            this.id = id;
            this.center = center;
            this.radius = radius;
            this.radiusSq = radius * radius;
            this.remainingTicks = durationTicks;
        }

        private boolean tick(ServerWorld world) {
            if (--remainingTicks <= 0) {
                return true;
            }
            List<ServerPlayerEntity> players = world.getPlayers(player -> !player.isSpectator() && player.squaredDistanceTo(center) <= radiusSq);
            for (ServerPlayerEntity player : players) {
                purgeStatusEffects(player);
            }
            return false;
        }

        private void purgeStatusEffects(ServerPlayerEntity player) {
            Set<StatusEffectInstance> effects = Set.copyOf(player.getStatusEffects());
            for (StatusEffectInstance instance : effects) {
                player.removeStatusEffect(instance.getEffectType());
            }
        }

        private boolean contains(Vec3d pos) {
            return pos.squaredDistanceTo(center) <= radiusSq;
        }
    }
}
