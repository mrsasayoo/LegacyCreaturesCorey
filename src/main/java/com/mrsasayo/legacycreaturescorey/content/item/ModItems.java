package com.mrsasayo.legacycreaturescorey.content.item;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the custom items required for bespoke mutation mechanics.
 */
public final class ModItems {
    // Declarar sin inicializar (se asigna en init())
    public static Item GHOST_FRAGMENT;

    private ModItems() {
    }

    public static void init() {
        // Minecraft 1.21+ requiere ResourceKey en Settings antes de la construcción
        ResourceLocation id = ResourceLocation.registerSimple(Legacycreaturescorey.MOD_ID, "ghost_fragment");
        ResourceKey<Item> key = ResourceKey.registerSimple(RegistryKeys.ITEM, id);
        GHOST_FRAGMENT = Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new GhostFragmentItem(new Item.Settings().registryKey(key).maxCount(16)));

        Legacycreaturescorey.LOGGER.debug("Items inicializados para Último Aliento");
    }
}
