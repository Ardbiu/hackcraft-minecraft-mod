package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, PlayerGlowData> playerData = new HashMap<>();
    private static final int PLACEMENT_COOLDOWN = 5; // Ticks

    private record PlayerGlowData(BlockPos lastPos, long lastTick) {}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Glowstep Boots Mod");
        
        ModContent.register();

        // Register Server Tick to handle light placement
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTick = server.getTicks();
            
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handlePlayerTick(player, currentTick);
            }
            
            // Grapple Update
            com.example.GrappleManager.tick(server.getOverworld()); // Assuming single dim or we iterate all worlds. 
            // Better to iterate all worlds or get world from player in manager.
            // Manager iterates active grapples which have players, so we can pass any world or just handle inside? 
            // Manager tick takes a ServerWorld, but it creates iterators. 
            // Let's iterate all worlds for safety if we support multi-dim grapples, 
            // OR simpler: pass the server and let manager handle filtering.
            // My manager takes `ServerWorld` currently. Let's fix that usage.
            // Re-reading logic: Manager iterates `activeGrapples`, gets player, checks `player.getWorld()`.
            // So we just need to call it once per server tick, not per world really, 
            // BUT the manager code as written takes `ServerWorld` just to get `getTime()`.
            // The server time is global usually. 
            // Any world will do for time.
            com.example.GrappleManager.tick(server.getOverworld());
        });

        // Cleanup on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerData.remove(handler.player.getUuid());
            com.example.GrappleManager.onDisconnect(handler);
        });
	}

    private void handlePlayerTick(ServerPlayerEntity player, long currentTick) {
        // Check if wearing boots
        if (player.getEquippedStack(EquipmentSlot.FEET).getItem() != ModContent.GLOWSTEP_BOOTS) {
            return;
        }

        UUID uuid = player.getUuid();
        BlockPos currentPos = player.getBlockPos();
        PlayerGlowData data = playerData.get(uuid);

        // Check cooldown and position change
        // We want to place if:
        // 1. No data (first time)
        // 2. OR Moved to a new block AND cooldown passed (optimization: we can place immediately if moved, but prompt says "at most once every 5 ticks per player OR ...")
        // Prompt says: "Place at most once every 5 ticks ... OR only when the player changes block position (recommended)."
        // Let's do: Place if moved to a NEW block position. BUT rate limit checking?
        // Actually, if I walk 5 blocks in 1 tick (teleport), I might want a trail?
        // Let's stick to: If (moved block OR (time > last + 5)) -> attempt placement.
        // But prompt logic "Place at most once every 5 ticks" is a restriction.
        // "OR only when...". I'll prioritize position change.
        
        boolean shouldPlace = false;
        if (data == null) {
            shouldPlace = true;
        } else {
            boolean moved = !currentPos.equals(data.lastPos);
            boolean cooldownReady = currentTick - data.lastTick >= PLACEMENT_COOLDOWN;
            
            // "Place at most once every 5 ticks" suggests a hard cap on frequency? 
            // OR "only when player changes block position".
            // Implementation: Check if moved. If moved, place.
            // Also update lastTick.
            // If I just stand still, I don't need to place new lights periodically because they expire on their own time?
            // Actually, if I stand still, the light at my feet expires in 10s. I should replace it.
            // So if (moved OR cooldownReady) is not quite right if I want to maintain light while standing.
            // But user said "Stop moving; no new lights placed." (Acceptance Test 2).
            // So ONLY place when moving.
            
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

    private boolean tryPlaceLight(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        
        // Check A: playerBlockPos (feet)
        if (isValidPlacement(player, pos)) {
            placeLight(player, pos);
            return true;
        }
        
        // Check B: down
        BlockPos down = pos.down();
        if (isValidPlacement(player, down)) {
            placeLight(player, down);
            return true;
        }

        return false;
    }

    private boolean isValidPlacement(ServerPlayerEntity player, BlockPos pos) {
        // Must be AIR (or replacable) and NOT liquid.
        // Actually, ModContent.TEMPORARY_LIGHT_BLOCK is replaceable.
        // We should check if the CURRENT block at pos is replaceable/air.
        
        // Do NOT overwrite non-air blocks. "Only place a light if the target block position is AIR (or replaceable) and not a fluid block."
        
        net.minecraft.block.BlockState state = player.getWorld().getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) {
            return false;
        }
        
        // Check for fluid
        if (!player.getWorld().getFluidState(pos).isEmpty()) {
            return false;
        }

        // Avoid overwriting our own light block just to reset timer?
        // The block entity handles expiration. If we overwrite it, it resets the TE?
        // If we place the block again, it usually replaces.
        // If it's already a light block, should we refresh it? 
        // User says "Stop moving; no new lights placed." so if I'm standing still, it expires.
        // That implies I lose light if I stand still. That matches "Stop moving; no new lights placed".
        
        // Avoid overwriting existing TemporaryLightBlock? 
        if (state.isOf(ModContent.TEMPORARY_LIGHT_BLOCK)) {
            return false; // Already there, don't spam.
        }

        return true;
    }

    private void placeLight(ServerPlayerEntity player, BlockPos pos) {
        player.getWorld().setBlockState(pos, ModContent.TEMPORARY_LIGHT_BLOCK.getDefaultState());
    }
}