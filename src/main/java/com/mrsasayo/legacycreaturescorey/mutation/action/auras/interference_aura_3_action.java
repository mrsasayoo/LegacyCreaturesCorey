package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

public class interference_aura_3_action implements MutationAction, InterferenceSource {
    private final double radius;
    private final double chance;
    private final float pearlDamage;

    public interference_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 8.0D);
        this.chance = config.getDouble("chance", 1.0D);
        this.pearlDamage = (float) config.getDouble("pearl_damage", 5.0D);
        InterferenceHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        InterferenceHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        InterferenceHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.PEARL_NEGATION;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getChance() {
        return chance;
    }

    @Override
    public float getPearlDamage() {
        return pearlDamage;
    }
}
