package com.mrsasayo.legacycreaturescorey.mixin.mechanic;

import com.mrsasayo.legacycreaturescorey.content.item.ModItems;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.FakeLootPileManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3: yarn onPlayerCollision -> mojmap playerTouch(Player); getStack -> getItem;
 * getEntityWorld().isClient() -> level().isClientSide; addStatusEffect -> addEffect;
 * SoundEvents.ENTITY_PHANTOM_HURT -> PHANTOM_HURT; SoundCategory -> SoundSource;
 * spawnParticles -> sendParticles; getBodyY -> getY
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    private static final int GHOST_FRAGMENT_NAUSEA_TICKS = 60;

    @Shadow public abstract ItemStack getItem();

    protected ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void legacy$handleGhostFragment(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (FakeLootPileManager.isPlaceholder(self)) {
            if (!player.level().isClientSide) {
                FakeLootPileManager.trigger(self, player);
            }
            ci.cancel();
            return;
        }

        ItemStack stack = getItem();
        if (!stack.is(ModItems.GHOST_FRAGMENT)) {
            return;
        }

        Level world = player.level();
        if (!world.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, GHOST_FRAGMENT_NAUSEA_TICKS, 0, true, true));
            world.playSound(null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PHANTOM_HURT,
                SoundSource.HOSTILE,
                0.7F,
                1.5F + world.random.nextFloat() * 0.3F);
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(),
                    player.getY(0.5D),
                    player.getZ(),
                    6,
                    0.1D,
                    0.1D,
                    0.1D,
                    0.01D);
            }
        }

        self.discard();
        ci.cancel();
    }
}
