package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * Utilidades compartidas para la serie Bloodlust (Vindicador).
 */
abstract class bloodlust_base_action implements mutation_action {
    protected boolean isVillageProtector(LivingEntity target) {
        return target instanceof Villager || target instanceof IronGolem;
    }

    protected boolean isPlayer(LivingEntity target) {
        return target instanceof Player;
    }
}
