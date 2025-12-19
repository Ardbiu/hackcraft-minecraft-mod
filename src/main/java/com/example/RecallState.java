package com.example;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecallState extends SavedData {
    public Map<UUID, RecallPos> recalls = new HashMap<>();

    public static RecallState getServerState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(RecallState::new, RecallState::load, null),
                "recall_totem_data");
    }

    public static RecallState load(CompoundTag tag, HolderLookup.Provider registries) {
        RecallState state = new RecallState();
        ListTag list = tag.getList("Recalls", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag compound = (CompoundTag) element;
            UUID uuid = compound.getUUID("UUID");
            RecallPos pos = RecallPos.fromTag(compound);
            state.recalls.put(uuid, pos);
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, RecallPos> entry : recalls.entrySet()) {
            CompoundTag compound = entry.getValue().toTag();
            compound.putUUID("UUID", entry.getKey());
            list.add(compound);
        }
        tag.put("Recalls", list);
        return tag;
    }

    public static class RecallPos {
        public final ResourceKey<Level> dim;
        public final Vec3 pos;
        public final float yaw;
        public final float pitch;

        public RecallPos(ResourceKey<Level> dim, Vec3 pos, float yaw, float pitch) {
            this.dim = dim;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", dim.location().toString());
            tag.putDouble("X", pos.x);
            tag.putDouble("Y", pos.y);
            tag.putDouble("Z", pos.z);
            tag.putFloat("Yaw", yaw);
            tag.putFloat("Pitch", pitch);
            return tag;
        }

        public static RecallPos fromTag(CompoundTag tag) {
            ResourceLocation dimId = ResourceLocation.parse(tag.getString("Dim"));
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
            Vec3 pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            float yaw = tag.getFloat("Yaw");
            float pitch = tag.getFloat("Pitch");
            return new RecallPos(dim, pos, yaw, pitch);
        }
    }
}
