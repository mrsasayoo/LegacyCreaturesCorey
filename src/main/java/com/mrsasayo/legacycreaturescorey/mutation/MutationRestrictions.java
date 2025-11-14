package com.mrsasayo.legacycreaturescorey.mutation;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class MutationRestrictions {
    private final Set<Identifier> allowedTypes;
    private final Set<Identifier> excludedTypes;
    private final boolean requiresWater;

    public MutationRestrictions(Set<Identifier> allowedTypes, Set<Identifier> excludedTypes, boolean requiresWater) {
        this.allowedTypes = allowedTypes == null ? Collections.emptySet() : Set.copyOf(allowedTypes);
        this.excludedTypes = excludedTypes == null ? Collections.emptySet() : Set.copyOf(excludedTypes);
        this.requiresWater = requiresWater;
    }

    boolean canApply(MobEntity entity) {
        if (!allowedTypes.isEmpty() && !allowedTypes.contains(typeId(entity))) {
            return false;
        }

        if (!excludedTypes.isEmpty() && excludedTypes.contains(typeId(entity))) {
            return false;
        }

        if (requiresWater && !entity.isTouchingWater()) {
            return false;
        }

        return true;
    }

    private Identifier typeId(MobEntity entity) {
        EntityType<?> type = entity.getType();
        return Registries.ENTITY_TYPE.getId(type);
    }

    public static MutationRestrictions empty() {
        return new MutationRestrictions(new HashSet<>(), new HashSet<>(), false);
    }

    /**
     * If the restriction disallows application, returns a short reason string.
     * Returns null when the entity satisfies the restrictions.
     */
    public String whyCannotApply(MobEntity entity) {
        if (!allowedTypes.isEmpty() && !allowedTypes.contains(typeId(entity))) {
            return "Entity type is not in the allowed list";
        }

        if (!excludedTypes.isEmpty() && excludedTypes.contains(typeId(entity))) {
            return "Entity type is explicitly excluded";
        }

        if (requiresWater && !entity.isTouchingWater()) {
            return "Requires water to apply";
        }

        return null;
    }
}
