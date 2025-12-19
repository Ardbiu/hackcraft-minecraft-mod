package com.example.item;

import com.example.RecallState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RecallTotemItem extends Item {
    public RecallTotemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResultHolder.success(user.getItemInHand(hand));
        }

        ServerPlayer player = (ServerPlayer) user;
        ServerLevel serverLevel = player.serverLevel();
        RecallState state = RecallState.getServerState(serverLevel.getServer());
        ItemStack stack = user.getItemInHand(hand);

        // Sneak + Right Click = Clear
        if (player.isCrouching()) {
            if (state.recalls.containsKey(player.getUUID())) {
                state.recalls.remove(player.getUUID());
                state.setDirty();
                player.displayClientMessage(Component.literal("Recall point cleared."), true);
                player.playSound(SoundEvents.UI_TOAST_OUT, 1.0f, 1.0f);
            } else {
                player.displayClientMessage(Component.literal("No recall point to clear."), true);
            }
            return InteractionResultHolder.success(stack);
        }

        // Normal Right Click
        if (state.recalls.containsKey(player.getUUID())) {
            RecallState.RecallPos recall = state.recalls.get(player.getUUID());
            attemptTeleport(player, recall, stack, state);
        } else {
            state.recalls.put(player.getUUID(), new RecallState.RecallPos(
                    player.serverLevel().dimension(),
                    player.position(),
                    player.getYRot(),
                    player.getXRot()));
            state.setDirty();
            player.displayClientMessage(Component.literal("Recall point saved!"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS,
                    1.0f, 1.0f);
        }

        return InteractionResultHolder.success(stack);
    }

    private void attemptTeleport(ServerPlayer player, RecallState.RecallPos recall, ItemStack stack,
            RecallState state) {
        ServerLevel targetLevel = player.getServer().getLevel(recall.dim);
        if (targetLevel == null) {
            player.displayClientMessage(Component.literal("Target dimension not found!"), true);
            return;
        }

        BlockPos targetBlock = BlockPos.containing(recall.pos);
        BlockPos safePos = findSafePos(targetLevel, targetBlock);

        if (safePos == null) {
            player.displayClientMessage(Component.literal("Target location is unsafe!"), true);
            return;
        }

        player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, recall.yaw,
                recall.pitch);

        targetLevel.playSound(null, safePos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        targetLevel.sendParticles(ParticleTypes.PORTAL, safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5,
                20, 0.5, 1, 0.5, 0.1);

        player.getCooldowns().addCooldown(this, 200);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    private BlockPos findSafePos(ServerLevel level, BlockPos pos) {
        if (isSafe(level, pos))
            return pos;

        for (int i = 1; i <= 3; i++) {
            if (isSafe(level, pos.above(i)))
                return pos.above(i);
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0)
                    continue;
                BlockPos p = pos.offset(x, 0, z);
                if (isSafe(level, p))
                    return p;
                if (isSafe(level, p.above()))
                    return p.above();
            }
        }

        return null;
    }

    private boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos.below());
        if (!floor.isSolid())
            return false;

        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());

        return feet.getCollisionShape(level, pos).isEmpty() && head.getCollisionShape(level, pos.above()).isEmpty()
                && !feet.liquid();
    }
}
