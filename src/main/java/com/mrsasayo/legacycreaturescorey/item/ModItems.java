package com.mrsasayo.legacycreaturescorey.item;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    
    public static final Item EXAMPLE_ITEM = Registry.register(
        Registries.ITEM,
        // CORRECCIÓN: usar Identifier.of()
        Identifier.of(Legacycreaturescorey.MOD_ID, "example_item"),
        new FollowerItem(new Item.Settings())
    );
    
    public static void initialize() {
        Legacycreaturescorey.LOGGER.info("✅ Ítems registrados");
    }
}