package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Utilidades compartidas para la serie Bloodlust (Vindicador).
 */
abstract class bloodlust_base_action implements mutation_action {
    protected boolean isVillageProtector(LivingEntity target) {
        return target instanceof VillagerEntity || target instanceof IronGolemEntity;
    }

    protected boolean isPlayer(LivingEntity target) {
        return target instanceof PlayerEntity;
    }
}
