package com.example.item;

import com.example.GrappleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class GrappleHookItem extends Item {
    public GrappleHookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            // Initiate grapple logic
            GrappleManager.attemptGrapple(serverPlayer, user.getStackInHand(hand));
        }
        
        // Return success to play the arm swing animation
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
