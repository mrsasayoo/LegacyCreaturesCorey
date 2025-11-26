package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import java.util.List;

public interface PsionicThornsSource {
    double getReflectionPercentage();

    double getMaxDistance();

    List<ThornsEffect> getEffects();

    List<ThornsEffect> getSelfEffects();

    record ThornsEffect(RegistryEntry<StatusEffect> effect, int duration, int amplifier) {
    }
}
