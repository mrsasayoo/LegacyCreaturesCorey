package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HuskEntity;

public final class ancient_curse_1_action extends ancient_curse_base_action {
    private final double chance;
    private final int durationTicks;
    private final int amplifier;

    public ancient_curse_1_action(mutation_action_config config) {
        this.chance = config.getDouble("chance", 0.10D);
        this.durationTicks = config.getInt("duration_ticks", 80);
        this.amplifier = config.getInt("amplifier", 0);
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        HuskEntity husk = asServerHusk(attacker);
        if (husk == null) {
            return;
        }
        if (husk.getRandom().nextDouble() <= chance) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, durationTicks, amplifier));
        }
    }
}
