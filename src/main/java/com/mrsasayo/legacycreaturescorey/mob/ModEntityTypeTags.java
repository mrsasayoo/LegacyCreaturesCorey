package com.mrsasayo.legacycreaturescorey.mob;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModEntityTypeTags {

    public static final TagKey<EntityType<?>> TIER_LEVE = create("tier_leve");
    public static final TagKey<EntityType<?>> TIER_BASIC = create("tier_basic");
    public static final TagKey<EntityType<?>> TIER_INTERMEDIATE = create("tier_intermediate");
    public static final TagKey<EntityType<?>> TIER_HARD = create("tier_hard");

    private ModEntityTypeTags() {
    }

    private static TagKey<EntityType<?>> create(String name) {
        return TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Legacycreaturescorey.MOD_ID, name));
    }
}
