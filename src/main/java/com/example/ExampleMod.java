package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, PlayerGlowData> playerData = new HashMap<>();

    private record PlayerGlowData(BlockPos lastPos, long lastTick) {
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing HackCraft Mod");

        ModContent.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTick = server.getTickCount();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                handlePlayerTick(player, currentTick);
            }

            GrappleManager.tick(server.overworld());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerData.remove(handler.player.getUUID());
            GrappleManager.onDisconnect(handler);
        });
    }

    private void handlePlayerTick(ServerPlayer player, long currentTick) {
        if (player.getItemBySlot(EquipmentSlot.FEET).getItem() != ModContent.GLOWSTEP_BOOTS) {
            return;
        }

        UUID uuid = player.getUUID();
        BlockPos currentPos = player.blockPosition();
        PlayerGlowData data = playerData.get(uuid);

        boolean shouldPlace = false;
        if (data == null) {
            shouldPlace = true;
        } else {
            boolean moved = !currentPos.equals(data.lastPos);
            if (moved) {
                shouldPlace = true;
            }
        }

        if (shouldPlace) {
            if (tryPlaceLight(player)) {
                playerData.put(uuid, new PlayerGlowData(currentPos, currentTick));
            }
        }
    }

    private boolean tryPlaceLight(ServerPlayer player) {
        BlockPos pos = player.blockPosition();

        if (isValidPlacement(player, pos)) {
            placeLight(player, pos);
            return true;
        }

        BlockPos down = pos.below();
        if (isValidPlacement(player, down)) {
            placeLight(player, down);
            return true;
        }

        return false;
    }

    private boolean isValidPlacement(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) {
            return false;
        }

        if (!player.level().getFluidState(pos).isEmpty()) {
            return false;
        }

        if (state.is(ModContent.TEMPORARY_LIGHT_BLOCK)) {
            return false;
        }

        return true;
    }

    private void placeLight(ServerPlayer player, BlockPos pos) {
        player.level().setBlockAndUpdate(pos, ModContent.TEMPORARY_LIGHT_BLOCK.defaultBlockState());
    }
}