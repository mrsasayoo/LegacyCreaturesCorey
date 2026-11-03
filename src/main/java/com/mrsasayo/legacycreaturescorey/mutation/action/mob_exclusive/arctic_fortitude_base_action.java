package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import net.minecraft.core.Holder;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.resources.Identifier;

abstract class arctic_fortitude_base_action implements mutation_action {
    protected PolarBear asServerPolarBear(LivingEntity entity) {
        if (entity instanceof PolarBear bear && !entity.level().isClientSide()) {
            return bear;
        }
        return null;
    }

    protected Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, path);
    }

    protected void applyModifier(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier identifier,
            double value,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(identifier)) {
            instance.addPermanentModifier(new AttributeModifier(identifier, value, operation));
        }
    }

    protected void removeModifier(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier identifier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.hasModifier(identifier)) {
            instance.removeModifier(identifier);
        }
    }

    protected boolean isIceOrSnow(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW);
    }

    protected void applySpeedBuff(PolarBear bear, int durationTicks, int amplifier) {
        bear.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, amplifier, false, false));
    }
}
