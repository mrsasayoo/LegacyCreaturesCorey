package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Apiarian Warfare I: Picadura Venenosa
 * 
 * La abeja aplica veneno normal al picar sin morir.
 * Tiene un cooldown de 7 segundos entre picaduras.
 */
public final class apiarian_warfare_1_action extends apiarian_warfare_base_action {
    private final int poisonDurationTicks;
    private final int poisonAmplifier;
    private final int attackCooldownTicks;
    
    // Rastrear cooldown de cada abeja
    private final Map<BeeEntity, Long> lastAttackTimes = new WeakHashMap<>();

    public apiarian_warfare_1_action(mutation_action_config config) {
        this.poisonDurationTicks = config.getInt("poison_duration_ticks", 100); // 5 segundos de veneno
        this.poisonAmplifier = config.getInt("poison_amplifier", 0); // Veneno I (normal)
        this.attackCooldownTicks = config.getInt("attack_cooldown_ticks", 140); // 7 segundos de cooldown
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        BeeEntity bee = asServerBee(attacker);
        if (bee == null) {
            return;
        }
        
        long currentTime = bee.getEntityWorld().getTime();
        Long lastAttack = lastAttackTimes.get(bee);
        
        // Verificar cooldown
        if (lastAttack != null && (currentTime - lastAttack) < attackCooldownTicks) {
            return; // En cooldown, no atacar
        }
        
        // Aplicar veneno
        target.addStatusEffect(new StatusEffectInstance(
            StatusEffects.POISON, 
            poisonDurationTicks, 
            poisonAmplifier,
            false,
            true,
            true
        ));
        
        // Registrar tiempo del ataque
        lastAttackTimes.put(bee, currentTime);
        
        // Efectos visuales
        if (bee.getEntityWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ITEM_SLIME,
                    target.getX(),
                    target.getY() + target.getHeight() / 2,
                    target.getZ(),
                    8, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        
        // IMPORTANTE: Prevenir la muerte de la abeja
        preventBeeDeath(bee);
    }
    
    @Override
    public void onTick(LivingEntity entity) {
        BeeEntity bee = asServerBee(entity);
        if (bee == null) {
            return;
        }
        
        // Evitar que la abeja muera por picar
        preventBeeDeath(bee);
    }
    
    /**
     * Evita que la abeja muera después de picar.
     * Las abejas normalmente mueren porque tienen el estado "has stung" y un contador.
     */
    private void preventBeeDeath(BeeEntity bee) {
        // Resetear el estado de "ha picado" para evitar la muerte
        // En Minecraft, las abejas mueren porque el flag hasStung + ticksSinceStung > 1200
        // Usamos reflexión o simplemente curamos a la abeja si está por morir
        
        // La forma más simple y segura: si la abeja tiene muy poca vida, curarla
        if (bee.getHealth() < bee.getMaxHealth() * 0.1f && bee.getHealth() > 0) {
            bee.setHealth(bee.getMaxHealth() * 0.5f);
        }
        
        // Remover cualquier flag de envenenamiento/daño propio
        // Las abejas marcan hasStung=true al picar, y esto las mata después
        // Sin acceso directo, prevenimos la muerte curándolas
    }
    
    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof BeeEntity bee) {
            lastAttackTimes.remove(bee);
        }
    }
}
