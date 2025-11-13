package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class AttributeMutationAction implements MutationAction {
    private final Identifier attributeId;
    private final Mode mode;
    private final double amount;

    public AttributeMutationAction(Identifier attributeId, Mode mode, double amount) {
        this.attributeId = attributeId;
        this.mode = mode;
        this.amount = amount;
    }

    @Override
    public void onApply(LivingEntity entity) {
        EntityAttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }

        switch (mode) {
            case ADD -> instance.setBaseValue(instance.getBaseValue() + amount);
            case MULTIPLY -> instance.setBaseValue(instance.getBaseValue() * (1.0D + amount));
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        EntityAttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }

        switch (mode) {
            case ADD -> instance.setBaseValue(instance.getBaseValue() - amount);
            case MULTIPLY -> {
                double factor = 1.0D + amount;
                if (factor != 0.0D) {
                    instance.setBaseValue(instance.getBaseValue() / factor);
                }
            }
        }
    }

    private EntityAttributeInstance resolveInstance(LivingEntity entity) {
    RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(attributeId).orElse(null);
        return entry == null ? null : entity.getAttributeInstance(entry);
    }

    public enum Mode {
        ADD,
        MULTIPLY;

        public static Mode fromString(String raw) {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
    }
}
