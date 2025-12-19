package com.example.block.entity;

import com.example.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TemporaryLightBlockEntity extends BlockEntity {
    private int ticksLeft = 200; // 10 seconds default

    public TemporaryLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.TEMPORARY_LIGHT_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TemporaryLightBlockEntity blockEntity) {
        blockEntity.ticksLeft--;
        if (blockEntity.ticksLeft <= 0) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TicksLeft", this.ticksLeft);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("TicksLeft")) {
            this.ticksLeft = tag.getInt("TicksLeft");
        }
    }
}
