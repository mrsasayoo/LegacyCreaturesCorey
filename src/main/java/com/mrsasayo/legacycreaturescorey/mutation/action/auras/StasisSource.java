package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public interface StasisSource {
    Mode getMode();

    double getRadius();

    double getProjectileSlowFactor();

    double getAttackSpeedMultiplier();

    int getShieldCooldownTicks();

    EntityAttributeModifier getAttackSpeedModifier();

    Identifier getAttackSpeedModifierId();

    enum Mode {
        PROJECTILE_DAMPEN,
        SPRINT_SUPPRESSION,
        TELEPORT_ANCHOR
    }
}
