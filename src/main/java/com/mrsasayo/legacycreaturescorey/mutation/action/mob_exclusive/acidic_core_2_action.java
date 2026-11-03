package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Acidic Core II: Nube Ácida Persistente
 * 
 * Al morir, el slime crea un área de efecto con veneno y partículas densas.
 * Mejorado: Usa múltiples tipos de partículas para mayor visibilidad.
 */
public final class acidic_core_2_action implements mutation_action {
    private final float radius;
    private final int durationTicks;
    private final int effectDuration;
    private final int effectAmplifier;

    public acidic_core_2_action(mutation_action_config config) {
        this.radius = (float) Math.max(0.5D, config.getDouble("radius", 2.0D));
        this.durationTicks = Math.max(20, config.getInt("duration_ticks", 160));
        this.effectDuration = Math.max(20, config.getInt("effect_duration_ticks", 60));
        this.effectAmplifier = Math.max(0, config.getInt("effect_amplifier", 0));
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, LivingEntity killer) {
        if (!(entity instanceof Slime slime)) {
            return;
        }
        if (!(slime.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        
        double x = slime.getX();
        double y = slime.getY();
        double z = slime.getZ();
        
        // Crear nube de área con partículas de slime (visible y compatible)
        AreaEffectCloud cloud = new AreaEffectCloud(world, x, y, z);
        cloud.setCustomParticle(ParticleTypes.ITEM_SLIME);
        cloud.setRadius(radius);
        cloud.setDuration(durationTicks);
        cloud.setWaitTime(0);
        cloud.setRadiusOnUse(-0.1F);
        cloud.setRadiusPerTick(-radius / (float) (durationTicks * 1.5));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, effectDuration, effectAmplifier));
        world.spawnEntity(cloud);
        
        // Partículas adicionales de explosión inicial
        world.sendParticles(ParticleTypes.ITEM_SLIME,
                x, y + 0.5D, z,
                60,
                radius * 0.8D,
                0.3D,
                radius * 0.8D,
                0.1D);
        
        // Partículas de vapor ácido ascendente
        world.sendParticles(ParticleTypes.SNEEZE,
                x, y + 0.2D, z,
                30,
                radius * 0.5D,
                0.5D,
                radius * 0.5D,
                0.05D);
        
        // Partículas verdes brillantes
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y + 0.1D, z,
                40,
                radius * 0.7D,
                0.1D,
                radius * 0.7D,
                0.02D);
        
        // Crear segunda nube visual con partículas de slime
        AreaEffectCloud visualCloud = new AreaEffectCloud(world, x, y + 0.1D, z);
        visualCloud.setCustomParticle(ParticleTypes.ITEM_SLIME);
        visualCloud.setRadius(radius * 0.8f);
        visualCloud.setDuration(durationTicks / 2);
        visualCloud.setWaitTime(0);
        visualCloud.setRadiusPerTick(-0.01F);
        world.spawnEntity(visualCloud);
    }
}
