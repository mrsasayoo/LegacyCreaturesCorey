package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class deep_darkness_aura_2_action implements mutation_action {
    private final double radius;
    private final int intervalTicks;
    private final int darknessDurationTicks;
    private final int lightThreshold;
    private final boolean removeNightVision;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public deep_darkness_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 7.0D);
        this.intervalTicks = config.getInt("interval_ticks", 20);
        this.darknessDurationTicks = config.getInt("darkness_duration_ticks", 40);
        this.lightThreshold = Math.max(0, config.getInt("light_threshold", 7));
        this.removeNightVision = config.getBoolean("remove_night_vision", false);
        this.effects = resolveEffects(config, darknessDurationTicks);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity))
            return;
        if (intervalTicks > 0 && entity.age % intervalTicks != 0)
            return;

        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.squaredDistanceTo(entity) <= radius * radius) {
                int lightLevel = world.getLightLevel(player.getBlockPos());
                if (lightLevel >= lightThreshold) {
                    continue;
                }
                if (removeNightVision) {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                }
                status_effect_config_parser.applyEffects(player, effects);
            }
        }
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDurationTicks) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(MobEffects.DARKNESS,
                    fallbackDurationTicks,
                    0,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }
}
