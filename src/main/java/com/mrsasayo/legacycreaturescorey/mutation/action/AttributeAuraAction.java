package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies an attribute modifier to nearby entities while the aura is active.
 */
public final class AttributeAuraAction implements MutationAction {
    private final Identifier attributeId;
    private RegistryEntry<EntityAttribute> attribute;
    private EntityAttribute attributeType;
    private boolean loggedMissingAttribute;
    private final Operation operation;
    private final double amount;
    private final double radius;
    private final Target target;
    private final boolean excludeSelf;

    private final Map<LivingEntity, Identifier> appliedModifiers = new HashMap<>();

    public AttributeAuraAction(Identifier attributeId, Operation operation, double amount, double radius, Target target, boolean excludeSelf) {
        this.attributeId = attributeId;
        this.operation = operation;
        this.amount = amount;
        this.radius = Math.max(0.5D, radius);
        this.target = target;
        this.excludeSelf = excludeSelf;
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        if (!ensureAttributeAvailable()) {
            return;
        }
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        Set<LivingEntity> currentTargets = findTargets(entity, world);

        // Apply modifiers to new targets
        for (LivingEntity target : currentTargets) {
            if (appliedModifiers.containsKey(target)) {
                continue;
            }
            applyModifier(target);
        }

        // Remove modifiers from entities that left the aura
        Iterator<Map.Entry<LivingEntity, Identifier>> iterator = appliedModifiers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Identifier> entry = iterator.next();
            LivingEntity target = entry.getKey();
            if (!currentTargets.contains(target) || !target.isAlive()) {
                removeModifier(target, entry.getValue());
                iterator.remove();
            }
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        // Clear any lingering modifiers when the aura is removed
        Iterator<Map.Entry<LivingEntity, Identifier>> iterator = appliedModifiers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Identifier> entry = iterator.next();
            removeModifier(entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    private boolean ensureAttributeAvailable() {
        if (attribute != null && attributeType != null) {
            return true;
        }
        resolveAttribute();
        return attribute != null && attributeType != null;
    }

    private void resolveAttribute() {
        EntityAttribute resolved = Registries.ATTRIBUTE.get(attributeId);
        if (resolved == null) {
            if (!loggedMissingAttribute) {
                Legacycreaturescorey.LOGGER.debug("Atributo de aura {} aún no disponible", attributeId);
                loggedMissingAttribute = true;
            }
            return;
        }
        this.attribute = Registries.ATTRIBUTE.getEntry(resolved);
        this.attributeType = resolved;
        loggedMissingAttribute = false;
    }

    private Set<LivingEntity> findTargets(LivingEntity source, ServerWorld world) {
        double radiusSquared = radius * radius;
        Box box = source.getBoundingBox().expand(radius);
        List<? extends Entity> candidates = switch (target) {
            case ALLY_MOBS -> world.getEntitiesByClass(MobEntity.class, box, Entity::isAlive);
            case PLAYERS -> world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSquared);
            case SELF -> Collections.singletonList(source);
        };

        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }

        Set<LivingEntity> results = new HashSet<>();
        for (Entity candidate : candidates) {
            if (!(candidate instanceof LivingEntity living)) {
                continue;
            }
            if (excludeSelf && candidate == source) {
                continue;
            }
            if (source.squaredDistanceTo(living) > radiusSquared) {
                continue;
            }
            results.add(living);
        }
        return results;
    }

    private void applyModifier(LivingEntity target) {
        if (!ensureAttributeAvailable()) {
            return;
        }
        EntityAttributeInstance instance = target.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        Identifier modifierId = Identifier.of(
            Legacycreaturescorey.MOD_ID,
            "attribute_aura_" + Integer.toHexString(System.identityHashCode(this)) + "_" + target.getId()
        );
        EntityAttributeModifier.Operation op = switch (operation) {
            case ADD -> EntityAttributeModifier.Operation.ADD_VALUE;
            case MULTIPLY_BASE -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        };
        if (instance.hasModifier(modifierId)) {
            appliedModifiers.put(target, modifierId);
            return;
        }
        EntityAttributeModifier modifier = new EntityAttributeModifier(modifierId, amount, op);
        instance.addPersistentModifier(modifier);
        appliedModifiers.put(target, modifierId);
        if (attributeType == EntityAttributes.MAX_HEALTH) {
            // Ensure current health is not above the new cap
            float maxHealth = target.getMaxHealth();
            if (target.getHealth() > maxHealth) {
                target.setHealth(maxHealth);
            }
        }
    }

    private void removeModifier(LivingEntity target, Identifier modifierId) {
        if (!ensureAttributeAvailable()) {
            return;
        }
        EntityAttributeInstance instance = target.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        if (instance.hasModifier(modifierId)) {
            instance.removeModifier(modifierId);
        }
    }

    public enum Operation {
        ADD,
        MULTIPLY_BASE;

        public static Operation fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "ADD", "ADDITION", "ADD_VALUE" -> ADD;
                case "MULTIPLY_BASE", "MULTIPLY" -> MULTIPLY_BASE;
                default -> throw new IllegalArgumentException("Operacion de atributo desconocida: " + raw);
            };
        }
    }

    public enum Target {
        ALLY_MOBS,
        PLAYERS,
        SELF;

        public static Target fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "ALLY", "ALLY_MOBS", "ALLIES" -> ALLY_MOBS;
                case "PLAYER", "PLAYERS" -> PLAYERS;
                case "SELF" -> SELF;
                default -> throw new IllegalArgumentException("Objetivo de aura de atributo desconocido: " + raw);
            };
        }
    }
}
