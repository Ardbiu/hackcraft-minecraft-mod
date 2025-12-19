package com.example.item;

import com.example.RecallState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RecallTotemItem extends Item {
    public RecallTotemItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return TypedActionResult.success(user.getStackInHand(hand));
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ServerWorld serverWorld = player.getServerWorld();
        RecallState state = RecallState.getServerState(serverWorld.getServer());
        ItemStack stack = user.getStackInHand(hand);

        // Sneak + Right Click = Clear
        if (player.isSneaking()) {
            if (state.recalls.containsKey(player.getUuid())) {
                state.recalls.remove(player.getUuid());
                state.markDirty();
                player.sendMessage(Text.of("Recall point cleared."), true);
                player.playSound(SoundEvents.UI_TOAST_OUT, 1.0f, 1.0f);
            } else {
                player.sendMessage(Text.of("No recall point to clear."), true);
            }
            return TypedActionResult.success(stack);
        }

        // Normal Right Click
        if (state.recalls.containsKey(player.getUuid())) {
            // Teleport
            RecallState.RecallPos recall = state.recalls.get(player.getUuid());
            attemptTeleport(player, recall, stack, state);
        } else {
            // Save
            state.recalls.put(player.getUuid(), new RecallState.RecallPos(
                    player.getServerWorld().getRegistryKey(),
                    player.getPos(),
                    player.getYaw(),
                    player.getPitch()
            ));
            state.markDirty();
            player.sendMessage(Text.of("Recall point saved!"), true);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        return TypedActionResult.success(stack);
    }

    private void attemptTeleport(ServerPlayerEntity player, RecallState.RecallPos recall, ItemStack stack, RecallState state) {
        ServerWorld targetWorld = player.getServer().getWorld(recall.dim);
        if (targetWorld == null) {
            player.sendMessage(Text.of("Target dimension not found!"), true);
            return;
        }

        BlockPos targetBlock = BlockPos.ofFloored(recall.pos);
        BlockPos safePos = findSafePos(targetWorld, targetBlock);

        if (safePos == null) {
            player.sendMessage(Text.of("Target location is unsafe!"), true);
            return;
        }

        // Execute Teleport
        player.teleport(targetWorld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, recall.yaw, recall.pitch);

        // Effects
        targetWorld.playSound(null, safePos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        targetWorld.spawnParticles(ParticleTypes.PORTAL, safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5, 20, 0.5, 1, 0.5, 0.1);
        
        // Cooldown & Cost
        player.getItemCooldownManager().set(this, 200); // 10s
        stack.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
    }

    private BlockPos findSafePos(ServerWorld world, BlockPos pos) {
        // Simple check: is target block safe?
        if (isSafe(world, pos)) return pos;

        // Check upwards 3 blocks
        for (int i = 1; i <= 3; i++) {
             if (isSafe(world, pos.up(i))) return pos.up(i);
        }

        // Check 3x3 horizontal
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos p = pos.add(x, 0, z);
                if (isSafe(world, p)) return p;
                if (isSafe(world, p.up())) return p.up();
            }
        }
        
        return null;
    }

    private boolean isSafe(ServerWorld world, BlockPos pos) {
        BlockState floor = world.getBlockState(pos.down());
        if (!floor.isSolid()) return false; // Need solid ground

        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        
        // Must be air/replaceable and not damaging (ignoring complex checks for now)
        return feet.getCollisionShape(world, pos).isEmpty() && head.getCollisionShape(world, pos.up()).isEmpty()
                && !feet.isLiquid();
    }
}
