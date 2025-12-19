package com.example;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecallState extends PersistentState {
    public Map<UUID, RecallPos> recalls = new HashMap<>();

    public static RecallState getServerState(MinecraftServer server) {
        // Use the Overworld to store the state globally
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new Type<>(RecallState::new, RecallState::fromNbt, null),
                "recall_totem_data"
        );
    }

    public static RecallState fromNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        RecallState state = new RecallState();
        NbtList list = nbt.getList("Recalls", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound compound = (NbtCompound) element;
            UUID uuid = compound.getUuid("UUID");
            RecallPos pos = RecallPos.fromNbt(compound);
            state.recalls.put(uuid, pos);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, RecallPos> entry : recalls.entrySet()) {
            NbtCompound compound = entry.getValue().toNbt();
            compound.putUuid("UUID", entry.getKey());
            list.add(compound);
        }
        nbt.put("Recalls", list);
        return nbt;
    }

    public static class RecallPos {
        public final RegistryKey<World> dim;
        public final Vec3d pos;
        public final float yaw;
        public final float pitch;

        public RecallPos(RegistryKey<World> dim, Vec3d pos, float yaw, float pitch) {
            this.dim = dim;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("Dim", dim.getValue().toString());
            nbt.putDouble("X", pos.x);
            nbt.putDouble("Y", pos.y);
            nbt.putDouble("Z", pos.z);
            nbt.putFloat("Yaw", yaw);
            nbt.putFloat("Pitch", pitch);
            return nbt;
        }

        public static RecallPos fromNbt(NbtCompound nbt) {
            Identifier dimId = Identifier.of(nbt.getString("Dim"));
            RegistryKey<World> dim = RegistryKey.of(RegistryKeys.WORLD, dimId);
            Vec3d pos = new Vec3d(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
            float yaw = nbt.getFloat("Yaw");
            float pitch = nbt.getFloat("Pitch");
            return new RecallPos(dim, pos, yaw, pitch);
        }
    }
}
