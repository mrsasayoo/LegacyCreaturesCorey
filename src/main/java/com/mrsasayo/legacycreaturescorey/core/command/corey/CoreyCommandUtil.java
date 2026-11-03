package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Small helper collection shared by the different Corey command modules.
 */
public final class CoreyCommandUtil {
    private CoreyCommandUtil() {}

    public static ServerPlayer requirePlayer(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrThrow();
    }

    public static Vec3 resolvePosition(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return Vec3Argument.getVec3(ctx, name);
        } catch (IllegalArgumentException ignored) {
            return ctx.getSource().getPosition();
        }
    }

    public static Mob raycastMob(ServerPlayer player, double distance) {
        Vec3 start = player.getCameraPosVec(1.0F);
        Vec3 direction = player.getRotationVec(1.0F);
        Vec3 end = start.add(direction.multiply(distance));
        AABB searchBox = player.getBoundingBox().stretch(direction.multiply(distance)).expand(1.0D);
        Predicate<Entity> predicate = entity -> entity instanceof Mob mob && mob.isAlive() && !mob.isSpectator();
        EntityHitResult hit = ProjectileUtil.raycast(
            player,
            start,
            end,
            searchBox,
            predicate,
            distance * distance
        );
        if (hit != null && hit.getEntity() instanceof Mob mob) {
            return mob;
        }
        return null;
    }

    public static Mob findNearestMob(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().expand(radius);
        return player.level().getEntitiesByClass(Mob.class, box, Entity::isAlive)
            .stream()
            .min((a, b) -> Double.compare(a.squaredDistanceTo(player), b.squaredDistanceTo(player)))
            .orElse(null);
    }

    public static Mob findMobByUuid(MinecraftServer server, UUID uuid) {
        for (Level world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

}
