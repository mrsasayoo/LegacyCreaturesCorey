package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public class horde_beacon_aura_1_action implements mutation_action, HordeBeaconSource {
    private final double radius;
    private final Identifier followRangeModifierId;
    private final EntityAttributeModifier followRangeModifier;

    public horde_beacon_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 16.0D);
        double followRangeBonus = config.getDouble("follow_range_bonus", 0.5D);
        String key = "horde_beacon_follow_range_"
                + Integer.toHexString(Double.hashCode(radius + followRangeBonus));
        this.followRangeModifierId = Identifier.of("legacycreaturescorey", key);
        this.followRangeModifier = new EntityAttributeModifier(followRangeModifierId, followRangeBonus,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        HordeBeaconHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        HordeBeaconHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        HordeBeaconHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.FOLLOW_RANGE_BOOST;
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
        return 0;
    }

    @Override
    public EntityAttributeModifier getFollowRangeModifier() {
        return followRangeModifier;
    }

    @Override
    public Identifier getFollowRangeModifierId() {
        return followRangeModifierId;
    }
}
