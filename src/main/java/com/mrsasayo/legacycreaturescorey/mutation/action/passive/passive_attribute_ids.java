package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import net.minecraft.resources.Identifier;

/**
 * IDs de atributos del REGISTRO 26.3 — son PLANOS (minecraft:armor), SIN el prefijo
 * "generic." (verificado con javap contra el constant pool de Attributes: los ids
 * registrados son "armor", "scale", "gravity", etc.). Los ids "minecraft:generic.*"
 * ya NO existen y resolvían a null: TODAS las pasivas eran no-ops silenciosos.
 */
final class passive_attribute_ids {
    static final Identifier ARMOR = Identifier.parse("minecraft:armor");
    static final Identifier ATTACK_DAMAGE = Identifier.parse("minecraft:attack_damage");
    static final Identifier ATTACK_SPEED = Identifier.parse("minecraft:attack_speed");
    static final Identifier FOLLOW_RANGE = Identifier.parse("minecraft:follow_range");
    static final Identifier KNOCKBACK_RESISTANCE = Identifier.parse("minecraft:knockback_resistance");
    static final Identifier MAX_HEALTH = Identifier.parse("minecraft:max_health");
    static final Identifier MOVEMENT_SPEED = Identifier.parse("minecraft:movement_speed");
    // Nuevas familias (Lote Pasivas 2026-11-05)
    static final Identifier ATTACK_KNOCKBACK = Identifier.parse("minecraft:attack_knockback");
    static final Identifier SCALE = Identifier.parse("minecraft:scale");
    static final Identifier STEP_HEIGHT = Identifier.parse("minecraft:step_height");
    static final Identifier GRAVITY = Identifier.parse("minecraft:gravity");
    static final Identifier FALL_DAMAGE_MULTIPLIER = Identifier.parse("minecraft:fall_damage_multiplier");
    static final Identifier JUMP_STRENGTH = Identifier.parse("minecraft:jump_strength");
    static final Identifier WATER_MOVEMENT_EFFICIENCY = Identifier.parse("minecraft:water_movement_efficiency");

    private passive_attribute_ids() {
    }
}
