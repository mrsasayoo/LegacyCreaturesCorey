package com.mrsasayo.legacycreaturescorey.mutation.util;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Acción de mutación que modifica atributos de entidades.
 */
public final class attribute_mutation_action implements mutation_action {
    private final double amount;
    private final Holder<Attribute> attributeEntry;
    private final Attribute attributeType;
    private final Identifier modifierId;
    private final AttributeModifier.Operation operation;

    public attribute_mutation_action(Identifier attributeId, Mode mode, double amount) {
        this(attributeId, mode, amount, null);
    }

    public attribute_mutation_action(Identifier attributeId,
            Mode mode,
            double amount,
            Identifier preferredId) {
        this.amount = amount;
        Attribute resolved = null;
        Holder<Attribute> entry = null;
        if (BuiltInRegistries.ATTRIBUTE.containsId(attributeId)) {
            resolved = BuiltInRegistries.ATTRIBUTE.get(attributeId);
            if (resolved != null) {
                entry = BuiltInRegistries.ATTRIBUTE.getEntry(resolved);
            }
        }
        this.attributeEntry = entry;
        this.attributeType = resolved;
        this.operation = mode == Mode.ADD
                ? AttributeModifier.Operation.ADD_VALUE
            : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        this.modifierId = preferredId != null
            ? preferredId
            : buildModifierId(attributeId, mode, amount);
    }

    @Override
    public void onApply(LivingEntity entity) {
        AttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }
        removeModifier(instance);
        double before = instance.getValue();
        AttributeModifier modifier = new AttributeModifier(modifierId, amount, operation);
        instance.addPersistentModifier(modifier);
        handleMaxHealthAdjustment(entity, before, instance.getValue());
    }

    @Override
    public void onRemove(LivingEntity entity) {
        AttributeInstance instance = resolveInstance(entity);
        if (instance == null) {
            return;
        }
        double before = instance.getValue();
        removeModifier(instance);
        double after = instance.getValue();
        if (attributeType == Attributes.MAX_HEALTH && after < before) {
            entity.setHealth((float) Math.min(entity.getHealth(), after));
        }
    }

    private AttributeInstance resolveInstance(LivingEntity entity) {
        return attributeEntry == null ? null : entity.getAttributeInstance(attributeEntry);
    }

    private void removeModifier(AttributeInstance instance) {
        if (instance.hasModifier(modifierId)) {
            instance.removeModifier(modifierId);
        }
    }

    private void handleMaxHealthAdjustment(LivingEntity entity, double before, double after) {
        if (attributeType != Attributes.MAX_HEALTH) {
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
        String suffix = mode.name().toLowerCase(Locale.WRITABLE_REGISTRY) + "_" + bits;
        return Identifier.fromNamespaceAndPath(namespace, "passive_" + attrPath + "_" + suffix);
    }

    public enum Mode {
        ADD,
        MULTIPLY;

        public static Mode fromString(String raw) {
            return valueOf(raw.trim().toUpperCase(Locale.WRITABLE_REGISTRY));
        }
    }
}
