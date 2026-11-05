package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * «Coloso» (size_N) — tres atributos a la vez: escala física (MULTIPLY sobre
 * minecraft:scale), vida máxima y armadura extra. COMPATIBLE con las familias
 * armor_* y max_health_* (solo es incompatible con sus propios hermanos de nivel).
 */
abstract class size_base_action implements mutation_action {
    private final attribute_mutation_action scaleDelegate;
    private final attribute_mutation_action healthDelegate;
    private final attribute_mutation_action armorDelegate;

    protected size_base_action(mutation_action_config config,
            String id,
            double defaultScale,
            double defaultHealth,
            double defaultArmor) {
        Identifier base = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/" + id);
        this.scaleDelegate = new attribute_mutation_action(passive_attribute_ids.SCALE,
                attribute_mutation_action.Mode.MULTIPLY,
                config.getDouble("scale", defaultScale),
                Identifier.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "/scale"));
        this.healthDelegate = new attribute_mutation_action(passive_attribute_ids.MAX_HEALTH,
                attribute_mutation_action.Mode.ADD,
                config.getDouble("health_bonus", defaultHealth),
                Identifier.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "/health"));
        this.armorDelegate = new attribute_mutation_action(passive_attribute_ids.ARMOR,
                attribute_mutation_action.Mode.ADD,
                config.getDouble("armor_bonus", defaultArmor),
                Identifier.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "/armor"));
    }

    @Override
    public void onApply(LivingEntity entity) {
        scaleDelegate.onApply(entity);
        healthDelegate.onApply(entity);
        armorDelegate.onApply(entity);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        scaleDelegate.onRemove(entity);
        healthDelegate.onRemove(entity);
        armorDelegate.onRemove(entity);
    }
}
