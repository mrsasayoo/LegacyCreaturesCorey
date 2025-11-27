package com.mrsasayo.legacycreaturescorey.core.api;

import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.feature.mob.TierManager;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyManager;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyProvider;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyStatus;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Public entry-point so external modules can interact with Legacy Creatures features
 * without depending on internal packages.
 */
public final class CoreyAPI {

    private CoreyAPI() {
    }

    public static Optional<MobEntity> spawnCategorizedMob(ServerWorld world,
                                                          BlockPos position,
                                                          EntityType<? extends MobEntity> entityType,
                                                          MobTier tier) {
        return spawnCategorizedMob(world, position, entityType, tier, List.of());
    }

    public static Optional<MobEntity> spawnCategorizedMob(ServerWorld world,
                                                          BlockPos position,
                                                          EntityType<? extends MobEntity> entityType,
                                                          MobTier tier,
                                                          List<Identifier> forcedMutations) {
        if (world == null || position == null || entityType == null) {
            return Optional.empty();
        }

        MobEntity mob = entityType.create(world, null, position, SpawnReason.COMMAND, true, false);
        if (mob == null) {
            return Optional.empty();
        }

        if (!world.spawnEntity(mob)) {
            mob.discard();
            return Optional.empty();
        }

        MobTier resolvedTier = tier == null ? MobTier.NORMAL : tier;
        boolean assignDefaults = forcedMutations == null || forcedMutations.isEmpty();
        TierManager.forceTier(mob, resolvedTier, assignDefaults);

        if (!assignDefaults) {
            MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
            data.clearMutations();
            for (Identifier mutationId : forcedMutations) {
                data.addMutation(mutationId);
            }
        }

        return Optional.of(mob);
    }

    public static Optional<MobTier> getTier(MobEntity mob) {
        if (mob == null) {
            return Optional.empty();
        }
        MobLegacyData data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
        if (data == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.getTier());
    }

    public static List<Identifier> getMutations(MobEntity mob) {
        if (mob == null) {
            return List.of();
        }
        MobLegacyData data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
        return data == null ? List.of() : data.getMutations();
    }

    public static boolean registerSynergyProvider(SynergyProvider provider) {
        if (provider == null) {
            return false;
        }
        return SynergyManager.registerExternal(provider);
    }

    public static List<SynergyStatus> getSynergyStatuses() {
        return SynergyManager.dumpStatuses();
    }
}
