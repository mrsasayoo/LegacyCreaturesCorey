package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class corruption_aura_3_action implements mutation_action {
    private final float damage;
    private final int intervalTicks;
    private final double radius;
    private final int hungerDurationTicks;
    private final int hungerAmplifier;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public corruption_aura_3_action(mutation_action_config config) {
        this.damage = config.getFloat("damage", 3.0F);
        this.intervalTicks = config.getInt("interval_ticks", 80);
        this.radius = config.getDouble("radius", 5.0D);
        this.hungerDurationTicks = config.getInt("hunger_duration_ticks", 20);
        this.hungerAmplifier = config.getInt("hunger_amplifier", 0);
        this.effects = resolveEffects(config, hungerDurationTicks, hungerAmplifier);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) return;
        if (intervalTicks > 0 && entity.tickCount % intervalTicks != 0) return;

        ServerLevel world = (ServerLevel) entity.level();
        DamageSource source = world.damageSources().magic();
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.distanceToSqr(entity) <= radius * radius) {
                player.hurtServer(world, source, damage);
                status_effect_config_parser.applyEffects(player, effects);
            }
        }
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDuration,
            int fallbackAmplifier) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackDuration > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(MobEffects.HUNGER,
                    fallbackDuration,
                    Math.max(0, fallbackAmplifier),
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }
}
