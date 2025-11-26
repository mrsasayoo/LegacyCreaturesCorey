package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public class horde_beacon_aura_2_action implements MutationAction, HordeBeaconSource {
    private final double radius;
    private final int retargetCooldownTicks;

    public horde_beacon_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 12.0D);
        this.retargetCooldownTicks = config.getInt("retarget_cooldown_ticks", 100);
        HordeBeaconHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        HordeBeaconHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        HordeBeaconHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.FEAR_OVERRIDE;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getIntervalTicks() {
        return 0;
    }

    @Override
    public int getMarkDurationTicks() {
        return 0;
    }

    @Override
    public int getSpeedDurationTicks() {
        return 0;
    }

    @Override
    public int getSpeedAmplifier() {
        return 0;
    }

    @Override
    public int getRetargetCooldownTicks() {
        return retargetCooldownTicks;
    }

    @Override
    public EntityAttributeModifier getFollowRangeModifier() {
        return null;
    }

    @Override
    public Identifier getFollowRangeModifierId() {
        return null;
    }
}
