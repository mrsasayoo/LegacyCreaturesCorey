package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

abstract class frenzy_base_action extends proc_on_hit_action {
    private final FrenzyProfile profile;

    protected frenzy_base_action(mutation_action_config config,
            double defaultChance,
            FrenzyProfile defaults) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.profile = applyOverrides(config, defaults);
    }

    private FrenzyProfile applyOverrides(mutation_action_config config, FrenzyProfile base) {
        int speedDuration = resolveTicks(config, "speed_duration", base.speedDurationTicks());
        int hasteDuration = resolveTicks(config, "haste_duration", base.hasteDurationTicks());
        int playerBuffDuration = resolveTicks(config, "player_buff_duration", base.playerBuffDurationTicks());
        int baseAmplifier = Math.max(0, config.getInt("base_amplifier", base.baseAmplifier()));
        int playerBuffAmplifier = Math.max(0, config.getInt("player_buff_amplifier", base.playerBuffAmplifier()));
        int maxStacks = Math.max(1, config.getInt("max_stacks", base.maxStacks()));
        boolean applyHaste = config.getBoolean("apply_haste", base.applyHaste());
        boolean stackable = config.getBoolean("stackable", base.stackable());
        return new FrenzyProfile(speedDuration, hasteDuration, applyHaste, stackable, maxStacks, baseAmplifier,
                playerBuffDuration, playerBuffAmplifier);
    }

    private int resolveTicks(mutation_action_config config, String keyPrefix, int fallback) {
        int ticks = config.getInt(keyPrefix + "_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt(keyPrefix + "_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallback);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!action_context.isServer(attacker)) {
            return;
        }

        int speedAmplifier = profile.stackable()
                ? getNextAmplifier(attacker, StatusEffects.SPEED, profile.maxStacks())
                : profile.baseAmplifier();
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, profile.speedDurationTicks(), speedAmplifier));

        if (profile.applyHaste()) {
            int hasteAmplifier = profile.stackable()
                    ? getNextAmplifier(attacker, StatusEffects.HASTE, profile.maxStacks())
                    : profile.baseAmplifier();
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, profile.hasteDurationTicks(), hasteAmplifier));
        }

        if (profile.playerBuffDurationTicks() > 0) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                    profile.playerBuffDurationTicks(), profile.playerBuffAmplifier()));
        }
    }

    private int getNextAmplifier(LivingEntity entity, RegistryEntry<StatusEffect> effect, int maxStacks) {
        StatusEffectInstance current = entity.getStatusEffect(effect);
        if (current == null) {
            return profile.baseAmplifier();
        }
        int next = current.getAmplifier() + 1;
        int maxAmplifier = Math.max(profile.baseAmplifier(), maxStacks - 1);
        return Math.min(next, maxAmplifier);
    }

    protected record FrenzyProfile(int speedDurationTicks,
                                   int hasteDurationTicks,
                                   boolean applyHaste,
                                   boolean stackable,
                                   int maxStacks,
                                   int baseAmplifier,
                                   int playerBuffDurationTicks,
                                   int playerBuffAmplifier) {
    }
}
