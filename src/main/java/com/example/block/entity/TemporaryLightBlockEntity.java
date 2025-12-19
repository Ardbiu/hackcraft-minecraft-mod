package com.example.block.entity;

import com.example.ModContent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TemporaryLightBlockEntity extends BlockEntity {
    private int ticksLeft = 200; // 10 seconds default

    public TemporaryLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.TEMPORARY_LIGHT_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, TemporaryLightBlockEntity blockEntity) {
        blockEntity.ticksLeft--;
        if (blockEntity.ticksLeft <= 0) {
            world.removeBlock(pos, false);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("TicksLeft", this.ticksLeft);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("TicksLeft")) {
            this.ticksLeft = nbt.getInt("TicksLeft");
        }
    }
}
