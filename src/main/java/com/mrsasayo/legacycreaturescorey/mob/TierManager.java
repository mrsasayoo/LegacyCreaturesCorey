package com.mrsasayo.legacycreaturescorey.mob;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.antifarm.AntiFarmManager;
import com.mrsasayo.legacycreaturescorey.api.event.TierEvents;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.data.MobAttributeDataLoader;
import com.mrsasayo.legacycreaturescorey.mob.data.MobTierRuleDataLoader;
import com.mrsasayo.legacycreaturescorey.mob.data.BiomeTierWeightDataLoader;
import com.mrsasayo.legacycreaturescorey.mutation.MutationAssigner;
import com.mrsasayo.legacycreaturescorey.synergy.SynergyManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.EnumSet;

public final class TierManager {

    private TierManager() {
    }

    public static MobTier tryCategorize(MobEntity mob, int effectiveDifficulty) {
        var data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        MobTier currentTier = data.getTier();

        if (currentTier != MobTier.NORMAL) {
            return currentTier;
        }

        if (AntiFarmManager.shouldBlockTieredSpawns(mob)) {
            if (CoreyConfig.INSTANCE.debugLogProbabilityDetails) {
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
            if (CoreyConfig.INSTANCE.debugLogProbabilityDetails) {
                Legacycreaturescorey.LOGGER.debug("⛔ {} sin tiers permitidos", mob.getType().getTranslationKey());
            } else {
                Legacycreaturescorey.LOGGER.debug("⛔ {} sin tiers permitidos", mob.getType().getTranslationKey());
            }
            return currentTier;
        }

        MobTier chosen = resolveTier(mob, effectiveDifficulty, allowedTiers);
        if (chosen == MobTier.NORMAL) {
            if (CoreyConfig.INSTANCE.debugLogProbabilityDetails) {
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
        MutationAssigner.assignMutations(mob, chosen, data);
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
    public static void forceTier(MobEntity mob, MobTier tier, boolean assignDefaultMutations) {
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
            MutationAssigner.assignMutations(mob, tier, data);
        }

        FuryHelper.applyFury(mob, data);
        SynergyManager.onMobTiered(mob, tier, data);
        TierEvents.TIER_APPLIED.invoker().onTierApplied(mob, tier, true);
    }

    private static MobTier resolveTier(MobEntity mob, int effectiveDifficulty, EnumSet<MobTier> allowedTiers) {
        CoreyConfig config = CoreyConfig.INSTANCE;

        if (config.debugForceExactTier != null && allowedTiers.contains(config.debugForceExactTier)) {
            return config.debugForceExactTier;
        }
        if (config.debugForceExactTier != null && !allowedTiers.contains(config.debugForceExactTier) && config.debugLogProbabilityDetails) {
            Legacycreaturescorey.LOGGER.debug(
                "🚫 {} no permite forzar tier {}. Permitidos: {}",
                mob.getType().getTranslationKey(),
                config.debugForceExactTier,
                allowedTiers
            );
        }

        if (config.debugForceHighestAllowedTier) {
            MobTier forced = findHighestAllowedTier(allowedTiers);
            if (forced != null) {
                return forced;
            }
            if (config.debugLogProbabilityDetails) {
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

    private static void applyBaseScaling(MobEntity mob, MobTier tier) {
        EntityAttributeInstance health = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);
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

        EntityAttributeInstance damage = mob.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
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

    private static void applyVisuals(MobEntity mob, MobTier tier) {
        MutableText baseName = Text.translatable(mob.getType().getTranslationKey());
        MutableText tierSuffix = Text.literal(" " + tier.getDisplayName());
        MutableText customName = Text.empty().append(baseName).append(tierSuffix)
            .styled(style -> style.withColor(TextColor.fromRgb(tier.getNameColor())));

        mob.setCustomName(customName);
        mob.setCustomNameVisible(true);

        if (mob.getEntityWorld() instanceof ServerWorld serverWorld) {
            TierParticleHelper.spawnInitialBurst(serverWorld, mob, tier);
        }
    }
}
