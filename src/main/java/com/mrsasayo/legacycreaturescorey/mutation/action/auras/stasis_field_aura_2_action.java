package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public class stasis_field_aura_2_action implements MutationAction, StasisSource {
    private final double radius;
    private final int shieldCooldownTicks;
    private final double attackSpeedMultiplier;
    private final Identifier attackSpeedModifierId;
    private final EntityAttributeModifier attackSpeedModifier;

    public stasis_field_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 6.0D);
        this.shieldCooldownTicks = Math.max(0, config.getInt("shield_cooldown_ticks", 100));
        this.attackSpeedMultiplier = Math.max(0.0D, config.getDouble("attack_speed_multiplier", 0.8D));
        this.attackSpeedModifierId = resolveModifierId(config, "stasis_field_sprint_suppression");
        this.attackSpeedModifier = new EntityAttributeModifier(
                attackSpeedModifierId,
                attackSpeedMultiplier - 1.0D,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        StasisHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        StasisHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        StasisHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.SPRINT_SUPPRESSION;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getProjectileSlowFactor() {
        return 1.0;
    }

    @Override
    public double getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }

    @Override
    public int getShieldCooldownTicks() {
        return shieldCooldownTicks;
    }

    @Override
    public EntityAttributeModifier getAttackSpeedModifier() {
        return attackSpeedModifier;
    }

    @Override
    public Identifier getAttackSpeedModifierId() {
        return attackSpeedModifierId;
    }

    private Identifier resolveModifierId(mutation_action_config config, String prefix) {
        String raw = config.getString("modifier_id", "").trim();
        if (!raw.isEmpty()) {
            Identifier parsed = Identifier.tryParse(raw);
            if (parsed != null) {
                return parsed;
            }
        }
        String key = prefix + "_" + Integer.toHexString(Double.hashCode(radius + attackSpeedMultiplier));
        return Identifier.of("legacycreaturescorey", key);
    }
}
