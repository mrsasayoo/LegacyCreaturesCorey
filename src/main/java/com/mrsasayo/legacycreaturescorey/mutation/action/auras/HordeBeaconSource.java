package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public interface HordeBeaconSource {
    Mode getMode();

    double getRadius();

    int getIntervalTicks();

    int getMarkDurationTicks();

    int getSpeedDurationTicks();

    int getSpeedAmplifier();

    int getRetargetCooldownTicks();

    EntityAttributeModifier getFollowRangeModifier();

    Identifier getFollowRangeModifierId();

    enum Mode {
        FOLLOW_RANGE_BOOST,
        FEAR_OVERRIDE,
        TARGET_MARK
    }
}
