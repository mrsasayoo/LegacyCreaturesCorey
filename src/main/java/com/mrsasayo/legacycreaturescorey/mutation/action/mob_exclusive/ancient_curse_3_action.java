package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;

public final class ancient_curse_3_action extends ancient_curse_base_action {
    private final double chance;
    private final int curseDurationTicks;
    private final double precariousHungerChance;
    private final int precariousHungerDuration;

    public ancient_curse_3_action(mutation_action_config config) {
        this.chance = config.getDouble("chance", 0.10D);
        this.curseDurationTicks = config.getInt("curse_duration_ticks", 600);
        // Nueva configuración para precarious_hunger
        this.precariousHungerChance = config.getDouble("precarious_hunger_chance", 0.10D); // 10% de probabilidad
        this.precariousHungerDuration = config.getInt("precarious_hunger_duration", 400); // 20 segundos
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        Husk husk = asServerHusk(attacker);
        if (husk == null || !(target instanceof Player player)) {
            return;
        }
        
        // Aplicar maldición de momia con la probabilidad configurada
        if (husk.getRandom().nextDouble() <= chance) {
            applyMummysCurse(player, curseDurationTicks);
            spawnCurseParticles(player);
        }
        
        // Nuevo efecto: 10% de probabilidad de aplicar precarious_hunger
        if (husk.getRandom().nextDouble() <= precariousHungerChance) {
            applyPrecariousHunger(player);
        }
    }
    
    /**
     * Aplica el efecto de hambre precaria que reduce la nutrición de la comida en 25%
     */
    private void applyPrecariousHunger(Player player) {
        player.addEffect(new MobEffectInstance(
            Holder.direct(ModStatusEffects.PRECARIOUS_HUNGER),
            precariousHungerDuration,
            0, // amplificador 0 = 25% reducción
            false, // ambient
            true, // show particles
            true  // show icon
        ));
        
        // Partículas especiales de hambre al aplicar el efecto
        if (player.getEntityWorld() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.WITCH,
                    player.getX(),
                    player.getY() + 0.5D,
                    player.getZ(),
                    15,
                    0.3D,
                    0.4D,
                    0.3D,
                    0.02D);
        }
    }
    
    /**
     * Genera partículas de maldición alrededor del jugador
     */
    private void spawnCurseParticles(Player player) {
        if (player.getEntityWorld() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.SOUL,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    10,
                    0.5D,
                    0.5D,
                    0.5D,
                    0.1D);
        }
    }
}
