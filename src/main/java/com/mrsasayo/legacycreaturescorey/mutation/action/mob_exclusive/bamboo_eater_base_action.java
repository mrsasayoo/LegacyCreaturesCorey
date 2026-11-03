package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mixin.accessor.mob_entity_goal_accessor;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

abstract class bamboo_eater_base_action implements mutation_action {
    private static final Identifier STOP_MOVE_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "bamboo_stop");
    private static final Identifier REACH_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "bamboo_reach");
    private static final Identifier KNOCKBACK_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "bamboo_knockback");
    
    // Rastrear pandas que ya han sido configurados para ser agresivos
    private static final Map<Panda, Boolean> aggressivePandas = new WeakHashMap<>();

    protected Panda asServerPanda(LivingEntity entity) {
        if (entity instanceof Panda panda && !entity.getEntityWorld().isClient()) {
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
    protected void makeAggressive(Panda panda) {
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
        accessor.getTargetSelector().add(1, new NearestAttackableTargetGoal<>(panda, Player.class, true));
        
        // Marcar como configurado
        aggressivePandas.put(panda, true);
        
        // Forzar objetivo inicial
        forceTargetPlayer(panda);
    }
    
    /**
     * Fuerza al panda a targetear al jugador más cercano
     */
    protected void forceTargetPlayer(Panda panda) {
        if (panda.getTarget() == null || !panda.getTarget().isAlive()) {
            // Buscar jugador más cercano
            Player nearestPlayer = panda.getEntityWorld().getClosestPlayer(
                    panda.getX(), panda.getY(), panda.getZ(),
                    16.0D, // Radio de detección
                    true   // Solo jugadores vivos
            );
            
            if (nearestPlayer != null) {
                panda.setTarget(nearestPlayer);
            }
        }
    }

    protected boolean tryConsumeBamboo(Panda panda,
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
        ServerLevel world = (ServerLevel) panda.getEntityWorld();
        world.breakBlock(bambooPos, false);
        panda.heal(healAmount);
        if (resistanceTicks > 0) {
            panda.addStatusEffect(new MobEffectInstance(MobEffects.RESISTANCE,
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

    protected boolean tryEquipBambooWeapon(Panda panda,
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
        ServerLevel world = (ServerLevel) panda.getEntityWorld();
        world.breakBlock(bambooPos, false);
        handler.equipBamboo(panda, weaponHits);
        applyWeaponModifiers(panda, reachBonus, knockbackBonus);
        if (cooldownTicks > 0) {
            handler.setCooldown(panda, "equip_bamboo", cooldownTicks);
        }
        return true;
    }

    protected void handleWeaponHit(Panda panda) {
        Handler handler = handler();
        if (!handler.hasBambooWeapon(panda)) {
            return;
        }
        handler.decrementWeaponDurability(panda);
        if (!handler.hasBambooWeapon(panda)) {
            removeWeaponModifiers(panda);
        }
    }

    private BlockPos findBamboo(Panda panda) {
        BlockPos center = panda.getBlockPos();
        ServerLevel world = (ServerLevel) panda.getEntityWorld();
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

    private static void applyStopMovement(Panda panda) {
        AttributeInstance speed = panda.getAttributeInstance(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(STOP_MOVE_ID)) {
            speed.addPersistentModifier(new AttributeModifier(STOP_MOVE_ID,
                    -1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeStopMovement(Panda panda) {
        AttributeInstance speed = panda.getAttributeInstance(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(STOP_MOVE_ID)) {
            speed.removeModifier(STOP_MOVE_ID);
        }
    }

    protected void removeStopState(LivingEntity entity) {
        if (entity instanceof Panda panda) {
            handler().stopEating(panda);
            removeStopMovement(panda);
        }
    }

    protected void applyWeaponModifiers(Panda panda, double reachBonus, double knockbackBonus) {
        addModifier(panda, Attributes.DEFAULT_ENTITY_INTERACTION_RANGE, REACH_ID, reachBonus,
                AttributeModifier.Operation.ADD_VALUE);
        addModifier(panda, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_ID, knockbackBonus,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void addModifier(Panda panda,
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier id,
            double value,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = panda.getAttributeInstance(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addPersistentModifier(new AttributeModifier(id, value, operation));
        }
    }

    private static void removeWeaponModifiers(Panda panda) {
        removeModifier(panda, Attributes.DEFAULT_ENTITY_INTERACTION_RANGE, REACH_ID);
        removeModifier(panda, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_ID);
    }

    protected void clearWeaponModifiers(LivingEntity entity) {
        if (entity instanceof Panda panda) {
            removeWeaponModifiers(panda);
        }
    }

    private static void removeModifier(Panda panda,
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier id) {
        AttributeInstance instance = panda.getAttributeInstance(attribute);
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof Panda panda) {
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
        private final Map<Panda, Map<String, Integer>> cooldowns = new WeakHashMap<>();
        private final Map<Panda, Integer> eatingTicks = new WeakHashMap<>();
        private final Map<Panda, Integer> weaponDurability = new WeakHashMap<>();

        private Handler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                tickCooldowns(world);
                tickEating(world);
            });
        }

        private void tickCooldowns(ServerLevel world) {
            Iterator<Map.Entry<Panda, Map<String, Integer>>> iterator = cooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Panda, Map<String, Integer>> entry = iterator.next();
                Panda panda = entry.unwrapKey();
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

        private void tickEating(ServerLevel world) {
            Iterator<Map.Entry<Panda, Integer>> iterator = eatingTicks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Panda, Integer> entry = iterator.next();
                Panda panda = entry.unwrapKey();
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

        boolean isOnCooldown(Panda panda, String ability) {
            return cooldowns.computeIfAbsent(panda, ignored -> new HashMap<>()).getOrDefault(ability, 0) > 0;
        }

        void setCooldown(Panda panda, String ability, int ticks) {
            cooldowns.computeIfAbsent(panda, ignored -> new HashMap<>()).put(ability, Math.max(0, ticks));
        }

        boolean isEating(Panda panda) {
            return eatingTicks.containsKey(panda);
        }

        void startEating(Panda panda, int ticks) {
            eatingTicks.put(panda, Math.max(1, ticks));
        }

        void stopEating(Panda panda) {
            eatingTicks.remove(panda);
            removeStopMovement(panda);
        }

        boolean hasBambooWeapon(Panda panda) {
            return weaponDurability.getOrDefault(panda, 0) > 0;
        }

        void equipBamboo(Panda panda, int hits) {
            weaponDurability.put(panda, Math.max(1, hits));
        }

        void decrementWeaponDurability(Panda panda) {
            weaponDurability.computeIfPresent(panda, (ignored, value) -> {
                int remaining = Math.max(0, value - 1);
                return remaining <= 0 ? null : remaining;
            });
        }

        void clear(Panda panda) {
            cooldowns.remove(panda);
            eatingTicks.remove(panda);
            weaponDurability.remove(panda);
        }
    }
}
