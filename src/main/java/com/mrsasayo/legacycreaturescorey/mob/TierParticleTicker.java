package com.mrsasayo.legacycreaturescorey.mob;

import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Reaplica partículas de tier a intervalos regulares para mantenerlas visibles.
 */
public final class TierParticleTicker {
    private static final int AMBIENT_INTERVAL_TICKS = 15;

    private TierParticleTicker() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(TierParticleTicker::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        if (world.getTime() % AMBIENT_INTERVAL_TICKS != 0) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof MobEntity mob)) {
                continue;
            }

            var data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
            if (data == null) {
                continue;
            }

            MobTier tier = data.getTier();
            if (tier == MobTier.NORMAL) {
                continue;
            }

            TierParticleHelper.spawnAmbient(world, mob, tier);
        }
    }
}
