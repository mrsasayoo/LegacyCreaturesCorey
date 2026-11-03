package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;

public interface StasisSource {
    Mode getMode();

    double getRadius();

    double getProjectileSlowFactor();

    double getAttackSpeedMultiplier();

    int getShieldCooldownTicks();

    AttributeModifier getAttackSpeedModifier();

    Identifier getAttackSpeedModifierId();

    enum Mode {
        PROJECTILE_DAMPEN,
        SPRINT_SUPPRESSION,
        TELEPORT_ANCHOR
    }
}
