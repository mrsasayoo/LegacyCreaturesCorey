package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.network.ClientEffectPayload;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Applies camera distortion or status-based disorientation effects to the target.
 */
public final class ConcussiveBlowOnHitAction extends ProcOnHitAction {
    private final Mode mode;

    public ConcussiveBlowOnHitAction(double chance, Mode mode) {
        super(chance);
        this.mode = mode;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof ServerPlayerEntity player)) {
            return;
        }

        switch (mode) {
            case SHAKE -> applyCameraShake(player);
            case TUNNEL -> applyTunnelVision(player);
            case DISORIENT -> applyDisorientation(attacker, player);
        }
    }

    private void applyCameraShake(ServerPlayerEntity player) {
        ModNetworking.sendClientEffect(player, new ClientEffectPayload(ClientEffectType.CAMERA_SHAKE, true, 10, 1.2F));
    }

    private void applyTunnelVision(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 60, 0));
    }

    private void applyDisorientation(LivingEntity attacker, ServerPlayerEntity player) {
        float newYaw = MathHelper.wrapDegrees(player.getYaw() + 180.0F);
        player.setYaw(newYaw);
        player.setHeadYaw(newYaw);
        player.setBodyYaw(newYaw);
        player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), newYaw, player.getPitch());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0));
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 30, 2));
    }

    public enum Mode {
        SHAKE,
        TUNNEL,
        DISORIENT;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "SHAKE", "I" -> SHAKE;
                case "TUNNEL", "II" -> TUNNEL;
                case "DISORIENT", "III" -> DISORIENT;
                default -> throw new IllegalArgumentException("Modo de golpe conmocionador desconocido: " + raw);
            };
        }
    }
}
