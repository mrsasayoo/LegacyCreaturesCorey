package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class DetonatingRemainsOnDeathAction implements MutationAction {
    private final DetonatingRemainsManager.RemnantConfig remnantConfig;
    private final double chance;
    private final double chainRadius;
    private final int chainDurationTicks;

    public DetonatingRemainsOnDeathAction(int lingerTicks,
                                          double triggerRadius,
                                          float damage,
                                          @Nullable RegistryEntry<net.minecraft.entity.effect.StatusEffect> statusEffect,
                                          int statusDuration,
                                          int statusAmplifier,
                                          boolean harmless,
                                          double chance,
                                          double chainRadius,
                                          int chainDurationTicks) {
        this.remnantConfig = new DetonatingRemainsManager.RemnantConfig(
            Math.max(1, lingerTicks),
            Math.max(0.25D, triggerRadius),
            Math.max(0.0F, damage),
            statusEffect,
            Math.max(0, statusDuration),
            Math.max(0, statusAmplifier),
            harmless
        );
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        this.chainRadius = Math.max(0.0D, chainRadius);
        this.chainDurationTicks = Math.max(0, chainDurationTicks);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (chance < 1.0D && entity.getRandom().nextDouble() > chance) {
            return;
        }
        Vec3d origin = new Vec3d(entity.getX(), entity.getBodyY(0.1D), entity.getZ());
        DetonatingRemainsManager.registerRemnant(world, origin, remnantConfig);
        if (chainRadius > 0.0D && chainDurationTicks > 0) {
            DetonatingRemainsManager.markChainTargets(world, entity, chainRadius, chainDurationTicks, remnantConfig);
        }
    }
}
