package com.example;

import com.example.block.TemporaryLightBlock;
import com.example.block.entity.TemporaryLightBlockEntity;
import com.example.item.GlowstepBootsItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModContent {
    
    public static final TemporaryLightBlock TEMPORARY_LIGHT_BLOCK;
    public static final BlockEntityType<TemporaryLightBlockEntity> TEMPORARY_LIGHT_BLOCK_ENTITY;
    public static final GlowstepBootsItem GLOWSTEP_BOOTS;
    public static final com.example.item.GrappleHookItem GRAPPLE_HOOK;
    public static final com.example.item.RecallTotemItem RECALL_TOTEM;

    static {
        // Block Settings: Air-like, luminance 12, no collision, replaceable, drops nothing
        AbstractBlock.Settings blockSettings = AbstractBlock.Settings.copy(Blocks.AIR)
                .luminance(state -> 12)
                .noCollision()
                .replaceable()
                .dropsNothing()
                .nonOpaque(); // Ensure it's treated as transparent

        TEMPORARY_LIGHT_BLOCK = new TemporaryLightBlock(blockSettings);

        // Block Entity
        TEMPORARY_LIGHT_BLOCK_ENTITY = BlockEntityType.Builder.create(TemporaryLightBlockEntity::new, TEMPORARY_LIGHT_BLOCK)
                .build(null);

        // Item Settings
        GLOWSTEP_BOOTS = new GlowstepBootsItem(new Item.Settings().maxCount(1));
        GRAPPLE_HOOK = new com.example.item.GrappleHookItem(new Item.Settings().maxCount(1));
        RECALL_TOTEM = new com.example.item.RecallTotemItem(new Item.Settings().maxCount(1).maxDamage(64));
    }

    public static void register() {
        Registry.register(Registries.BLOCK, Identifier.of(ExampleMod.MOD_ID, "temporary_light"), TEMPORARY_LIGHT_BLOCK);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(ExampleMod.MOD_ID, "temporary_light"), TEMPORARY_LIGHT_BLOCK_ENTITY);
        Registry.register(Registries.ITEM, Identifier.of(ExampleMod.MOD_ID, "glowstep_boots"), GLOWSTEP_BOOTS);
        Registry.register(Registries.ITEM, Identifier.of(ExampleMod.MOD_ID, "grapple_hook"), GRAPPLE_HOOK);
        Registry.register(Registries.ITEM, Identifier.of(ExampleMod.MOD_ID, "recall_totem"), RECALL_TOTEM);
    }
}
