package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;

public final class ambusher_2_action extends ambusher_base_action {
    private final int maxLightLevel;
    private final int invisibilityDuration;
    private final int invisibilityAmplifier;
    private final boolean ambient;
    private final boolean showParticles;

    public ambusher_2_action(mutation_action_config config) {
        this.maxLightLevel = config.getInt("light_level_threshold", 6);
        this.invisibilityDuration = config.getInt("invisibility_duration", 40);
        this.invisibilityAmplifier = config.getInt("invisibility_amplifier", 0);
        this.ambient = config.getBoolean("ambient", false);
        this.showParticles = config.getBoolean("show_particles", false);
    }

    @Override
    public void onTick(LivingEntity entity) {
        Creeper creeper = asServerCreeper(entity);
        if (creeper == null) {
            return;
        }
        int lightLevel = creeper.getEntityWorld().getLightLevel(creeper.getBlockPos());
        if (lightLevel <= maxLightLevel) {
            MobEffectInstance invisibility = new MobEffectInstance(
                    MobEffects.INVISIBILITY,
                    invisibilityDuration,
                    invisibilityAmplifier,
                    ambient,
                    showParticles);
            creeper.addStatusEffect(invisibility);
        }
    }
}
