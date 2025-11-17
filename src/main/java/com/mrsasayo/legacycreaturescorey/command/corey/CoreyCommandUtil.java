package com.mrsasayo.legacycreaturescorey.command.corey;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Small helper collection shared by the different Corey command modules.
 */
public final class CoreyCommandUtil {
    private CoreyCommandUtil() {}

    public static ServerPlayerEntity requirePlayer(ServerCommandSource source) throws CommandSyntaxException {
        return source.getPlayerOrThrow();
    }

    public static Vec3d resolvePosition(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return Vec3ArgumentType.getVec3(ctx, name);
        } catch (IllegalArgumentException ignored) {
            return ctx.getSource().getPosition();
        }
    }

    public static MobEntity raycastMob(ServerPlayerEntity player, double distance) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(distance));
        Box searchBox = player.getBoundingBox().stretch(direction.multiply(distance)).expand(1.0D);
        Predicate<Entity> predicate = entity -> entity instanceof MobEntity mob && mob.isAlive() && !mob.isSpectator();
        EntityHitResult hit = ProjectileUtil.raycast(
            player,
            start,
            end,
            searchBox,
            predicate,
            distance * distance
        );
        if (hit != null && hit.getEntity() instanceof MobEntity mob) {
            return mob;
        }
        return null;
    }

    public static MobEntity findNearestMob(ServerPlayerEntity player, double radius) {
        Box box = player.getBoundingBox().expand(radius);
        return player.getEntityWorld().getEntitiesByClass(MobEntity.class, box, Entity::isAlive)
            .stream()
            .min((a, b) -> Double.compare(a.squaredDistanceTo(player), b.squaredDistanceTo(player)))
            .orElse(null);
    }

    public static MobEntity findMobByUuid(MinecraftServer server, UUID uuid) {
        for (World world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof MobEntity mob) {
                return mob;
            }
        }
        return null;
    }

}
