package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

public class entropy_aura_2_action implements MutationAction {
    private final double radius;
    private final int intervalTicks;
    private final int durabilityDamage;

    public entropy_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 5.0D);
        this.intervalTicks = config.getInt("interval_ticks", 80);
        this.durabilityDamage = Math.max(1, config.getInt("durability_damage", 1));
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity))
            return;
        if (intervalTicks > 0 && entity.age % intervalTicks != 0)
            return;

        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        for (PlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.squaredDistanceTo(entity) <= radius * radius) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                        ItemStack stack = player.getEquippedStack(slot);
                        if (!stack.isEmpty() && stack.isDamageable()) {
                            stack.damage(durabilityDamage, player, slot);
                        }
                    }
                }
            }
        }
    }
}
