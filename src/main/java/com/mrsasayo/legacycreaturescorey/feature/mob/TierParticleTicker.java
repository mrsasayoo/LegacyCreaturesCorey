package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;

/**
 * Reaplica partículas de tier a intervalos regulares para mantenerlas visibles.
 */
public final class TierParticleTicker {
    private static final int AMBIENT_INTERVAL_TICKS = 15;

    private TierParticleTicker() {}

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(TierParticleTicker::onWorldTick);
    }

    private static void onWorldTick(ServerLevel world) {
        if (world.getGameTime() % AMBIENT_INTERVAL_TICKS != 0) {
            return;
        }

        for (Entity entity : world.getAllEntities()) {
            if (!(entity instanceof Mob mob)) {
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
