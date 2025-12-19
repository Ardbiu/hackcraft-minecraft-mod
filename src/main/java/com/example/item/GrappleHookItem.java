package com.example.item;

import com.example.GrappleManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GrappleHookItem extends Item {
    public GrappleHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            GrappleManager.attemptGrapple(serverPlayer, player.getItemInHand(hand));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
