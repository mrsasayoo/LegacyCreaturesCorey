package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.server.level.ServerPlayer;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

public class entropy_aura_2_action implements mutation_action {
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
        if (!action_context.isServer(entity))
            return;
        if (intervalTicks > 0 && entity.tickCount % intervalTicks != 0)
            return;

        ServerLevel world = (ServerLevel) entity.level();
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.distanceToSqr(entity) <= radius * radius) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                        ItemStack stack = player.getItemBySlot(slot);
                        if (!stack.isEmpty() && stack.isDamageableItem()) {
                            stack.hurtAndBreak(durabilityDamage, ((ServerLevel) player.level()), (ServerPlayer) player, (brokenStack) -> {});
                        }
                    }
                }
            }
        }
    }
}
