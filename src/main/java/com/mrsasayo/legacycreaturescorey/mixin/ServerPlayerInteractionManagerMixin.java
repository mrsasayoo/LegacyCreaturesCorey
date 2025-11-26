package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registra en consola cada vez que el servidor rechaza romper un bloque cuando la depuración está activa.
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow public ServerPlayerEntity player;

    @Shadow public ServerWorld world;

    @Shadow
    public abstract GameMode getGameMode();

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    @SuppressWarnings("deprecation")
    private void legacycreaturescorey$logDeniedBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || player == null || !CoreyConfig.INSTANCE.debugTraceBlockBreakDenials) {
            return;
        }
        ServerWorld serverWorld = this.world;
        var state = serverWorld.getBlockState(pos);
        Legacycreaturescorey.LOGGER.warn(
            "[Depuración Bloques] {} no pudo romper {} en {} ({}, {}, {}) GM={} creativo={} espectador={} chunkCargado={}",
            player.getName().getString(),
            state.getBlock().getTranslationKey(),
            serverWorld.getRegistryKey().getValue(),
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            getGameMode(),
            player.getAbilities().creativeMode,
            player.isSpectator(),
            serverWorld.isChunkLoaded(pos)
        );
    }
}
