package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 26.3: yarn PersistentProjectileEntity -> mojmap AbstractArrow; asItemStack -> getPickupItemStackOrigin;
 * campo damage -> baseDamage
 */
@Mixin(AbstractArrow.class)
public interface persistent_projectile_entity_accessor {
    @Invoker("getPickupItemStackOrigin")
    ItemStack legacycreaturescorey$invokeAsItemStack();

    @Accessor("baseDamage")
    double legacycreaturescorey$getBaseDamage();
}
