package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.util.Identifier;

abstract class arctic_fortitude_base_action implements mutation_action {
    protected PolarBearEntity asServerPolarBear(LivingEntity entity) {
        if (entity instanceof PolarBearEntity bear && !entity.getEntityWorld().isClient()) {
            return bear;
        }
        return null;
    }

    protected Identifier id(String path) {
        return Identifier.of(Legacycreaturescorey.MOD_ID, path);
    }

    protected void applyModifier(LivingEntity entity,
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
            Identifier identifier,
            double value,
            EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null && !instance.hasModifier(identifier)) {
            instance.addPersistentModifier(new EntityAttributeModifier(identifier, value, operation));
        }
    }

    protected void removeModifier(LivingEntity entity,
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
            Identifier identifier) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null && instance.hasModifier(identifier)) {
            instance.removeModifier(identifier);
        }
    }

    protected boolean isIceOrSnow(BlockState state) {
        return state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE)
                || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.SNOW) || state.isOf(Blocks.POWDER_SNOW);
    }

    protected void applySpeedBuff(PolarBearEntity bear, int durationTicks, int amplifier) {
        bear.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, durationTicks, amplifier, false, false));
    }
}
