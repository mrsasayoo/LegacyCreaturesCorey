package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.attack_downtime_manager;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * anti_shield_3 «Juramento Roto»: no solo bloquea el escudo 4 s — lo DESTROUYE:
 * 50 de durabilidad de daño directo al ítem con sonido de grieta. El coste es que
 * el mob queda 1 s sin atacar (Fatiga IV, mismo mecanismo de la familia).
 */
public final class anti_shield_3_action extends anti_shield_base_action {
    public anti_shield_3_action(mutation_action_config config) {
        super(config, 0.15D, 80);
    }

    @Override
    protected void afterProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof Player player) || !(player.level() instanceof ServerLevel world)) {
            return;
        }

        ItemStack shield = player.getOffhandItem();
        EquipmentSlot slot = EquipmentSlot.OFFHAND;
        if (!shield.is(Items.SHIELD)) {
            shield = player.getMainHandItem();
            slot = EquipmentSlot.MAINHAND;
        }
        if (shield.is(Items.SHIELD)) {
            shield.hurtAndBreak(25, player, slot);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BREAK.value(), SoundSource.HOSTILE, 0.9F, 0.7F);
        }

        // Coste del juramento: el mob queda 1 s sin poder atacar (bloqueo real, no Fatiga).
        attack_downtime_manager.apply(attacker, 20);
    }
}
