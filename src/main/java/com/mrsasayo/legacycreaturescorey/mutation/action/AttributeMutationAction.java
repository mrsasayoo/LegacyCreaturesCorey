package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class AttributeMutationAction implements MutationAction {
    private final double amount;
    private final RegistryEntry<EntityAttribute> attributeEntry;
    private final EntityAttribute attributeType;
    private final Identifier modifierId;
    private final EntityAttributeModifier.Operation operation;

    public AttributeMutationAction(Identifier attributeId, Mode mode, double amount) {
        this(attributeId, mode, amount, null);
    }

    public AttributeMutationAction(Identifier attributeId,
            Mode mode,
            double amount,
            Identifier preferredId) {
        this.amount = amount;
        EntityAttribute resolved = null;
        RegistryEntry<EntityAttribute> entry = null;
        if (Registries.ATTRIBUTE.containsId(attributeId)) {
            resolved = Registries.ATTRIBUTE.get(attributeId);
            if (resolved != null) {
                entry = Registries.ATTRIBUTE.getEntry(resolved);
            }
        }
        this.attributeEntry = entry;
        this.attributeType = resolved;
        this.operation = mode == Mode.ADD
                ? EntityAttributeModifier.Operation.ADD_VALUE
            : EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        this.modifierId = preferredId != null
            ? preferredId
            : buildModifierId(attributeId, mode, amount);
    }

    @Override
    public void onApply(LivingEntity entity) {
        EntityAttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }
        removeModifier(instance);
        double before = instance.getValue();
        EntityAttributeModifier modifier = new EntityAttributeModifier(modifierId, amount, operation);
        instance.addPersistentModifier(modifier);
        handleMaxHealthAdjustment(entity, before, instance.getValue());
    }

    @Override
    public void onRemove(LivingEntity entity) {
        EntityAttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }
        double before = instance.getValue();
        removeModifier(instance);
        double after = instance.getValue();
        if (attributeType == EntityAttributes.MAX_HEALTH && after < before) {
            entity.setHealth((float) Math.min(entity.getHealth(), after));
        }
    }

    private EntityAttributeInstance resolveInstance(LivingEntity entity) {
        return attributeEntry == null ? null : entity.getAttributeInstance(attributeEntry);
    }

    private void removeModifier(EntityAttributeInstance instance) {
        if (instance.hasModifier(modifierId)) {
            instance.removeModifier(modifierId);
        }
    }

    private void handleMaxHealthAdjustment(LivingEntity entity, double before, double after) {
        if (attributeType != EntityAttributes.MAX_HEALTH) {
            return;
        }
        if (after > before) {
            entity.setHealth((float) Math.min(entity.getHealth() + (after - before), after));
        } else {
            entity.setHealth((float) Math.min(entity.getHealth(), after));
        }
    }

    private Identifier buildModifierId(Identifier attributeId, Mode mode, double amount) {
        String namespace = Legacycreaturescorey.MOD_ID;
        String attrPath = attributeId.getNamespace() + "_" + attributeId.getPath().replace('.', '_');
        String bits = Long.toHexString(Double.doubleToLongBits(amount));
        String suffix = mode.name().toLowerCase(Locale.ROOT) + "_" + bits;
        return Identifier.of(namespace, "passive_" + attrPath + "_" + suffix);
    }

    public enum Mode {
        ADD,
        MULTIPLY;

        public static Mode fromString(String raw) {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
    }
}
