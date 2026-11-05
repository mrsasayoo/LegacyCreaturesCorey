package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

public final class wither_3_action extends status_effect_single_target_base_action {
    public wither_3_action(mutation_action_config config) {
        super(config,
                MobEffects.WITHER,
                secondsToTicks(7),
                0,
                Target.OTHER,
                0.08D);
    }

    @Override
    protected void applyAdditionalEffects(LivingEntity attacker, LivingEntity victim) {
        // «Toque Mortal»: latido grave que acelera según la vida restante del jugador.
        if (victim.level() instanceof ServerLevel world) {
            proc_side_effects_helper.scheduleWitherHeartbeat(world, victim);
        }
    }
}
