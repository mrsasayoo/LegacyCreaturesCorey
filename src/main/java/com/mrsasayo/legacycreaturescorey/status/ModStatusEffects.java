package com.mrsasayo.legacycreaturescorey.status;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Holds the custom status effects required to reproduce bespoke mechanics.
 */
public final class ModStatusEffects {
    public static final StatusEffect SLIPPERY_FLOOR = register("slippery_floor",
        new SlipperyFloorStatusEffect(StatusEffectCategory.HARMFUL, 0x58C5FF));
    public static final StatusEffect HEAVY_GRAVITY = register("heavy_gravity",
        new HeavyGravityStatusEffect(StatusEffectCategory.HARMFUL, 0x4B0076));
    public static final StatusEffect INVERTED_CONTROLS = register("inverted_controls",
        new NotifyingStatusEffect(StatusEffectCategory.HARMFUL, 0xFFAA00, ClientEffectType.INVERT_CONTROLS, 1.0F));
    public static final StatusEffect HOSTILE_MUFFLE = register("hostile_muffle",
        new NotifyingStatusEffect(StatusEffectCategory.HARMFUL, 0x7A7A7A, ClientEffectType.HOSTILE_VOLUME_SCALE, 0.5F));
    public static final StatusEffect HOSTILE_SILENCE = register("hostile_silence",
        new NotifyingStatusEffect(StatusEffectCategory.HARMFUL, 0x2C2C2C, ClientEffectType.HOSTILE_VOLUME_SCALE, 0.0F));
    public static final StatusEffect PHANTOM_SOUNDS = register("phantom_sounds",
        new NotifyingStatusEffect(StatusEffectCategory.HARMFUL, 0xAD3DE1, ClientEffectType.PHANTOM_SOUNDS, 1.0F));
    public static final StatusEffect CAMERA_SHAKE = register("camera_shake",
        new NotifyingStatusEffect(StatusEffectCategory.HARMFUL, 0xFF3366, ClientEffectType.CAMERA_SHAKE, 0.6F));
    public static final StatusEffect MORTAL_WOUND_MINOR = register("mortal_wound_minor",
        new HealingReductionStatusEffect(StatusEffectCategory.HARMFUL, 0x9E3030, 0.67F));
    public static final StatusEffect MORTAL_WOUND_MAJOR = register("mortal_wound_major",
        new HealingReductionStatusEffect(StatusEffectCategory.HARMFUL, 0x5B0B0B, 0.25F));
    public static final StatusEffect MORTAL_WOUND_TOTAL = register("mortal_wound_total",
        new HealingReductionStatusEffect(StatusEffectCategory.HARMFUL, 0x1A0000, 0.0F));

    private ModStatusEffects() {}

    public static void init() {
        Legacycreaturescorey.LOGGER.info("✅ Status effects personalizados registrados");
    }

    private static StatusEffect register(String name, StatusEffect effect) {
        return Registry.register(Registries.STATUS_EFFECT, Identifier.of(Legacycreaturescorey.MOD_ID, name), effect);
    }
}
