package com.mrsasayo.legacycreaturescorey.content.status;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

/**
 * Holds the custom status effects required to reproduce bespoke mechanics.
 */
public final class ModStatusEffects {
    public static final MobEffect SLIPPERY_FLOOR = register("slippery_floor",
        new SlipperyFloorStatusEffect(MobEffectCategory.HARMFUL, 0x58C5FF));
    public static final MobEffect HEAVY_GRAVITY = register("heavy_gravity",
        new HeavyGravityStatusEffect(MobEffectCategory.HARMFUL, 0x4B0076));
    public static final MobEffect INVERTED_CONTROLS = register("inverted_controls",
        new NotifyingStatusEffect(MobEffectCategory.HARMFUL, 0xFFAA00, ClientEffectType.INVERT_CONTROLS, 1.0F));
    public static final MobEffect HOSTILE_MUFFLE = register("hostile_muffle",
        new NotifyingStatusEffect(MobEffectCategory.HARMFUL, 0x7A7A7A, ClientEffectType.HOSTILE_VOLUME_SCALE, 0.5F));
    public static final MobEffect HOSTILE_SILENCE = register("hostile_silence",
        new NotifyingStatusEffect(MobEffectCategory.HARMFUL, 0x2C2C2C, ClientEffectType.HOSTILE_VOLUME_SCALE, 0.0F));
    public static final MobEffect PHANTOM_SOUNDS = register("phantom_sounds",
        new NotifyingStatusEffect(MobEffectCategory.HARMFUL, 0xAD3DE1, ClientEffectType.PHANTOM_SOUNDS, 1.0F));
    public static final MobEffect CAMERA_SHAKE = register("camera_shake",
        new NotifyingStatusEffect(MobEffectCategory.HARMFUL, 0xFF3366, ClientEffectType.CAMERA_SHAKE, 0.6F));
    public static final MobEffect BLEEDING = register("bleeding",
        new bleeding_status_effect());
    public static final MobEffect TUNNEL_VISION = register("tunnel_vision",
        new tunnel_vision_status_effect());
    public static final MobEffect RINGING_EARS = register("ringing_ears",
        new ringing_ears_status_effect());
    public static final MobEffect PHANTOM_NOISE = register("phantom_noise",
        new NotifyingStatusEffect(MobEffectCategory.HARMFUL, 0x4E6E8E, ClientEffectType.PHANTOM_SOUNDS, 1.0F));
    public static final MobEffect ABSOLUTE_SILENCE = register("absolute_silence",
        new absolute_silence_status_effect());
    public static final MobEffect AGOBIO = register("agobio",
        new agobio_status_effect());
    public static final MobEffect EXPUESTO = register("expuesto",
        new expuesto_status_effect());
    public static final MobEffect PAIN_LINK_MARK = register("pain_link",
        new pain_link_mark_status_effect());
    public static final MobEffect MORTAL_WOUND_MINOR = register("mortal_wound_minor",
        new HealingReductionStatusEffect(MobEffectCategory.HARMFUL, 0x9E3030, 0.67F));
    public static final MobEffect MORTAL_WOUND_MAJOR = register("mortal_wound_major",
        new HealingReductionStatusEffect(MobEffectCategory.HARMFUL, 0x5B0B0B, 0.25F));
    public static final MobEffect MORTAL_WOUND_TOTAL = register("mortal_wound_total",
        new HealingReductionStatusEffect(MobEffectCategory.HARMFUL, 0x1A0000, 0.0F));
    public static final MobEffect PRECARIOUS_HUNGER = register("precarious_hunger",
        new precarious_hunger_status_effect());

    private ModStatusEffects() {}

    public static void init() {
        Legacycreaturescorey.LOGGER.info("✅ Status effects personalizados registrados");
    }

    private static MobEffect register(String name, MobEffect effect) {
        return Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, name), effect);
    }
}
