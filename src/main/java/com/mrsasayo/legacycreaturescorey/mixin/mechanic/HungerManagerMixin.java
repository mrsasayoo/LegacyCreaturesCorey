package com.mrsasayo.legacycreaturescorey.mixin.mechanic;

import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.ancient_curse_curse_tracker;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.content.status.precarious_hunger_status_effect;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3: yarn HungerManager -> mojmap world.food.FoodData; update(ServerPlayerEntity) -> tick(ServerPlayer);
 * yarn add(int)/add(float) -> mojmap eat(int, float) publico (los ModifyVariable van sobre eat)
 */
@Mixin(FoodData.class)
public class HungerManagerMixin {
    @Unique
    private Player player;

    @Inject(method = "tick", at = @At("HEAD"))
    private void capturePlayer(ServerPlayer player, CallbackInfo ci) {
        this.player = player;
    }

    @ModifyVariable(method = "eat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyFoodLevel(int food) {
        if (player == null) return food;

        // Verificar efecto precarious_hunger primero
        MobEffectInstance hungerEffect = player.getEffect(Holder.direct(ModStatusEffects.PRECARIOUS_HUNGER));
        if (hungerEffect != null) {
            return precarious_hunger_status_effect.calculateReducedNutrition(food, hungerEffect.getAmplifier());
        }

        // Fallback al sistema de maldicion antiguo
        if (ancient_curse_curse_tracker.isCursed(player)) {
            return Math.max(1, (int) (food * 0.75f)); // Reduce by 25%
        }
        return food;
    }

    @ModifyVariable(method = "eat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modifySaturation(float saturationModifier) {
        if (player == null) return saturationModifier;

        // Verificar efecto precarious_hunger primero
        MobEffectInstance hungerEffect = player.getEffect(Holder.direct(ModStatusEffects.PRECARIOUS_HUNGER));
        if (hungerEffect != null) {
            return precarious_hunger_status_effect.calculateReducedSaturation(saturationModifier, hungerEffect.getAmplifier());
        }

        // Fallback al sistema de maldicion antiguo
        if (ancient_curse_curse_tracker.isCursed(player)) {
            return saturationModifier * 0.75f; // Reduce by 25%
        }
        return saturationModifier;
    }
}
