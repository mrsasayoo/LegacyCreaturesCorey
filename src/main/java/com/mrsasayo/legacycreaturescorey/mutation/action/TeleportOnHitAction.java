package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

public final class TeleportOnHitAction extends ProcOnHitAction {
    private final double radius;
    private final List<StatusEffectOnHitAction.AdditionalEffect> sideEffects;
    private final Target target;

    public TeleportOnHitAction(double chance,
                               double radius,
                               Target target,
                               List<StatusEffectOnHitAction.AdditionalEffect> sideEffects) {
        super(chance);
        this.radius = Math.max(0.5D, radius);
        this.target = target;
        this.sideEffects = sideEffects == null ? List.of() : List.copyOf(sideEffects);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        LivingEntity teleportee = target == Target.SELF ? attacker : victim;
        if (!(teleportee.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Vec3d origin = new Vec3d(teleportee.getX(), teleportee.getY(), teleportee.getZ());
        Vec3d destination = tryTeleportRandomly(world, teleportee, radius);
        if (destination != null) {
            playTeleportFeedback(world, teleportee, origin, destination);
            if (!sideEffects.isEmpty()) {
                for (StatusEffectOnHitAction.AdditionalEffect effect : sideEffects) {
                    effect.apply(attacker, victim);
                }
            }
        }
    }

    private Vec3d tryTeleportRandomly(ServerWorld world, LivingEntity entity, double range) {
        Random random = entity.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            double dx = (random.nextDouble() * 2.0D - 1.0D) * range;
            double dz = (random.nextDouble() * 2.0D - 1.0D) * range;
            BlockPos target = BlockPos.ofFloored(entity.getX() + dx, entity.getY(), entity.getZ() + dz);
            if (!world.isSpaceEmpty(entity, entity.getBoundingBox().offset(dx, 0.0D, dz))) {
                continue;
            }

            Vec3d destination = new Vec3d(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            entity.refreshPositionAfterTeleport(destination);
            entity.setHeadYaw(entity.getYaw());
            entity.velocityModified = true;
            return destination;
        }
        return null;
    }

    private void playTeleportFeedback(ServerWorld world, LivingEntity entity, Vec3d origin, Vec3d destination) {
        float pitch = 0.85F + world.getRandom().nextFloat() * 0.2F;
        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.7F, pitch);
        world.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.7F, pitch + 0.15F);
        double eyeHeight = entity.getStandingEyeHeight() * 0.5D;
        world.spawnParticles(ParticleTypes.PORTAL, origin.x, origin.y + eyeHeight, origin.z, 16, 0.5D, 0.5D, 0.5D, 0.3D);
        world.spawnParticles(ParticleTypes.PORTAL, destination.x, destination.y + eyeHeight, destination.z, 16, 0.5D, 0.5D, 0.5D, 0.3D);
    }

    public enum Target {
        SELF,
        OTHER;

        public static Target fromString(String raw) {
            if (raw == null) {
                return OTHER;
            }
            return switch (raw.trim().toUpperCase()) {
                case "SELF", "ATTACKER" -> SELF;
                case "OTHER", "VICTIM", "TARGET" -> OTHER;
                default -> throw new IllegalArgumentException("Objetivo de teletransporte desconocido: " + raw);
            };
        }
    }
}
