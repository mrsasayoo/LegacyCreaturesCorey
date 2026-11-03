package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.feature.antifarm.AntiFarmManager;
import com.mrsasayo.legacycreaturescorey.core.api.event.TierEvents;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.MobAttributeDataLoader;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.MobTierRuleDataLoader;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.BiomeTierWeightDataLoader;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_assigner;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.EnumSet;

public final class TierManager {

    private TierManager() {
    }

    public static MobTier tryCategorize(Mob mob, int effectiveDifficulty) {
        var data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        MobTier currentTier = data.getTier();

        if (currentTier != MobTier.NORMAL) {
            return currentTier;
        }

        if (AntiFarmManager.shouldBlockTieredSpawns(mob)) {
            if (difficulty_config.isDebugLogProbabilityDetails()) {
                Legacycreaturescorey.LOGGER.debug(
                    "🚧 {} spawn bloqueado por anti-granja en chunk {}|{}",
                    mob.getType().getTranslationKey(),
                    mob.getChunkPos().x,
                    mob.getChunkPos().z
                );
            }
            return MobTier.NORMAL;
        }

        EnumSet<MobTier> allowedTiers = determineAllowedTiers(mob.getType());
        if (allowedTiers.isEmpty()) {
            if (difficulty_config.isDebugLogProbabilityDetails()) {
                Legacycreaturescorey.LOGGER.debug("⛔ {} sin tiers permitidos", mob.getType().getTranslationKey());
            } else {
                Legacycreaturescorey.LOGGER.debug("⛔ {} sin tiers permitidos", mob.getType().getTranslationKey());
            }
            return currentTier;
        }

        MobTier chosen = resolveTier(mob, effectiveDifficulty, allowedTiers);
        if (chosen == MobTier.NORMAL) {
            if (difficulty_config.isDebugLogProbabilityDetails()) {
                Legacycreaturescorey.LOGGER.debug(
                    "🎲 {} permaneció Normal | dificultad={} | permitidos={}",
                    mob.getType().getTranslationKey(),
                    effectiveDifficulty,
                    allowedTiers
                );
            }
            return currentTier;
        }

        data.setTier(chosen);
        applyBaseScaling(mob, chosen);
        applyVisuals(mob, chosen);
        mutation_assigner.assignMutations(mob, chosen, data);
        FuryHelper.applyFury(mob, data);
        SynergyManager.onMobTiered(mob, chosen, data);
        TierEvents.TIER_APPLIED.invoker().onTierApplied(mob, chosen, false);
        Legacycreaturescorey.LOGGER.info(
            "⚔️ {} promovido a tier {} en ({}, {}, {})",
            mob.getType().getTranslationKey(),
            chosen.getDisplayName(),
            mob.getBlockX(),
            mob.getBlockY(),
            mob.getBlockZ()
        );
        return chosen;
    }

    /**
     * Fuerza un tier específico sobre un mob ya spawneado aplicando los mismos
     * efectos visuales y escalados que {@link #tryCategorize}, opcionalmente
     * reasignando mutaciones basadas en el tier solicitado.
     */
    public static void forceTier(Mob mob, MobTier tier, boolean assignDefaultMutations) {
        if (tier == null || mob == null) {
            return;
        }

        var data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        if (assignDefaultMutations) {
            data.clearMutations();
            data.setFarmed(false);
            data.setFurious(false);
        }

        data.setTier(tier);

        applyBaseScaling(mob, tier);
        applyVisuals(mob, tier);

        if (assignDefaultMutations) {
            mutation_assigner.assignMutations(mob, tier, data);
        }

        FuryHelper.applyFury(mob, data);
        SynergyManager.onMobTiered(mob, tier, data);
        TierEvents.TIER_APPLIED.invoker().onTierApplied(mob, tier, true);
    }

    private static MobTier resolveTier(Mob mob, int effectiveDifficulty, EnumSet<MobTier> allowedTiers) {
        MobTier forcedExact = difficulty_config.getDebugForceExactTier();
        boolean logDetails = difficulty_config.isDebugLogProbabilityDetails();

        if (forcedExact != null && allowedTiers.contains(forcedExact)) {
            return forcedExact;
        }
        if (forcedExact != null && !allowedTiers.contains(forcedExact) && logDetails) {
            Legacycreaturescorey.LOGGER.debug(
                "🚫 {} no permite forzar tier {}. Permitidos: {}",
                mob.getType().getTranslationKey(),
                forcedExact,
                allowedTiers
            );
        }

        if (difficulty_config.isDebugForceHighestAllowedTier()) {
            MobTier forced = findHighestAllowedTier(allowedTiers);
            if (forced != null) {
                return forced;
            }
            if (logDetails) {
                Legacycreaturescorey.LOGGER.debug(
                    "⚠️ {} no tiene tiers superiores a Normal disponibles",
                    mob.getType().getTranslationKey()
                );
            }
        }

        return TierProbabilityCalculator.chooseTier(
            effectiveDifficulty,
            allowedTiers,
            mob.getRandom(),
            tier -> BiomeTierWeightDataLoader.getMultiplier(mob, tier)
        );
    }

    private static MobTier findHighestAllowedTier(EnumSet<MobTier> allowedTiers) {
        MobTier result = null;
        for (MobTier tier : MobTier.values()) {
            if (tier == MobTier.NORMAL) {
                continue;
            }
            if (allowedTiers.contains(tier)) {
                result = tier;
            }
        }
        return result;
    }

    private static EnumSet<MobTier> determineAllowedTiers(EntityType<?> type) {
        EnumSet<MobTier> datapackTiers = MobTierRuleDataLoader.getAllowedTiers(type);
        return datapackTiers != null ? datapackTiers : EnumSet.noneOf(MobTier.class);
    }

    private static void applyBaseScaling(Mob mob, MobTier tier) {
        AttributeInstance health = mob.getAttributeInstance(Attributes.MAX_HEALTH);
        if (health != null) {
            Double override = MobAttributeDataLoader.getMaxHealth(mob.getType(), tier);
            double base = health.getBaseValue();
            double target = override != null && override > 0.0D
                ? override
                : base * tier.getHealthMultiplier();
            if ((override != null && override > 0.0D) || tier.getHealthMultiplier() != 1.0D) {
                health.setBaseValue(target);
                mob.setHealth((float) Math.max(target, 1.0F));
            }
        }

        AttributeInstance damage = mob.getAttributeInstance(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            Double override = MobAttributeDataLoader.getAttackDamage(mob.getType(), tier);
            double base = damage.getBaseValue();
            double target = override != null && override > 0.0D
                ? override
                : base * tier.getDamageMultiplier();
            if ((override != null && override > 0.0D) || tier.getDamageMultiplier() != 1.0D) {
                damage.setBaseValue(target);
            }
        }
    }

    private static void applyVisuals(Mob mob, MobTier tier) {
        MutableComponent baseName = Component.translatable(mob.getType().getTranslationKey());
        MutableComponent tierSuffix = Component.literal(" " + tier.getDisplayName());
        MutableComponent customName = Component.empty().append(baseName).append(tierSuffix)
            .styled(style -> style.withColor(TextColor.fromRgb(tier.getNameColor())));

        mob.setCustomName(customName);
        mob.setCustomNameVisible(true);

        if (mob.getEntityWorld() instanceof ServerLevel serverWorld) {
            TierParticleHelper.spawnInitialBurst(serverWorld, mob, tier);
        }
    }
}
