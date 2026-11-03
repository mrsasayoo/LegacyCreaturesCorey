package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;

public interface HordeBeaconSource {
    Mode getMode();

    double getRadius();

    int getIntervalTicks();

    int getMarkDurationTicks();

    int getSpeedDurationTicks();

    int getSpeedAmplifier();

    int getRetargetCooldownTicks();

    AttributeModifier getFollowRangeModifier();

    Identifier getFollowRangeModifierId();

    enum Mode {
        FOLLOW_RANGE_BOOST,
        FEAR_OVERRIDE,
        TARGET_MARK
    }
}
