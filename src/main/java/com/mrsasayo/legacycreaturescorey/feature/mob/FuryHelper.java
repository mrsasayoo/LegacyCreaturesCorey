package com.mrsasayo.legacycreaturescorey.feature.mob;

import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Maneja la conversión de mobs neutrales en agresivos cuando reciben un tier.
 */
final class FuryHelper {
    private static final double TARGET_RANGE = 16.0D;

    private FuryHelper() {}

    static void applyFury(Mob mob, MobLegacyData data) {
        if (data.isFurious() || mob.getEntityWorld().isClient()) {
            return;
        }

        if (mob instanceof Monster) {
            return; // Ya son hostiles por defecto
        }

        Player player = mob.getEntityWorld().getClosestPlayer(mob, TARGET_RANGE);
        if (player == null) {
            return;
        }

        data.setFurious(true);
        mob.setTarget(player);
        mob.setAggressive(true);

        if (mob instanceof NeutralMob angerable) {
            angerable.setAngerTime(600);
            angerable.setAngryAt(player.getUuid());
        }
    }
}
