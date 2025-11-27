package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mixin.accessor.mob_entity_goal_accessor;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

abstract class bamboo_eater_base_action implements mutation_action {
    private static final Identifier STOP_MOVE_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "bamboo_stop");
    private static final Identifier REACH_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "bamboo_reach");
    private static final Identifier KNOCKBACK_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "bamboo_knockback");
    
    // Rastrear pandas que ya han sido configurados para ser agresivos
    private static final Map<PandaEntity, Boolean> aggressivePandas = new WeakHashMap<>();

    protected PandaEntity asServerPanda(LivingEntity entity) {
        if (entity instanceof PandaEntity panda && !entity.getEntityWorld().isClient()) {
            return panda;
        }
        return null;
    }

    protected Handler handler() {
        return Handler.INSTANCE;
    }
    
    /**
     * Configura al panda para que sea agresivo y ataque jugadores continuamente.
     * Modifica el GoalSelector para añadir comportamientos de ataque.
     */
    protected void makeAggressive(PandaEntity panda) {
        if (aggressivePandas.containsKey(panda)) {
            // Ya está configurado, solo actualizar objetivo
            forceTargetPlayer(panda);
            return;
        }
        
        // Usar el accessor para acceder a los goal selectors
        mob_entity_goal_accessor accessor = (mob_entity_goal_accessor) panda;
        
        // Añadir goal de ataque melee con alta prioridad
        accessor.getGoalSelector().add(1, new MeleeAttackGoal(panda, 1.5D, true));
        
        // Añadir goal para targetear jugadores
        accessor.getTargetSelector().add(1, new ActiveTargetGoal<>(panda, PlayerEntity.class, true));
        
        // Marcar como configurado
        aggressivePandas.put(panda, true);
        
        // Forzar objetivo inicial
        forceTargetPlayer(panda);
    }
    
    /**
     * Fuerza al panda a targetear al jugador más cercano
     */
    protected void forceTargetPlayer(PandaEntity panda) {
        if (panda.getTarget() == null || !panda.getTarget().isAlive()) {
            // Buscar jugador más cercano
            PlayerEntity nearestPlayer = panda.getEntityWorld().getClosestPlayer(
                    panda.getX(), panda.getY(), panda.getZ(),
                    16.0D, // Radio de detección
                    true   // Solo jugadores vivos
            );
            
            if (nearestPlayer != null) {
                panda.setTarget(nearestPlayer);
            }
        }
    }

    protected boolean tryConsumeBamboo(PandaEntity panda,
            float healAmount,
            int stopTicks,
            int resistanceTicks,
            int resistanceAmplifier,
            int cooldownTicks) {
        Handler handler = handler();
        if (handler.isOnCooldown(panda, "eat_bamboo") || handler.isEating(panda)) {
            return false;
        }
        BlockPos bambooPos = findBamboo(panda);
        if (bambooPos == null) {
            return false;
        }
        ServerWorld world = (ServerWorld) panda.getEntityWorld();
        world.breakBlock(bambooPos, false);
        panda.heal(healAmount);
        if (resistanceTicks > 0) {
            panda.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    resistanceTicks,
                    Math.max(0, resistanceAmplifier)));
        }
        applyStopMovement(panda);
        if (stopTicks > 0) {
            handler.startEating(panda, stopTicks);
        }
        handler.setCooldown(panda, "eat_bamboo", cooldownTicks);
        return true;
    }

    protected boolean tryEquipBambooWeapon(PandaEntity panda,
            int weaponHits,
            double reachBonus,
            double knockbackBonus,
            int cooldownTicks) {
        Handler handler = handler();
        if (handler.hasBambooWeapon(panda) || handler.isOnCooldown(panda, "equip_bamboo")) {
            return false;
        }
        BlockPos bambooPos = findBamboo(panda);
        if (bambooPos == null) {
            return false;
        }
        ServerWorld world = (ServerWorld) panda.getEntityWorld();
        world.breakBlock(bambooPos, false);
        handler.equipBamboo(panda, weaponHits);
        applyWeaponModifiers(panda, reachBonus, knockbackBonus);
        if (cooldownTicks > 0) {
            handler.setCooldown(panda, "equip_bamboo", cooldownTicks);
        }
        return true;
    }

    protected void handleWeaponHit(PandaEntity panda) {
        Handler handler = handler();
        if (!handler.hasBambooWeapon(panda)) {
            return;
        }
        handler.decrementWeaponDurability(panda);
        if (!handler.hasBambooWeapon(panda)) {
            removeWeaponModifiers(panda);
        }
    }

    private BlockPos findBamboo(PandaEntity panda) {
        BlockPos center = panda.getBlockPos();
        ServerWorld world = (ServerWorld) panda.getEntityWorld();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos candidate = center.add(x, y, z);
                    if (world.getBlockState(candidate).isOf(Blocks.BAMBOO)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static void applyStopMovement(PandaEntity panda) {
        EntityAttributeInstance speed = panda.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(STOP_MOVE_ID)) {
            speed.addPersistentModifier(new EntityAttributeModifier(STOP_MOVE_ID,
                    -1.0D,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeStopMovement(PandaEntity panda) {
        EntityAttributeInstance speed = panda.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(STOP_MOVE_ID)) {
            speed.removeModifier(STOP_MOVE_ID);
        }
    }

    protected void removeStopState(LivingEntity entity) {
        if (entity instanceof PandaEntity panda) {
            handler().stopEating(panda);
            removeStopMovement(panda);
        }
    }

    protected void applyWeaponModifiers(PandaEntity panda, double reachBonus, double knockbackBonus) {
        addModifier(panda, EntityAttributes.ENTITY_INTERACTION_RANGE, REACH_ID, reachBonus,
                EntityAttributeModifier.Operation.ADD_VALUE);
        addModifier(panda, EntityAttributes.ATTACK_KNOCKBACK, KNOCKBACK_ID, knockbackBonus,
                EntityAttributeModifier.Operation.ADD_VALUE);
    }

    private static void addModifier(PandaEntity panda,
            RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
            Identifier id,
            double value,
            EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = panda.getAttributeInstance(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
        }
    }

    private static void removeWeaponModifiers(PandaEntity panda) {
        removeModifier(panda, EntityAttributes.ENTITY_INTERACTION_RANGE, REACH_ID);
        removeModifier(panda, EntityAttributes.ATTACK_KNOCKBACK, KNOCKBACK_ID);
    }

    protected void clearWeaponModifiers(LivingEntity entity) {
        if (entity instanceof PandaEntity panda) {
            removeWeaponModifiers(panda);
        }
    }

    private static void removeModifier(PandaEntity panda,
            RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
            Identifier id) {
        EntityAttributeInstance instance = panda.getAttributeInstance(attribute);
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof PandaEntity panda) {
            removeStopMovement(panda);
            removeWeaponModifiers(panda);
            handler().clear(panda);
            aggressivePandas.remove(panda);
            // Nota: No podemos remover goals fácilmente, pero el panda dejará de atacar
            // cuando no tenga un objetivo o cuando lo pierda
            panda.setTarget(null);
        }
    }

    protected static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private final Map<PandaEntity, Map<String, Integer>> cooldowns = new WeakHashMap<>();
        private final Map<PandaEntity, Integer> eatingTicks = new WeakHashMap<>();
        private final Map<PandaEntity, Integer> weaponDurability = new WeakHashMap<>();

        private Handler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                tickCooldowns(world);
                tickEating(world);
            });
        }

        private void tickCooldowns(ServerWorld world) {
            Iterator<Map.Entry<PandaEntity, Map<String, Integer>>> iterator = cooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PandaEntity, Map<String, Integer>> entry = iterator.next();
                PandaEntity panda = entry.getKey();
                if (panda == null || panda.isRemoved()) {
                    iterator.remove();
                    weaponDurability.remove(panda);
                    eatingTicks.remove(panda);
                    continue;
                }
                if (panda.getEntityWorld() != world) {
                    continue;
                }
                Map<String, Integer> map = entry.getValue();
                map.replaceAll((key, value) -> Math.max(0, value - 1));
            }
        }

        private void tickEating(ServerWorld world) {
            Iterator<Map.Entry<PandaEntity, Integer>> iterator = eatingTicks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PandaEntity, Integer> entry = iterator.next();
                PandaEntity panda = entry.getKey();
                if (panda == null || panda.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                if (panda.getEntityWorld() != world) {
                    continue;
                }
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    removeStopMovement(panda);
                    iterator.remove();
                } else {
                    entry.setValue(remaining);
                }
            }
        }

        boolean isOnCooldown(PandaEntity panda, String ability) {
            return cooldowns.computeIfAbsent(panda, ignored -> new HashMap<>()).getOrDefault(ability, 0) > 0;
        }

        void setCooldown(PandaEntity panda, String ability, int ticks) {
            cooldowns.computeIfAbsent(panda, ignored -> new HashMap<>()).put(ability, Math.max(0, ticks));
        }

        boolean isEating(PandaEntity panda) {
            return eatingTicks.containsKey(panda);
        }

        void startEating(PandaEntity panda, int ticks) {
            eatingTicks.put(panda, Math.max(1, ticks));
        }

        void stopEating(PandaEntity panda) {
            eatingTicks.remove(panda);
            removeStopMovement(panda);
        }

        boolean hasBambooWeapon(PandaEntity panda) {
            return weaponDurability.getOrDefault(panda, 0) > 0;
        }

        void equipBamboo(PandaEntity panda, int hits) {
            weaponDurability.put(panda, Math.max(1, hits));
        }

        void decrementWeaponDurability(PandaEntity panda) {
            weaponDurability.computeIfPresent(panda, (ignored, value) -> {
                int remaining = Math.max(0, value - 1);
                return remaining <= 0 ? null : remaining;
            });
        }

        void clear(PandaEntity panda) {
            cooldowns.remove(panda);
            eatingTicks.remove(panda);
            weaponDurability.remove(panda);
        }
    }
}
