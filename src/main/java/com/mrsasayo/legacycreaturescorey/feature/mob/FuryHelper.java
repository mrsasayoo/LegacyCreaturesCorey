package com.mrsasayo.legacycreaturescorey.feature.mob;

import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Maneja la conversión de mobs neutrales en agresivos cuando reciben un tier.
 */
final class FuryHelper {
    private static final double TARGET_RANGE = 16.0D;

    private FuryHelper() {}

    static void applyFury(MobEntity mob, MobLegacyData data) {
        if (data.isFurious() || mob.getEntityWorld().isClient()) {
            return;
        }

        if (mob instanceof HostileEntity) {
            return; // Ya son hostiles por defecto
        }

        PlayerEntity player = mob.getEntityWorld().getClosestPlayer(mob, TARGET_RANGE);
        if (player == null) {
            return;
        }

        data.setFurious(true);
        mob.setTarget(player);
        mob.setAttacking(true);

        if (mob instanceof Angerable angerable) {
            angerable.setAngerTime(600);
            angerable.setAngryAt(player.getUuid());
        }
    }
}
