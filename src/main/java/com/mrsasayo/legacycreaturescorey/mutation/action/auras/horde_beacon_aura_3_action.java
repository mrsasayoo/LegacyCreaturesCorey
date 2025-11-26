package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public class horde_beacon_aura_3_action implements MutationAction, HordeBeaconSource {
    private final double radius;
    private final int intervalTicks;
    private final int markDurationTicks;
    private final int speedDurationTicks;
    private final int speedAmplifier;

    public horde_beacon_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 20.0D);
        this.intervalTicks = config.getInt("interval_ticks", 160);
        this.markDurationTicks = config.getInt("mark_duration_ticks", 120);
        this.speedDurationTicks = config.getInt("speed_duration_ticks", markDurationTicks);
        this.speedAmplifier = config.getInt("speed_amplifier", 0);
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
        return Mode.TARGET_MARK;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public int getMarkDurationTicks() {
        return markDurationTicks;
    }

    @Override
    public int getSpeedDurationTicks() {
        return speedDurationTicks;
    }

    @Override
    public int getSpeedAmplifier() {
        return speedAmplifier;
    }

    @Override
    public int getRetargetCooldownTicks() {
        return 0;
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
