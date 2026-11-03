package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.server.level.ServerLevel;

public final class abyssal_armor_1_action extends abyssal_armor_base_action {
    private final float bonusDamage;

    public abyssal_armor_1_action(mutation_action_config config) {
        double configured = config.getDouble("bonus_magic_damage", 1.0D);
        this.bonusDamage = (float) Math.max(0.0D, configured);
    }

    @Override
    protected boolean hasThornsRetaliation() {
        return true;
    }

    @Override
    protected void onThornsRetaliation(ElderGuardian owner, LivingEntity victim, ServerLevel world) {
        if (bonusDamage <= 0.0F) {
            return;
        }
        victim.damage(world, owner.getDamageSources().magic(), bonusDamage);
    }
}
