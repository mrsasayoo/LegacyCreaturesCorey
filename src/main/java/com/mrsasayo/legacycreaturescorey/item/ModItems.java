package com.mrsasayo.legacycreaturescorey.item;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    
    private static final Identifier EXAMPLE_ITEM_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "example_item");
    private static final RegistryKey<Item> EXAMPLE_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, EXAMPLE_ITEM_ID);

    public static final Item EXAMPLE_ITEM = Registry.register(
        Registries.ITEM,
        EXAMPLE_ITEM_KEY,
        new FollowerItem(new Item.Settings().registryKey(EXAMPLE_ITEM_KEY))
    );
    
    public static void initialize() {
        Legacycreaturescorey.LOGGER.info("✅ Ítems registrados");
    }
}