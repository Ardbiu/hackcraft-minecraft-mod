package com.example;

import com.example.item.GrappleHookItem;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class GrappleManager {
    private static final double MAX_RANGE = 30.0;
    private static final double STOP_DISTANCE_SQUARED = 2.0 * 2.0; // 2 blocks
    private static final int MAX_DURATION_TICKS = 40; // 2 seconds (20 tps * 2)
    private static final int COOLDOWN_TICKS = 40;
    
    // Tuning parameters
    private static final double MIN_SPEED = 0.6;
    private static final double MAX_SPEED = 1.6;

    private static final Map<UUID, GrappleState> activeGrapples = new HashMap<>();

    public static void attemptGrapple(ServerPlayerEntity player, net.minecraft.item.ItemStack stack) {
        // Cleanup existing state if any (allows retrying/resetting)
        activeGrapples.remove(player.getUuid());

        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(MAX_RANGE));

        BlockHitResult hitResult = player.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Hit logic
            Vec3d targetPos = hitResult.getPos();
            GrappleState state = new GrappleState(targetPos, player.getServerWorld().getTime());
            activeGrapples.put(player.getUuid(), state);
            
            // Set Cooldown
            player.getItemCooldownManager().set(stack.getItem(), COOLDOWN_TICKS);

            // Visuals/Sound
            ServerWorld world = player.getServerWorld();
            world.spawnParticles(ParticleTypes.CRIT, targetPos.x, targetPos.y, targetPos.z, 
                    10, 0.5, 0.5, 0.5, 0.05);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
            world.playSound(null, targetPos.x, targetPos.y, targetPos.z, 
                    SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.5F, 2.0F); // "Clink" sound at target

        } else {
            // Fail feedback
            player.playSound(SoundEvents.BLOCK_DISPENSER_FAIL, 1.0F, 1.2F);
        }
    }

    public static void tick(ServerWorld world) {
        long currentTick = world.getTime();
        Iterator<Map.Entry<UUID, GrappleState>> iterator = activeGrapples.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, GrappleState> entry = iterator.next();
            UUID uuid = entry.getKey();
            GrappleState state = entry.getValue();
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);

            // Cleanup conditions
            if (player == null || player.isDisconnected() || player.isDead() || player.isSpectator()) {
                iterator.remove();
                continue;
            }

            if (player.getWorld() != world) {
                // Different dimension, remove (simplified)
                iterator.remove();
                continue;
            }

            // Time limit
            long elapsed = currentTick - state.startTick;
            if (elapsed > MAX_DURATION_TICKS) {
                iterator.remove();
                continue; // Timeout
            }

            // Check distance
            Vec3d playerPos = player.getPos(); // Use feet pos specifically? Prompt said "target Vec3d" which comes from raycast.
            // Check distance to target
            if (playerPos.squaredDistanceTo(state.target) < STOP_DISTANCE_SQUARED) {
                iterator.remove();
                // Final little hop or stop? Prompt says "Stops near wall".
                // Halt velocity?
                player.setVelocity(0, 0, 0);
                player.velocityModified = true;
                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                continue;
            }

            // Apply Velocity
            Vec3d direction = state.target.subtract(playerPos).normalize();
            
            // Speed ramp logic: (elapsed / max_duration) or constant? 
            // "speed ramp: min 0.6, max 1.6 blocks/tick"
            // Let's ramp up based on elapsed time to make it feel like "reeling in" accelerates? 
            // Or maybe just based on distance? The prompt implies "ramp" over time probably.
            double progress = (double) elapsed / MAX_DURATION_TICKS;
            double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * progress; // Linear ramp
            
            Vec3d velocity = direction.multiply(speed);
            
            // Apply to player
            player.setVelocity(velocity);
            player.velocityModified = true;
            
            // Send packet for smooth server-side authority override
            player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

            // Prevent fall damage
            player.fallDistance = 0;

            // Optional: Particles along the line or at player
            if (elapsed % 4 == 0) {
                 world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.5, player.getZ(), 
                         1, 0, 0, 0, 0);
            }
        }
    }
    
    public static void onDisconnect(ServerPlayNetworkHandler handler) {
        activeGrapples.remove(handler.player.getUuid());
    }

    private static class GrappleState {
        final Vec3d target;
        final long startTick;

        GrappleState(Vec3d target, long startTick) {
            this.target = target;
            this.startTick = startTick;
        }
    }
}
