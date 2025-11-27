package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

abstract class shatter_armor_base_action extends proc_on_hit_action {
    private static final RegistryEntry<EntityAttribute> ARMOR = EntityAttributes.ARMOR;

    private final double percentModifier;
    private final int durationTicks;

    protected shatter_armor_base_action(mutation_action_config config,
            double defaultChance,
            double defaultReductionPercent,
            int defaultDurationSeconds) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        double reduction = config.getDouble("reduction_percent", defaultReductionPercent);
        this.percentModifier = -Math.abs(reduction);
        this.durationTicks = resolveDurationTicks(config, defaultDurationSeconds * 20);
    }

    private int resolveDurationTicks(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt("duration_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(1, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        EntityAttributeInstance instance = victim.getAttributeInstance(ARMOR);
        if (instance == null || percentModifier == 0.0D) {
            return;
        }
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "shatter_armor_" + UUID.randomUUID().toString().replace("-", ""));
        EntityAttributeModifier modifier = new EntityAttributeModifier(id, percentModifier, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        instance.addTemporaryModifier(modifier);
        playFeedback(world, victim);
        mutation_task_scheduler.schedule(world, new RemoveModifierTask(victim, ARMOR, id, durationTicks));
    }

    private void playFeedback(ServerWorld world, LivingEntity victim) {
        float pitch = 0.8F + world.getRandom().nextFloat() * 0.2F;
        double centerY = victim.getY() + victim.getStandingEyeHeight() * 0.5D;
        world.playSound(null, victim.getX(), centerY, victim.getZ(), SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 0.9F, pitch);
        world.spawnParticles(ParticleTypes.CRIT, victim.getX(), centerY, victim.getZ(), 8, 0.35D, 0.25D, 0.35D, 0.05D);
        world.spawnParticles(ParticleTypes.SMOKE, victim.getX(), centerY, victim.getZ(), 6, 0.25D, 0.2D, 0.25D, 0.01D);
    }

    private static final class RemoveModifierTask implements mutation_task_scheduler.TimedTask {
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
