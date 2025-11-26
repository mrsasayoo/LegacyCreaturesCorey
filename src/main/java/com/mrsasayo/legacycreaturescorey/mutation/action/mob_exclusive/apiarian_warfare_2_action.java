package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;

public final class apiarian_warfare_2_action extends apiarian_warfare_base_action {
    private final double chance;
    private final int honeyDurationTicks;
    private final int hurtTimeTrigger;

    public apiarian_warfare_2_action(mutation_action_config config) {
        this.chance = config.getDouble("chance", 0.20D);
        this.honeyDurationTicks = config.getInt("honey_duration_ticks", 120);
        this.hurtTimeTrigger = config.getInt("hurt_time_trigger", 9);
    }

    @Override
    public void onTick(LivingEntity entity) {
        BeeEntity bee = asServerBee(entity);
        if (bee == null) {
            return;
        }
        Handler handler = handler();
        if (bee.hurtTime == hurtTimeTrigger) {
            handler.trySpawnHoneyTrap(bee, chance, honeyDurationTicks);
        }
    }
}
