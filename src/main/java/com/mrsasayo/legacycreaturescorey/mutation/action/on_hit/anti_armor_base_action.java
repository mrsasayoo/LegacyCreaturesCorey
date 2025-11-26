package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

abstract class anti_armor_base_action extends ProcOnHitAction {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final int damageAmount;

    protected anti_armor_base_action(mutation_action_config config,
            double defaultChance,
            int defaultDamage) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.damageAmount = Math.max(1, config.getInt("damage", defaultDamage));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        List<EquipmentSlot> candidates = new ArrayList<>(ARMOR_SLOTS.length);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = victim.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable()) {
                continue;
            }
            candidates.add(slot);
        }

        if (candidates.isEmpty()) {
            return;
        }

        EquipmentSlot selected = candidates.get(attacker.getRandom().nextInt(candidates.size()));
        ItemStack stack = victim.getEquippedStack(selected);
        if (stack.isEmpty() || !stack.isDamageable()) {
            return;
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }

        int newDamage = stack.getDamage() + damageAmount;
        if (newDamage >= maxDamage) {
            victim.equipStack(selected, ItemStack.EMPTY);
        } else {
            stack.setDamage(newDamage);
        }
    }
}
