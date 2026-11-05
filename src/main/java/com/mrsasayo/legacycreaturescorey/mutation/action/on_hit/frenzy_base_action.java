package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * «Frenesí»: cada golpe tiene la chance del nivel de acumular 1 stack (máx. según nivel).
 * Escalera de stacks (sin Prisa: esa subclase no aportaba nada):
 *   stack 1 = Velocidad I
 *   stack 2 = Velocidad II
 *   stack 3 = Velocidad II + Fuerza I
 *   stack 4 = Velocidad II + Fuerza I + Resistencia I
 * En el nivel 3, al llegar a 4 stacks la Fuerza sube a II (strength_amplifier_at_max).
 * Contrapeso: el jugador golpeado recibe Fuerza según el nivel.
 */
abstract class frenzy_base_action extends proc_on_hit_action {
    private static final Map<LivingEntity, long[]> STACKS = new WeakHashMap<>(); // [stacks, lastProcTick]

    private final int speedDurationTicks;
    private final int maxStacks;
    private final int playerBuffDurationTicks;
    private final int playerBuffAmplifier;
    private final int strengthAmplifierAtMax;

    protected frenzy_base_action(mutation_action_config config,
            double defaultChance,
            int defaultSpeedSeconds,
            int defaultMaxStacks,
            int defaultPlayerBuffSeconds,
            int defaultPlayerBuffAmplifier,
            int defaultStrengthAmplifierAtMax) {
        super(config, defaultChance);
        this.speedDurationTicks = resolveSeconds(config, "speed_duration", defaultSpeedSeconds * 20);
        this.maxStacks = Math.max(1, config.getInt("max_stacks", defaultMaxStacks));
        this.playerBuffDurationTicks = resolveSeconds(config, "player_buff_duration", defaultPlayerBuffSeconds * 20);
        this.playerBuffAmplifier = Math.max(0, config.getInt("player_buff_amplifier", defaultPlayerBuffAmplifier));
        this.strengthAmplifierAtMax = Math.max(0, config.getInt("strength_amplifier_at_max", defaultStrengthAmplifierAtMax));
    }

    private static int resolveSeconds(mutation_action_config config, String prefix, int fallbackTicks) {
        int ticks = config.getInt(prefix + "_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt(prefix + "_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!action_context.isServer(attacker) || !(attacker.level() instanceof ServerLevel world)) {
            return;
        }
        long time = world.getGameTime();
        long[] state = STACKS.get(attacker);
        int previous = (state != null && time - state[1] <= speedDurationTicks + 40L) ? (int) state[0] : 0;
        int stacks = Math.min(maxStacks, previous + 1);
        STACKS.put(attacker, new long[]{stacks, time});

        int speedAmplifier = Math.min(stacks, 2) - 1;
        attacker.addEffect(new MobEffectInstance(MobEffects.SPEED, speedDurationTicks, speedAmplifier));
        if (stacks >= 3) {
            int strengthAmplifier = (stacks >= maxStacks && strengthAmplifierAtMax > 0) ? strengthAmplifierAtMax : 0;
            attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH, speedDurationTicks, strengthAmplifier));
        }
        if (stacks >= 4) {
            attacker.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, speedDurationTicks, 0));
        }

        // Contrapeso al jugador golpeado.
        if (playerBuffDurationTicks > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.STRENGTH, playerBuffDurationTicks, playerBuffAmplifier));
        }
    }
}
