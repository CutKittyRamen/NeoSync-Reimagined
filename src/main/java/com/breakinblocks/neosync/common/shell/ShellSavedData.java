package com.breakinblocks.neosync.common.shell;

import com.breakinblocks.neosync.api.networking.ShellStateUpdatePacket;
import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.api.shell.ShellStateManager;
import com.breakinblocks.neosync.api.shell.ShellStateUpdateType;
import com.breakinblocks.neosync.common.utils.NeoSyncDebug;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class ShellSavedData extends SavedData implements ShellStateManager {
    private static final String DATA_NAME = "neosync_shells";

    // ownerUuid -> shellUuid -> ShellState
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, ShellState>> shellsByOwner = new ConcurrentHashMap<>();
    private MinecraftServer server;

    public static ShellSavedData get(MinecraftServer server) {
        ShellSavedData data = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new ShellSavedData(server),
                        (nbt, registries) -> {
                            ShellSavedData loaded = new ShellSavedData(server);
                            loaded.load(nbt);
                            return loaded;
                        },
                        null
                ),
                DATA_NAME
        );
        data.server = server;
        return data;
    }

    private ShellSavedData(MinecraftServer server) {
        this.server = server;
    }

    private void load(CompoundTag nbt) {
        this.shellsByOwner.clear();
        if (nbt.contains("Owners", Tag.TAG_LIST)) {
            ListTag ownersList = nbt.getList("Owners", Tag.TAG_COMPOUND);
            for (int i = 0; i < ownersList.size(); i++) {
                CompoundTag ownerTag = ownersList.getCompound(i);
                UUID ownerId = ownerTag.getUUID("OwnerUUID");
                ConcurrentMap<UUID, ShellState> ownerShells = new ConcurrentHashMap<>();
                ListTag shellsList = ownerTag.getList("Shells", Tag.TAG_COMPOUND);
                for (int j = 0; j < shellsList.size(); j++) {
                    ShellState state = ShellState.fromNbt(shellsList.getCompound(j));
                    if (state != null && state.getUuid() != null) {
                        ownerShells.put(state.getUuid(), state);
                    }
                }
                if (!ownerShells.isEmpty()) {
                    this.shellsByOwner.put(ownerId, ownerShells);
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag ownersList = new ListTag();
        for (Map.Entry<UUID, ConcurrentMap<UUID, ShellState>> entry : this.shellsByOwner.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putUUID("OwnerUUID", entry.getKey());
            ListTag shellsList = new ListTag();
            for (ShellState state : entry.getValue().values()) {
                shellsList.add(state.writeNbt(new CompoundTag()));
            }
            ownerTag.put("Shells", shellsList);
            ownersList.add(ownerTag);
        }
        nbt.put("Owners", ownersList);
        return nbt;
    }

    @Override
    public void setAvailableShellStates(UUID owner, Stream<ShellState> states) {
        ConcurrentMap<UUID, ShellState> map = new ConcurrentHashMap<>();
        states.forEach(s -> map.put(s.getUuid(), s));
        this.shellsByOwner.put(owner, map);
        this.setDirty();
    }

    @Override
    public Stream<ShellState> getAvailableShellStates(UUID owner) {
        ConcurrentMap<UUID, ShellState> map = this.shellsByOwner.get(owner);
        return map == null ? Stream.empty() : map.values().stream();
    }

    @Override
    public @Nullable ShellState getShellStateByUuid(UUID owner, UUID uuid) {
        ConcurrentMap<UUID, ShellState> map = this.shellsByOwner.get(owner);
        return map == null ? null : map.get(uuid);
    }

    @Override
    public void add(ShellState state) {
        if (!isValidShellState(state)) return;
        UUID owner = state.getOwnerUuid();
        
        ConcurrentMap<UUID, ShellState> ownerShells = this.shellsByOwner.computeIfAbsent(owner, k -> new ConcurrentHashMap<>());
        
        // Handle collision: if a shell with this UUID exists and its pos is different,
        // it means someone copied a shell NBT to another block. We must re-roll the UUID.
        ShellState existing = ownerShells.get(state.getUuid());
        if (existing != null && existing != state) {
            boolean posDiffers = state.getPos() != null && !state.getPos().equals(existing.getPos());
            if (posDiffers) {
                NeoSyncDebug.info("server-manager", "UUID collision detected for {} at {}. Generating new UUID.", state.getUuid(), state.getPos());
                // Create a clone with a new UUID.
                CompoundTag tag = state.writeNbt(new CompoundTag());
                tag.putUUID("uuid", UUID.randomUUID());
                state = ShellState.fromNbt(tag);
            }
        }
        
        ownerShells.put(state.getUuid(), state);
        this.setDirty();
        this.sendDeltaToOwner(owner, ShellStateUpdateType.ADD, state);
    }

    @Override
    public void remove(ShellState state) {
        if (!isValidShellState(state)) return;
        UUID owner = state.getOwnerUuid();
        ConcurrentMap<UUID, ShellState> ownerShells = this.shellsByOwner.get(owner);
        if (ownerShells != null) {
            ownerShells.remove(state.getUuid());
            this.setDirty();
            this.sendDeltaToOwner(owner, ShellStateUpdateType.REMOVE, state);
        }
    }

    @Override
    public void update(ShellState state) {
        if (!isValidShellState(state)) return;
        UUID owner = state.getOwnerUuid();
        ConcurrentMap<UUID, ShellState> ownerShells = this.shellsByOwner.get(owner);
        if (ownerShells != null && ownerShells.containsKey(state.getUuid())) {
            ownerShells.put(state.getUuid(), state);
            this.setDirty();
            this.sendDeltaToOwner(owner, ShellStateUpdateType.UPDATE, state);
        }
    }

    private void sendDeltaToOwner(UUID ownerId, ShellStateUpdateType type, ShellState state) {
        if (this.server == null) return;
        ServerPlayer player = this.server.getPlayerList().getPlayer(ownerId);
        if (player != null) {
            new ShellStateUpdatePacket(type, state).send(player);
        }
    }

}
