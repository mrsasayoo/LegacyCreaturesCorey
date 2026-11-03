package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;

import java.util.List;

abstract class freezing_base_action extends proc_on_hit_action {
    private final int freezeTicks;
    private final int selfSlownessTicks;
    private final int selfSlownessAmplifier;
    private final List<status_effect_config_parser.status_effect_config_entry> victimEffects;

    protected freezing_base_action(mutation_action_config config,
            double defaultChance,
            int defaultFreezeSeconds,
            int defaultSelfSlownessTicks,
            int defaultSelfSlownessAmplifier) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.freezeTicks = resolveFreezeTicks(config, defaultFreezeSeconds);
        this.selfSlownessTicks = resolveSelfSlownessTicks(config, defaultSelfSlownessTicks);
        this.selfSlownessAmplifier = Math.max(0, config.getInt("self_slowness_amplifier", defaultSelfSlownessAmplifier));
        this.victimEffects = resolveVictimEffects(config, freezeTicks);
    }

    private int resolveFreezeTicks(mutation_action_config config, int fallbackSeconds) {
        int ticks = config.getInt("freeze_ticks", -1);
        if (ticks > 0) {
            return ticks;
        }
        int seconds = config.getInt("freeze_seconds", -1);
        if (seconds > 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackSeconds * 20);
    }

    private int resolveSelfSlownessTicks(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("self_slowness_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt("self_slowness_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackTicks);
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveVictimEffects(
            mutation_action_config config,
            int fallbackDurationTicks) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(MobEffects.SLOWNESS,
                    fallbackDurationTicks,
                    0,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (freezeTicks > 0) {
            victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), freezeTicks));
        }
        status_effect_config_parser.applyEffects(victim, victimEffects);
        if (selfSlownessTicks > 0 && action_context.isServer(attacker)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, selfSlownessTicks, selfSlownessAmplifier));
        }
    }
}
