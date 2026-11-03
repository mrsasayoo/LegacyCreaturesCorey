package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Utilidad para mantener consistentes las partículas asociadas a los tiers.
 */
final class TierParticleHelper {
    private TierParticleHelper() {}

    static void spawnInitialBurst(ServerLevel world, Mob mob, MobTier tier) {
        ParticleOptions effect = resolveEffect(tier);
        if (effect != null) {
            spawnParticles(world, mob, effect, initialCountFor(tier), 0.01D);
        }
    }

    static void spawnAmbient(ServerLevel world, Mob mob, MobTier tier) {
        ParticleOptions effect = resolveEffect(tier);
        if (effect != null) {
            spawnParticles(world, mob, effect, ambientCountFor(tier), 0.004D);
        }
    }

    private static ParticleOptions resolveEffect(MobTier tier) {
        return switch (tier) {
            case EPIC -> ParticleTypes.SOUL;
            case LEGENDARY -> ParticleTypes.FALLING_DRIPSTONE_LAVA;
            case MYTHIC -> PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 0.5F);
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

    private static void spawnParticles(ServerLevel world, Mob mob, ParticleOptions effect, int count, double speed) {
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
