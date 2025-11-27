package com.mrsasayo.legacycreaturescorey.content.item;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Registers the custom items required for bespoke mutation mechanics.
 */
public final class ModItems {
    // Declarar sin inicializar (se asigna en init())
    public static Item GHOST_FRAGMENT;

    private ModItems() {
    }

    public static void init() {
        // Minecraft 1.21+ requiere RegistryKey en Settings antes de la construcción
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "ghost_fragment");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        GHOST_FRAGMENT = Registry.register(
                Registries.ITEM,
                id,
                new GhostFragmentItem(new Item.Settings().registryKey(key).maxCount(16)));

        Legacycreaturescorey.LOGGER.debug("Items inicializados para Último Aliento");
    }
}
