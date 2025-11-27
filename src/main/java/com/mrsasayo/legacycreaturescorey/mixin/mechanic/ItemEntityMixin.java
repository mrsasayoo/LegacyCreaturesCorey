package com.mrsasayo.legacycreaturescorey.mixin.mechanic;

import com.mrsasayo.legacycreaturescorey.content.item.ModItems;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.FakeLootPileManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    private static final int GHOST_FRAGMENT_NAUSEA_TICKS = 60;

    @Shadow public abstract ItemStack getStack();

    protected ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void legacy$handleGhostFragment(PlayerEntity player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (FakeLootPileManager.isPlaceholder(self)) {
            if (!player.getEntityWorld().isClient()) {
                FakeLootPileManager.trigger(self, player);
            }
            ci.cancel();
            return;
        }

        ItemStack stack = getStack();
        if (!stack.isOf(ModItems.GHOST_FRAGMENT)) {
            return;
        }

        World world = player.getEntityWorld();
        if (!world.isClient()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, GHOST_FRAGMENT_NAUSEA_TICKS, 0, true, true));
            world.playSound(null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_PHANTOM_HURT,
                SoundCategory.HOSTILE,
                0.7F,
                1.5F + world.random.nextFloat() * 0.3F);
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(),
                    player.getBodyY(0.5D),
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
