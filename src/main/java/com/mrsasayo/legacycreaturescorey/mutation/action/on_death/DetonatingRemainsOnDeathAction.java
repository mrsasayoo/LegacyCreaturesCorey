package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.action.helper.DetonatingRemainsManager;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DetonatingRemainsOnDeathAction implements mutation_action {
    private final DetonatingRemainsManager.RemnantConfig remnantConfig;
    private final double chance;
    private final double chainRadius;
    private final int chainDurationTicks;

    public DetonatingRemainsOnDeathAction(int lingerTicks,
                                          double triggerRadius,
                                          float damage,
                                          @Nullable Holder<net.minecraft.world.effect.MobEffect> statusEffect,
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
        if (!(entity.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        if (chance < 1.0D && entity.getRandom().nextDouble() > chance) {
            return;
        }
        Vec3 origin = new Vec3(entity.getX(), entity.getBodyY(0.1D), entity.getZ());
        DetonatingRemainsManager.registerRemnant(world, origin, remnantConfig);
        if (chainRadius > 0.0D && chainDurationTicks > 0) {
            DetonatingRemainsManager.markChainTargets(world, entity, chainRadius, chainDurationTicks, remnantConfig);
        }
    }
}
