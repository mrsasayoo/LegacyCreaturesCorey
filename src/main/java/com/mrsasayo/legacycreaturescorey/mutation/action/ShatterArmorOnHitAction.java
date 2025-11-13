package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class ShatterArmorOnHitAction extends ProcOnHitAction {
    private static final RegistryEntry<EntityAttribute> ARMOR = EntityAttributes.ARMOR;

    private final double percent;
    private final int duration;

    public ShatterArmorOnHitAction(double chance, double percent, int durationTicks) {
        super(chance);
        this.percent = Math.min(0.0D, -Math.abs(percent));
        this.duration = Math.max(1, durationTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        EntityAttributeInstance instance = victim.getAttributeInstance(ARMOR);
        if (instance == null) {
            return;
        }

        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "shatter_armor_" + UUID.randomUUID().toString().replace("-", ""));
        EntityAttributeModifier modifier = new EntityAttributeModifier(id, percent, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        instance.addTemporaryModifier(modifier);

        OnHitTaskScheduler.schedule(world, new RemoveModifierTask(victim, ARMOR, id, duration));
    }

    private static final class RemoveModifierTask implements OnHitTaskScheduler.TimedTask {
        private final LivingEntity entity;
        private final RegistryEntry<EntityAttribute> attribute;
        private final Identifier modifierId;
        private int ticksLeft;

        private RemoveModifierTask(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, Identifier modifierId, int ticks) {
            this.entity = entity;
            this.attribute = attribute;
            this.modifierId = modifierId;
            this.ticksLeft = ticks;
        }

        @Override
        public boolean tick(ServerWorld world) {
            if (entity.isRemoved() || !entity.isAlive()) {
                return true;
            }
            if (--ticksLeft > 0) {
                return false;
            }
            EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifierId);
            }
            return true;
        }
    }
}
