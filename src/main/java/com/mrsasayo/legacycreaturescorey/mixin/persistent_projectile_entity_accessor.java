package com.mrsasayo.legacycreaturescorey.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface persistent_projectile_entity_accessor {
    @Invoker("asItemStack")
    ItemStack legacycreaturescorey$invokeAsItemStack();

    @Accessor("damage")
    double legacycreaturescorey$getBaseDamage();
}
