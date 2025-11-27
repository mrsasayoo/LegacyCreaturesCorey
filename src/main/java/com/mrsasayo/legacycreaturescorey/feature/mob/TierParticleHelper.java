package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Utilidad para mantener consistentes las partículas asociadas a los tiers.
 */
final class TierParticleHelper {
    private TierParticleHelper() {}

    static void spawnInitialBurst(ServerWorld world, MobEntity mob, MobTier tier) {
        ParticleEffect effect = resolveEffect(tier);
        if (effect != null) {
            spawnParticles(world, mob, effect, initialCountFor(tier), 0.01D);
        }
    }

    static void spawnAmbient(ServerWorld world, MobEntity mob, MobTier tier) {
        ParticleEffect effect = resolveEffect(tier);
        if (effect != null) {
            spawnParticles(world, mob, effect, ambientCountFor(tier), 0.004D);
        }
    }

    private static ParticleEffect resolveEffect(MobTier tier) {
        return switch (tier) {
            case EPIC -> ParticleTypes.SOUL;
            case LEGENDARY -> ParticleTypes.FALLING_DRIPSTONE_LAVA;
            case MYTHIC -> DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 0.5F);
            case DEFINITIVE -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> null;
        };
    }

    private static int initialCountFor(MobTier tier) {
        return switch (tier) {
            case EPIC -> 12;
            case LEGENDARY -> 32;
            case MYTHIC -> 28;
            case DEFINITIVE -> 36;
            default -> 0;
        };
    }

    private static int ambientCountFor(MobTier tier) {
        return switch (tier) {
            case EPIC -> 2;
            case LEGENDARY -> 5;
            case MYTHIC -> 4;
            case DEFINITIVE -> 6;
            default -> 0;
        };
    }

    private static void spawnParticles(ServerWorld world, MobEntity mob, ParticleEffect effect, int count, double speed) {
        if (count <= 0) {
            return;
        }
        world.spawnParticles(
            effect,
            mob.getX(),
            mob.getBodyY(0.5D),
            mob.getZ(),
            count,
            0.25D,
            0.35D,
            0.25D,
            speed
        );
    }
}
