package com.mrsasayo.legacycreaturescorey.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class FollowerItem extends Item {

    public FollowerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // CORRECCIÓN: usar world.isClient en lugar de isClient
        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;
            
            double x = user.getX();
            double y = user.getY() + 1;
            double z = user.getZ();
            
            for (int i = 0; i < 20; i++) {
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 2;
                
                serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    x + offsetX,
                    y + offsetY,
                    z + offsetZ,
                    1,
                    0, 0, 0,
                    0.1
                );
            }
            
            user.sendMessage(
                Text.literal("¡Partículas de fuego!"),
                false
            );
            
            stack.decrement(1);
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }
}