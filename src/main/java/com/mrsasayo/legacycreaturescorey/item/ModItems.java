package com.mrsasayo.legacycreaturescorey.item;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers the custom items required for bespoke mutation mechanics.
 */
public final class ModItems {
    public static final Item GHOST_FRAGMENT = register("ghost_fragment",
        new GhostFragmentItem(new Item.Settings().maxCount(16)));

    private ModItems() {}

    public static void init() {
        Legacycreaturescorey.LOGGER.debug("Items inicializados para Último Aliento");
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Legacycreaturescorey.MOD_ID, name), item);
    }
}
