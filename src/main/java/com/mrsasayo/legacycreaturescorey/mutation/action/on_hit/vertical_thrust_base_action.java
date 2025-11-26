package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

/**
 * Controla los parámetros compartidos de Levantamiento.
 */
abstract class vertical_thrust_base_action extends ProcOnHitAction {
    private final double upwardVelocity;
    private final int selfDowntimeTicks;
    private final int downtimeAmplifier;

    protected vertical_thrust_base_action(mutation_action_config config,
            double defaultChance,
            double defaultUpwardVelocity,
            int defaultDowntimeTicks,
            int defaultDowntimeAmplifier) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.upwardVelocity = Math.max(0.0D, config.getDouble("upward_velocity", defaultUpwardVelocity));
        this.selfDowntimeTicks = resolveDowntimeTicks(config, defaultDowntimeTicks);
        this.downtimeAmplifier = Math.max(0, config.getInt("self_downtime_amplifier", defaultDowntimeAmplifier));
    }

    private int resolveDowntimeTicks(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("self_downtime_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        double seconds = config.getDouble("self_downtime_seconds", -1.0D);
        if (seconds >= 0.0D) {
            return Math.max(0, (int) Math.round(seconds * 20.0D));
        }
        return Math.max(0, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (victim != null && upwardVelocity > 0.0D) {
            victim.addVelocity(0.0D, upwardVelocity, 0.0D);
            victim.velocityModified = true;
        }
        if (attacker != null && selfDowntimeTicks > 0) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                    selfDowntimeTicks,
                    Math.max(0, downtimeAmplifier)));
        }
    }
}
