package com.mrsasayo.legacycreaturescorey.mixin.mechanic;

import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.ancient_curse_curse_tracker;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.content.status.precarious_hunger_status_effect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {
    @Unique
    private PlayerEntity player;

    // Minecraft 1.21.10: update() ahora recibe ServerPlayerEntity en vez de PlayerEntity
    @Inject(method = "update", at = @At("HEAD"))
    private void capturePlayer(ServerPlayerEntity player, CallbackInfo ci) {
        this.player = player;
    }

    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyFoodLevel(int food) {
        if (player == null) return food;
        
        // Verificar efecto precarious_hunger primero
        StatusEffectInstance hungerEffect = player.getStatusEffect(RegistryEntry.of(ModStatusEffects.PRECARIOUS_HUNGER));
        if (hungerEffect != null) {
            return precarious_hunger_status_effect.calculateReducedNutrition(food, hungerEffect.getAmplifier());
        }
        
        // Fallback al sistema de maldición antiguo
        if (ancient_curse_curse_tracker.isCursed(player)) {
            return Math.max(1, (int) (food * 0.75f)); // Reduce by 25%
        }
        return food;
    }

    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modifySaturation(float saturationModifier) {
        if (player == null) return saturationModifier;
        
        // Verificar efecto precarious_hunger primero
        StatusEffectInstance hungerEffect = player.getStatusEffect(RegistryEntry.of(ModStatusEffects.PRECARIOUS_HUNGER));
        if (hungerEffect != null) {
            return precarious_hunger_status_effect.calculateReducedSaturation(saturationModifier, hungerEffect.getAmplifier());
        }
        
        // Fallback al sistema de maldición antiguo
        if (ancient_curse_curse_tracker.isCursed(player)) {
            return saturationModifier * 0.75f; // Reduce by 25%
        }
        return saturationModifier;
    }
}
