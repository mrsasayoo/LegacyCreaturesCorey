package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

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
public final class AttributeAuraAction implements mutation_action {
    private final Identifier attributeId;
    private Holder<Attribute> attribute;
    private Attribute attributeType;
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
        if (!action_context.isServer(entity)) {
            return;
        }
        if (!ensureAttributeAvailable()) {
            return;
        }
        ServerLevel world = (ServerLevel) entity.getEntityWorld();
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
            LivingEntity target = entry.unwrapKey();
            if (!currentTargets.intersects(target) || !target.isAlive()) {
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
            removeModifier(entry.unwrapKey(), entry.getValue());
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
        Attribute resolved = BuiltInRegistries.ATTRIBUTE.get(attributeId);
        if (resolved == null) {
            if (!loggedMissingAttribute) {
                Legacycreaturescorey.LOGGER.debug("Atributo de aura {} aún no disponible", attributeId);
                loggedMissingAttribute = true;
            }
            return;
        }
        this.attribute = BuiltInRegistries.ATTRIBUTE.getEntry(resolved);
        this.attributeType = resolved;
        loggedMissingAttribute = false;
    }

    private Set<LivingEntity> findTargets(LivingEntity source, ServerLevel world) {
        double radiusSquared = radius * radius;
        AABB box = source.getLocalBoundsForPose().inflate(radius);
        List<? extends Entity> candidates = switch (target) {
            case ALLY_MOBS -> world.getEntitiesByClass(Mob.class, box, Entity::isAlive);
            case PLAYERS -> world.getPlayers(player -> player.isAlive() && source.distanceToSqr(player) <= radiusSquared);
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
            if (source.distanceToSqr(living) > radiusSquared) {
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
        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        Identifier modifierId = Identifier.ofSize(
            Legacycreaturescorey.MOD_ID,
            "attribute_aura_" + Integer.toHexString(System.identityHashCode(this)) + "_" + target.getId()
        );
        AttributeModifier.Operation op = switch (operation) {
            case ADD -> AttributeModifier.Operation.ADD_VALUE;
            case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        };
        if (instance.hasModifier(modifierId)) {
            appliedModifiers.put(target, modifierId);
            return;
        }
        AttributeModifier modifier = new AttributeModifier(modifierId, amount, op);
        instance.addPermanentModifier(modifier);
        appliedModifiers.put(target, modifierId);
        if (attributeType == Attributes.MAX_HEALTH) {
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
        AttributeInstance instance = target.getAttribute(attribute);
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
