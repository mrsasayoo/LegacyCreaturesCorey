package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DamageArmorOnHitAction extends ProcOnHitAction {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD,
		EquipmentSlot.CHEST,
		EquipmentSlot.LEGS,
		EquipmentSlot.FEET
	};
	private final int damageAmount;

	public DamageArmorOnHitAction(double chance, int damageAmount) {
		super(chance);
		if (damageAmount <= 0) {
			throw new IllegalArgumentException("El danio a la armadura debe ser mayor a cero");
		}
		this.damageAmount = damageAmount;
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
