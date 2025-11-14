package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class AttributeMutationAction implements MutationAction {
    private final Mode mode;
    private final double amount;
    private final RegistryEntry<EntityAttribute> attributeEntry;
    private final EntityAttribute attributeType;

    public AttributeMutationAction(Identifier attributeId, Mode mode, double amount) {
        this.mode = mode;
        this.amount = amount;
        if (Registries.ATTRIBUTE.containsId(attributeId)) {
            EntityAttribute resolved = Registries.ATTRIBUTE.get(attributeId);
            if (resolved != null) {
                this.attributeEntry = Registries.ATTRIBUTE.getEntry(resolved);
                this.attributeType = resolved;
                return;
            }
        }
        this.attributeEntry = null;
        this.attributeType = null;
    }

    @Override
    public void onApply(LivingEntity entity) {
        EntityAttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }

        double previousBase = instance.getBaseValue();
        double newBase = previousBase;
        switch (mode) {
            case ADD -> newBase = previousBase + amount;
            case MULTIPLY -> newBase = previousBase * (1.0D + amount);
        }
        instance.setBaseValue(newBase);

        if (attributeType == EntityAttributes.MAX_HEALTH) {
            entity.setHealth((float) Math.min(entity.getMaxHealth(), entity.getHealth() + Math.max(0.0D, newBase - previousBase)));
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        EntityAttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }

        double previousBase = instance.getBaseValue();
        double newBase = previousBase;
        switch (mode) {
            case ADD -> newBase = previousBase - amount;
            case MULTIPLY -> {
                double factor = 1.0D + amount;
                if (factor != 0.0D) {
                    newBase = previousBase / factor;
                }
            }
        }
        instance.setBaseValue(newBase);

        if (attributeType == EntityAttributes.MAX_HEALTH) {
            entity.setHealth((float) Math.min(entity.getHealth(), entity.getMaxHealth()));
        }
    }

    private EntityAttributeInstance resolveInstance(LivingEntity entity) {
        return attributeEntry == null ? null : entity.getAttributeInstance(attributeEntry);
    }

    public enum Mode {
        ADD,
        MULTIPLY;

        public static Mode fromString(String raw) {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
    }
}
