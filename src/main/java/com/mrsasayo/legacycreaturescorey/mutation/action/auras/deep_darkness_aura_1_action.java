package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public class deep_darkness_aura_1_action implements MutationAction {
    private final double radius;
    private final int intervalTicks;
    private final int blindnessDurationTicks;
    private final boolean removeNightVision;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public deep_darkness_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 7.0D);
        this.intervalTicks = config.getInt("interval_ticks", 20);
        this.blindnessDurationTicks = config.getInt("blindness_duration_ticks", 40);
        this.removeNightVision = config.getBoolean("remove_night_vision", false);
        this.effects = resolveEffects(config, blindnessDurationTicks);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity))
            return;
        if (intervalTicks > 0 && entity.age % intervalTicks != 0)
            return;

        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        for (PlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.squaredDistanceTo(entity) <= radius * radius) {
                if (removeNightVision) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
                status_effect_config_parser.applyEffects(player, effects);
            }
        }
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDurationTicks) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(StatusEffects.BLINDNESS,
                    fallbackDurationTicks,
                    0,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }
}
