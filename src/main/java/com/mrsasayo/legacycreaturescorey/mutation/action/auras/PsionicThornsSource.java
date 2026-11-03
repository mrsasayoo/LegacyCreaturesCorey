package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import java.util.List;

public interface PsionicThornsSource {
    double getReflectionPercentage();

    double getMaxDistance();

    List<ThornsEffect> getEffects();

    List<ThornsEffect> getSelfEffects();

    record ThornsEffect(Holder<MobEffect> effect, int duration, int amplifier) {
    }
}
