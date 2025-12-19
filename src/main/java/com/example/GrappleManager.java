package com.example;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class GrappleManager {
    private static final double MAX_RANGE = 30.0;
    private static final double STOP_DISTANCE_SQUARED = 2.0 * 2.0;
    private static final int MAX_DURATION_TICKS = 40;
    private static final int COOLDOWN_TICKS = 40;
    private static final double MIN_SPEED = 0.6;
    private static final double MAX_SPEED = 1.6;

    private static final Map<UUID, GrappleState> activeGrapples = new HashMap<>();

    public static void attemptGrapple(ServerPlayer player, ItemStack stack) {
        activeGrapples.remove(player.getUUID());

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(MAX_RANGE));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 targetPos = hitResult.getLocation();
            GrappleState state = new GrappleState(targetPos, player.serverLevel().getGameTime());
            activeGrapples.put(player.getUUID(), state);

            player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);

            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.CRIT, targetPos.x, targetPos.y, targetPos.z,
                    10, 0.5, 0.5, 0.5, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5F, 2.0F);
        } else {
            player.playSound(SoundEvents.DISPENSER_FAIL, 1.0F, 1.2F);
        }
    }

    public static void tick(ServerLevel level) {
        long currentTick = level.getGameTime();
        Iterator<Map.Entry<UUID, GrappleState>> iterator = activeGrapples.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, GrappleState> entry = iterator.next();
            UUID uuid = entry.getKey();
            GrappleState state = entry.getValue();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);

            if (player == null || player.isRemoved() || player.isDeadOrDying() || player.isSpectator()) {
                iterator.remove();
                continue;
            }

            if (player.level() != level) {
                iterator.remove();
                continue;
            }

            long elapsed = currentTick - state.startTick;
            if (elapsed > MAX_DURATION_TICKS) {
                iterator.remove();
                continue;
            }

            Vec3 playerPos = player.position();
            if (playerPos.distanceToSqr(state.target) < STOP_DISTANCE_SQUARED) {
                iterator.remove();
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                continue;
            }

            Vec3 directionVec = state.target.subtract(playerPos).normalize();
            double progress = (double) elapsed / MAX_DURATION_TICKS;
            double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * progress;
            Vec3 velocity = directionVec.scale(speed);

            player.setDeltaMovement(velocity);
            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            player.fallDistance = 0;

            if (elapsed % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.5, player.getZ(),
                        1, 0, 0, 0, 0);
            }
        }
    }

    public static void onDisconnect(ServerGamePacketListenerImpl handler) {
        activeGrapples.remove(handler.player.getUUID());
    }

    private static class GrappleState {
        final Vec3 target;
        final long startTick;

        GrappleState(Vec3 target, long startTick) {
            this.target = target;
            this.startTick = startTick;
        }
    }
}
