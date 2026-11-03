package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.UUID;

abstract class shatter_armor_base_action extends proc_on_hit_action {
    private static final Holder<Attribute> ARMOR = Attributes.ARMOR;

    private final double percentModifier;
    private final int durationTicks;

    protected shatter_armor_base_action(mutation_action_config config,
            double defaultChance,
            double defaultReductionPercent,
            int defaultDurationSeconds) {
        super(Mth.sanitizeValue(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
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
        if (!(victim.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        AttributeInstance instance = victim.getAttribute(ARMOR);
        if (instance == null || percentModifier == 0.0D) {
            return;
        }
        ResourceLocation id = ResourceLocation.direct(Legacycreaturescorey.MOD_ID, "shatter_armor_" + UUID.randomUUID().toString().replace("-", ""));
        AttributeModifier modifier = new AttributeModifier(id, percentModifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        instance.addTransientModifier(modifier);
        playFeedback(world, victim);
        mutation_task_scheduler.schedule(world, new RemoveModifierTask(victim, ARMOR, id, durationTicks));
    }

    private void playFeedback(ServerLevel world, LivingEntity victim) {
        float pitch = 0.8F + world.getRandom().nextFloat() * 0.2F;
        double centerY = victim.getY() + victim.getStandingEyeHeight() * 0.5D;
        world.makeSound(null, victim.getX(), centerY, victim.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.9F, pitch);
        world.sendParticles(ParticleTypes.CRIT, victim.getX(), centerY, victim.getZ(), 8, 0.35D, 0.25D, 0.35D, 0.05D);
        world.sendParticles(ParticleTypes.SMOKE, victim.getX(), centerY, victim.getZ(), 6, 0.25D, 0.2D, 0.25D, 0.01D);
    }

    private static final class RemoveModifierTask implements mutation_task_scheduler.TimedTask {
        private final LivingEntity entity;
        private final Holder<Attribute> attribute;
        private final ResourceLocation modifierId;
        private int ticksLeft;

        private RemoveModifierTask(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId, int ticks) {
            this.entity = entity;
            this.attribute = attribute;
            this.modifierId = modifierId;
            this.ticksLeft = ticks;
        }

        @Override
        public boolean tick(ServerLevel world) {
            if (entity.isRemoved() || !entity.isAlive()) {
                return true;
            }
            if (--ticksLeft > 0) {
                return false;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifierId);
            }
            return true;
        }
    }
}
