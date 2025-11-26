package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Acidic Core II: Nube Ácida Persistente
 * 
 * Al morir, el slime crea un área de efecto con veneno y partículas densas.
 * Mejorado: Usa múltiples tipos de partículas para mayor visibilidad.
 */
public final class acidic_core_2_action implements MutationAction {
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
        if (!(entity instanceof SlimeEntity slime)) {
            return;
        }
        if (!(slime.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        double x = slime.getX();
        double y = slime.getY();
        double z = slime.getZ();
        
        // Crear nube de área con partículas de slime (visible y compatible)
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, x, y, z);
        cloud.setParticleType(ParticleTypes.ITEM_SLIME);
        cloud.setRadius(radius);
        cloud.setDuration(durationTicks);
        cloud.setWaitTime(0);
        cloud.setRadiusOnUse(-0.1F);
        cloud.setRadiusGrowth(-radius / (float) (durationTicks * 1.5));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, effectDuration, effectAmplifier));
        world.spawnEntity(cloud);
        
        // Partículas adicionales de explosión inicial
        world.spawnParticles(ParticleTypes.ITEM_SLIME,
                x, y + 0.5D, z,
                60,
                radius * 0.8D,
                0.3D,
                radius * 0.8D,
                0.1D);
        
        // Partículas de vapor ácido ascendente
        world.spawnParticles(ParticleTypes.SNEEZE,
                x, y + 0.2D, z,
                30,
                radius * 0.5D,
                0.5D,
                radius * 0.5D,
                0.05D);
        
        // Partículas verdes brillantes
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y + 0.1D, z,
                40,
                radius * 0.7D,
                0.1D,
                radius * 0.7D,
                0.02D);
        
        // Crear segunda nube visual con partículas de slime
        AreaEffectCloudEntity visualCloud = new AreaEffectCloudEntity(world, x, y + 0.1D, z);
        visualCloud.setParticleType(ParticleTypes.ITEM_SLIME);
        visualCloud.setRadius(radius * 0.8f);
        visualCloud.setDuration(durationTicks / 2);
        visualCloud.setWaitTime(0);
        visualCloud.setRadiusGrowth(-0.01F);
        world.spawnEntity(visualCloud);
    }
}
