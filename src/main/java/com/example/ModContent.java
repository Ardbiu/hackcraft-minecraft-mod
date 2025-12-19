package com.example;

import com.example.block.TemporaryLightBlock;
import com.example.block.entity.TemporaryLightBlockEntity;
import com.example.item.GlowstepBootsItem;
import com.example.item.GrappleHookItem;
import com.example.item.RecallTotemItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModContent {

    public static final TemporaryLightBlock TEMPORARY_LIGHT_BLOCK;
    public static final BlockEntityType<TemporaryLightBlockEntity> TEMPORARY_LIGHT_BLOCK_ENTITY;
    public static final GlowstepBootsItem GLOWSTEP_BOOTS;
    public static final GrappleHookItem GRAPPLE_HOOK;
    public static final RecallTotemItem RECALL_TOTEM;

    static {
        BlockBehaviour.Properties blockSettings = BlockBehaviour.Properties.ofFullCopy(Blocks.AIR)
                .lightLevel(state -> 12)
                .noCollission()
                .replaceable()
                .noLootTable()
                .noOcclusion();

        TEMPORARY_LIGHT_BLOCK = new TemporaryLightBlock(blockSettings);

        TEMPORARY_LIGHT_BLOCK_ENTITY = BlockEntityType.Builder.of(TemporaryLightBlockEntity::new, TEMPORARY_LIGHT_BLOCK)
                .build(null);

        GLOWSTEP_BOOTS = new GlowstepBootsItem(new Item.Properties().stacksTo(1));
        GRAPPLE_HOOK = new GrappleHookItem(new Item.Properties().stacksTo(1));
        RECALL_TOTEM = new RecallTotemItem(new Item.Properties().stacksTo(1).durability(64));
    }

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "temporary_light"), TEMPORARY_LIGHT_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "temporary_light"),
                TEMPORARY_LIGHT_BLOCK_ENTITY);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "glowstep_boots"), GLOWSTEP_BOOTS);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "grapple_hook"), GRAPPLE_HOOK);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "recall_totem"), RECALL_TOTEM);
    }
}
