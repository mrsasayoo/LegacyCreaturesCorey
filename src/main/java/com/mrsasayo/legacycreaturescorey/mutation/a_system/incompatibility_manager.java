package com.mrsasayo.legacycreaturescorey.mutation.a_system;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Define restricciones sobre qué tipos de entidades pueden recibir una mutación.
 */
public final class incompatibility_manager {
    private final Set<Identifier> allowedTypes;
    private final Set<Identifier> excludedTypes;
    private final Set<TagKey<EntityType<?>>> allowedTags;
    private final Set<TagKey<EntityType<?>>> excludedTags;
    private final boolean requiresWater;

    public incompatibility_manager(Set<Identifier> allowedTypes, Set<Identifier> excludedTypes, boolean requiresWater) {
        this(allowedTypes, excludedTypes, Collections.emptySet(), Collections.emptySet(), requiresWater);
    }

    public incompatibility_manager(Set<Identifier> allowedTypes,
                                   Set<Identifier> excludedTypes,
                                   Set<TagKey<EntityType<?>>> allowedTags,
                                   Set<TagKey<EntityType<?>>> excludedTags,
                                   boolean requiresWater) {
        this.allowedTypes = allowedTypes == null ? Collections.emptySet() : Set.copyOf(allowedTypes);
        this.excludedTypes = excludedTypes == null ? Collections.emptySet() : Set.copyOf(excludedTypes);
        this.allowedTags = allowedTags == null ? Collections.emptySet() : Set.copyOf(allowedTags);
        this.excludedTags = excludedTags == null ? Collections.emptySet() : Set.copyOf(excludedTags);
        this.requiresWater = requiresWater;
    }

    public boolean canApply(MobEntity entity) {
        if (!allowedTypes.isEmpty() || !allowedTags.isEmpty()) {
            boolean matchesId = allowedTypes.contains(typeId(entity));
            boolean matchesTag = allowedTags.stream().anyMatch(tag -> entity.getType().isIn(tag));
            if (!matchesId && !matchesTag) {
                return false;
            }
        }

        if (!excludedTypes.isEmpty() && excludedTypes.contains(typeId(entity))) {
            return false;
        }

        if (!excludedTags.isEmpty() && excludedTags.stream().anyMatch(tag -> entity.getType().isIn(tag))) {
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

    public static incompatibility_manager empty() {
        return new incompatibility_manager(new HashSet<>(), new HashSet<>(), false);
    }

    /**
     * If the restriction disallows application, returns a short reason string.
     * Returns null when the entity satisfies the restrictions.
     */
    public String whyCannotApply(MobEntity entity) {
        if (!allowedTypes.isEmpty() || !allowedTags.isEmpty()) {
            boolean matchesId = allowedTypes.contains(typeId(entity));
            boolean matchesTag = allowedTags.stream().anyMatch(tag -> entity.getType().isIn(tag));
            if (!matchesId && !matchesTag) {
                return "Entity type is not included in the allowed filters";
            }
        }

        if (!excludedTypes.isEmpty() && excludedTypes.contains(typeId(entity))) {
            return "Entity type is explicitly excluded";
        }

        if (!excludedTags.isEmpty() && excludedTags.stream().anyMatch(tag -> entity.getType().isIn(tag))) {
            return "Entity type is excluded by tag";
        }

        if (requiresWater && !entity.isTouchingWater()) {
            return "Requires water to apply";
        }

        return null;
    }
}
